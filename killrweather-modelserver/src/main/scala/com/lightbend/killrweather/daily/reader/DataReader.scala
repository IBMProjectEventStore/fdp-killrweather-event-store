package com.lightbend.killrweather.daily.reader

import com.lightbend.killrweather.kafka.MessageListener
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.kafka.common.serialization.ByteArrayDeserializer

object DataReader {

  def main(args: Array[String]) {
    val settings = WeatherSettings()
    import settings._

    println(s"Using kafka brokers at ${kafkaDaylyConfig.brokers}")

    val listener = MessageListener(kafkaDaylyConfig.brokers, kafkaDaylyConfig.topic, "GG", classOf[ByteArrayDeserializer].getName, classOf[ByteArrayDeserializer].getName, new RecordProcessor())
    listener.start()
  }
}
