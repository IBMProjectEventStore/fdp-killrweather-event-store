package com.lightbend.killrweather.daily.reader

import com.lightbend.killrweather.kafka.MessageListener
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.kafka.common.serialization.ByteArrayDeserializer

object DataReader {

  def main(args: Array[String]) {
    import WeatherSettings._

    println(s"Using kafka brokers at $kafkaBrokers")

    val listener = MessageListener(kafkaBrokers, KafkaTopicDaily, "GG", classOf[ByteArrayDeserializer].getName, classOf[ByteArrayDeserializer].getName, new RecordProcessor())
    listener.start()
  }
}
