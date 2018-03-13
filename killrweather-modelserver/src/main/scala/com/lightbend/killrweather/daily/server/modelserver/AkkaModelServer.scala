package com.lightbend.killrweather.daily.server.modelserver

import java.net.InetAddress
import java.util.Calendar

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import akka.pattern.ask
import akka.stream.scaladsl.Sink
import com.ibm.event.catalog.ResolvedTableSchema
import com.ibm.event.oltp.EventContext
import com.lightbend.killrweather.EventStore.EventStoreSupport
import com.lightbend.killrweather.daily.model.{DataRecord, ModelToServe, ServingResult}
import com.lightbend.killrweather.daily.server.actors.ModelServingManager
import com.lightbend.killrweather.daily.server.queryablestate.QueriesAkkaHttpResource
import com.lightbend.killrweather.settings.WeatherSettings._
import com.lightbend.scala.modelServer.model.ModelWithDescriptor
import org.apache.spark.sql.Row

import scala.concurrent.duration._
import scala.util.Success

/**
 * Created by boris on 7/21/17.
 */
object AkkaModelServer {


  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val askTimeout = Timeout(30.seconds)

  var ctx : Option[EventContext] = None
  var table : ResolvedTableSchema = null

  println(s"Using kafka brokers at ${kafkaBrokers} ")

  val dataConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(kafkaBrokers)
    .withGroupId(KafkaModelDataGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val modelConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(kafkaBrokers)
    .withGroupId(KafkaModelGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  def main(args: Array[String]): Unit = {

    val modelserver = system.actorOf(ModelServingManager.props)

    // Model stream processing
    Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(KafkaTopicModel))
      .map(record => ModelToServe.fromByteArray(record.value)).collect { case Success(a) => a }
      .map(record => ModelWithDescriptor.fromModelToServe(record)).collect { case Success(a) => a }
      .mapAsync(1)(elem => modelserver ? elem)
      .runWith(Sink.ignore) // run the stream, we do not read the results directly

    // Data stream processing
    Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(KafkaTopicDaily))
      .map(record => DataRecord.fromByteArray(record.value())).collect { case Success(a) => a }
      .mapAsync(1)(elem => (modelserver ? elem).mapTo[ServingResult])
      .runForeach(result => {
        result.processed match {
          case true => {
            val date = TempDate(result.ts)
            println(s"Calculated temperature - ${result.result} for station : ${result.wsid} for ${date.year}-${date.month}-${date.day} calculated in ${result.duration} ms")
            writeToES(result)
          }
          case _ => println ("No model available - skipping")
        }
      })

    // Rest Server
    startRest(modelserver)
  }


  def writeToES(result : ServingResult) : Unit = {
    if(!ctx.isDefined) {
      ctx = EventStoreSupport.createContext(eventStore, user, password)
      table = ctx.get.getTable(PREDICTTEMP)
    }
    val start = System.currentTimeMillis()
    val date = TempDate(result.ts)
    val row = Row(result.wsid.replace("_", ":"), date.year, date.month, date.day, result.ts, result.result)
    ctx.get.insert(table, row) match {
      case res if (res.failed) =>
        println(s"Failed to insert prediction ${res.toString()}")
        ctx = None
      case _ =>
    }
    println(s"Inserted prediction record in ES in ${System.currentTimeMillis() - start}")
  }

  // See http://localhost:5500/models
  // Then select a model shown and try http://localhost:5500/state/<model>, e.g., http://localhost:5500/state/wine
  def startRest(modelserver: ActorRef): Unit = {

    implicit val timeout = Timeout(10.seconds)
    val host = InetAddress.getLocalHost.getHostAddress
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
        case ex =>
          println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
      }
  }
}

case class TempDate(year : Int, month : Int, day : Int)

object TempDate{
  def apply(ts : Long) : TempDate = {
    val date = Calendar.getInstance()
    date.setTimeInMillis(ts)
    new TempDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
  }
}