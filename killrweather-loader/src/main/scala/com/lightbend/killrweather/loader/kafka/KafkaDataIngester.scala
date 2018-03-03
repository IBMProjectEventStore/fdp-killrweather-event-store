package com.lightbend.killrweather.loader.kafka

import com.lightbend.killrweather.loader.utils.{ DataConvertor, FilesIterator }
import com.lightbend.killrweather.kafka.MessageSender
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.kafka.common.serialization.ByteArraySerializer

import scala.collection.mutable.ListBuffer

/**
  * Created by boris on 7/7/17.
  */
object KafkaDataIngester {
  val file = "data/load/"
  val timeInterval: Long = 1000 * 1 // 1 sec
  val batchInterval: Long = 1000 * 600 // 10 min
  val batchSize = 10

  def main(args: Array[String]) {

    import WeatherSettings._

    val ingester = KafkaDataIngester(kafkaBrokers)
    ingester.execute(file, KafkaTopicRaw)
  }

  def pause(time : Long): Unit = {
    try {
      Thread.sleep(time)
    } catch {
      case _: Throwable => // Ignore
    }
  }

  def apply(brokers: String): KafkaDataIngester = new KafkaDataIngester(brokers)
}

class KafkaDataIngester(brokers: String) {

  import KafkaDataIngester._

  var sender = MessageSender[Array[Byte], Array[Byte]](brokers, classOf[ByteArraySerializer].getName, classOf[ByteArraySerializer].getName)

  def execute(file: String, topic: String): Unit = {

    while (true) {
      val iterator = FilesIterator(new java.io.File(file), "UTF-8")
      val batch = new ListBuffer[Array[Byte]]()
      var numrec = 0;
      iterator.foreach(record => {
        numrec += 1
        batch += DataConvertor.convertToGPB(record)
        if (batch.size >= batchSize) {
          try {
            if (sender == null)
              sender = MessageSender[Array[Byte], Array[Byte]](brokers, classOf[ByteArraySerializer].getName, classOf[ByteArraySerializer].getName)
            sender.batchWriteValue(topic, batch)
            batch.clear()
          } catch {
            case e: Throwable =>
              println(s"Kafka failed: ${e.printStackTrace()}")
              if (sender != null)
                sender.close()
              sender = null
          }
          pause(timeInterval)
        }
        if (numrec % 100 == 0)
          println(s"Submitted $numrec records")
      })
      if (batch.size > 0)
        sender.batchWriteValue(topic, batch)
      println(s"Submitted $numrec records")
      pause(batchInterval)
    }
  }
}