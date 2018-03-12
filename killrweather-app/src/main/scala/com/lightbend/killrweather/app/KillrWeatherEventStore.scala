package com.lightbend.killrweather.app

import java.util.Calendar

import com.lightbend.kafka.KafkaLocalServer
import com.lightbend.killrweather.EventStore.EventStoreSupport
import com.lightbend.killrweather.WeatherClient.WeatherRecord
import com.lightbend.killrweather.app.eventstore.EventStoreSink
import com.lightbend.killrweather.app.kafka.{KafkaDataConvertor, KafkaSink}
import com.lightbend.killrweather.kafka.{MessageListener, MessageProducer}
import com.lightbend.killrweather.settings.WeatherSettings
import com.lightbend.killrweather.utils._
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, State, StateSpec, StreamingContext}
import org.apache.spark.util.StatCounter
import org.apache.log4j.{Level, LogManager}

import scala.collection.mutable.ListBuffer

/**
 * Created by boris on 7/9/17.
 */
object KillrWeatherEventStore {

  def main(args: Array[String]): Unit = {

    // Create context
    import WeatherSettings._

    // Enable Logging for the Event Store
    //LogManager.getLogger("com.ibm.event").setLevel(Level.DEBUG)
    //LogManager.getLogger("org.apache.spark.sql.ibm.event").setLevel(Level.DEBUG)

    // Initialize Event Store
    val ctx = EventStoreSupport.createContext(eventStore, user, password)
    ctx.foreach(EventStoreSupport.ensureTables(_))
    println(s"Event Store initialised")

    // Create embedded Kafka and topic
    if(localKafkaBrokers) {
      val kafka = KafkaLocalServer(true)
      kafka.start()
      kafka.createTopic(KafkaTopicRaw)
      kafka.createTopic(KafkaTopicDaily)
      kafka.createTopic(KafkaTopicModel)
      println(s"Kafka Cluster created")
    }

    val spark = SparkSession
      .builder()
      .appName(AppName)
      .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()

    val ssc = new StreamingContext(spark.sparkContext, Seconds(SparkStreamingBatchInterval / 1000))
    ssc.checkpoint(SparkCheckpointDir)

    // Create raw data observations stream
    val kafkaParams = MessageListener.consumerProperties(
      kafkaBrokers,
      KafkaGroupId, classOf[ByteArrayDeserializer].getName, classOf[ByteArrayDeserializer].getName
    )
    val topics = List(KafkaTopicRaw)

    // Initial state RDD for daily accumulator
    val dailyRDD = ssc.sparkContext.emptyRDD[(String, ListBuffer[WeatherRecord])]

    // Initial state RDD for monthly accumulator
    val monthlyRDD = ssc.sparkContext.emptyRDD[(String, ListBuffer[DailyWeatherDataProcess])]

    // Create broadcast variable for the sink definition

    val eventStoreSink = spark.sparkContext.broadcast(EventStoreSink(eventStore, user, password))
    val kafkaSinkProps = MessageProducer.producerProperties(kafkaBrokers,
      classOf[ByteArraySerializer].getName, classOf[ByteArraySerializer].getName)
    val kafkaSink = spark.sparkContext.broadcast(KafkaSink(kafkaSinkProps))

    val kafkaDataStream = KafkaUtils.createDirectStream[Array[Byte], Array[Byte]](
      ssc, PreferConsistent, Subscribe[Array[Byte], Array[Byte]](topics, kafkaParams)
    )

    val rawStream = kafkaDataStream.map(r => WeatherRecord.parseFrom(r.value()))

    /** Saves the raw data to Event Store - raw table. */
//    rawStream.foreachRDD {spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeRaw(_)) }

    // Calculate daily
    val dailyMappingFunc = (station: String, reading: Option[WeatherRecord], state: State[ListBuffer[WeatherRecord]]) => {
      val current = state.getOption().getOrElse(new ListBuffer[WeatherRecord])
      var daily: Option[(String, DailyWeatherData)] = None
      val last = current.lastOption.getOrElse(null.asInstanceOf[WeatherRecord])
      val ts = Calendar.getInstance()
      reading match {
        case Some(weather) => {
          current match {
            case sequence if ((sequence.size > 0) && (weather.day != last.day)) => {
              // The day has changed
              val dailyPrecip = sequence.foldLeft(.0)(_ + _.oneHourPrecip)
              val tempAggregate = StatCounter(sequence.map(_.temperature))
              val windAggregate = StatCounter(sequence.map(_.windSpeed))
              val pressureAggregate = StatCounter(sequence.map(_.pressure).filter(_ > 1.0)) // remove 0 elements
              ts.set(last.year, last.month, last.day, 0, 0, 0)
              daily = Some((last.wsid, DailyWeatherData(last.wsid, last.year, last.month, last.day, ts.getTimeInMillis,
                tempAggregate.max, tempAggregate.min, tempAggregate.mean, tempAggregate.stdev, tempAggregate.variance,
                windAggregate.max, windAggregate.min, windAggregate.mean, windAggregate.stdev, windAggregate.variance,
                pressureAggregate.max, pressureAggregate.min, pressureAggregate.mean, pressureAggregate.stdev, pressureAggregate.variance,
                dailyPrecip)))
              current.clear()
            }
            case _ =>
          }
          current += weather
          state.update(current)
        }
        case None =>
      }
      daily
    }

    // Define StateSpec<KeyType,ValueType,StateType,MappedType> - types are derived from function
    val dailyStream = rawStream.map(r => (r.wsid, r)).mapWithState(StateSpec.function(dailyMappingFunc).initialState(dailyRDD))
      .filter(_.isDefined).map(_.get)

    // Just for testing
    dailyStream.print()

    //  Write dayly temperature to Kafka
    dailyStream.map(ds => KafkaDataConvertor.toGPB(ds._2)).foreachRDD(_.foreach(kafkaSink.value.send(KafkaTopicDaily, _)))

    // Save daily temperature
    dailyStream.map(ds => DailyTemperature(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeDailyTemperature(_)) }

    // Save daily wind
    dailyStream.map(ds => DailyWindSpeed(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeDailyWind(_)) }

    // Save daily pressure
    dailyStream.map(ds => DailyPressure(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeDailyPressure(_)) }

    // Save daily presipitations
    dailyStream.map(ds => DailyPrecipitation(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeDailyPresip(_)) }

    // Calculate monthly
    val monthlyMappingFunc = (station: String, reading: Option[DailyWeatherDataProcess], state: State[ListBuffer[DailyWeatherDataProcess]]) => {
      val current = state.getOption().getOrElse(new ListBuffer[DailyWeatherDataProcess])
      var monthly: Option[(String, MonthlyWeatherData)] = None
      val last = current.lastOption.getOrElse(null.asInstanceOf[DailyWeatherDataProcess])
      val ts = Calendar.getInstance()
      reading match {
        case Some(weather) => {
          current match {
            case sequence if ((sequence.size > 0) && (weather.month != last.month)) => {
              // The day has changed
              val tempAggregate = StatCounter(sequence.map(_.temp))
              val windAggregate = StatCounter(sequence.map(_.wind))
              val pressureAggregate = StatCounter(sequence.map(_.pressure))
              val presipAggregate = StatCounter(sequence.map(_.precip))
              ts.set(last.year, last.month, 1, 0, 0, 0)
              monthly = Some((last.wsid, MonthlyWeatherData(last.wsid, last.year, last.month, ts.getTimeInMillis,
                tempAggregate.max, tempAggregate.min, tempAggregate.mean, tempAggregate.stdev, tempAggregate.variance,
                windAggregate.max, windAggregate.min, windAggregate.mean, windAggregate.stdev, windAggregate.variance,
                pressureAggregate.max, pressureAggregate.min, pressureAggregate.mean, pressureAggregate.stdev, pressureAggregate.variance,
                presipAggregate.max, presipAggregate.min, presipAggregate.mean, presipAggregate.stdev, presipAggregate.variance)))
              current.clear()
            }
            case _ =>
          }
          current += weather
          state.update(current)
        }
        case None =>
      }
      monthly
    }

    val monthlyStream = dailyStream.map(r => (r._1, DailyWeatherDataProcess(r._2))).
      mapWithState(StateSpec.function(monthlyMappingFunc).initialState(monthlyRDD)).filter(_.isDefined).map(_.get)

    // Save monthly temperature
    monthlyStream.map(ds => MonthlyTemperature(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeMothlyTemperature(_)) }

    // Save monthly wind
    monthlyStream.map(ds => MonthlyWindSpeed(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeMothlyWind(_)) }

    // Save monthly pressure
    monthlyStream.map(ds => MonthlyPressure(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeMothlyPressure(_)) }

    // Save monthly presipitations
    monthlyStream.map(ds => MonthlyPrecipitation(ds._2))
      .foreachRDD { spark.createDataFrame(_).foreachPartition(eventStoreSink.value.writeMothlyPresip(_)) }

    // Execute
    ssc.start()
    ssc.awaitTermination()
  }
}
