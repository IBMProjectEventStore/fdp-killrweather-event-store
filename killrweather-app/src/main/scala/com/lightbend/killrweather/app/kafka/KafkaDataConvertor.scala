package com.lightbend.killrweather.app.kafka

import java.io.ByteArrayOutputStream

import com.lightbend.killrweather.utils.DailyWeatherData
import com.lightbend.killrweather.WeatherClient.TemperatureDailyRecord

object KafkaDataConvertor {

  private val bos = new ByteArrayOutputStream()

  def toGPB(daily: DailyWeatherData) : Array[Byte] = {
    bos.reset
    new TemperatureDailyRecord(daily.wsid, daily.year, daily.month, daily.day, daily.ts, daily.meanTemp).writeTo(bos)
    bos.toByteArray
  }
}
