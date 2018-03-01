package com.lightbend.killrweather.loader.utils

import java.io.ByteArrayOutputStream
import java.util.Calendar

import com.lightbend.killrweather.WeatherClient.WeatherRecord
import com.lightbend.killrweather.utils.RawWeatherData
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

object DataConvertor {

  implicit val formats = DefaultFormats
  private val bos = new ByteArrayOutputStream()

  def convertToJson(string: String): String = {
    val report = RawWeatherData(string.split(","))
    write(report)
  }

  def convertToRecord(string: String): WeatherRecord = {
    val report = RawWeatherData(string.split(","))
    val date = Calendar.getInstance()
    // Month is 0-based
    date.set(report.year, report.month-1, report.day, report.hour, DataConvertorImpl.DEFAULT_HOUR, DataConvertorImpl.DEFAULT_MIN)
    WeatherRecord(
      wsid = report.wsid,
      year = report.year,
      month = report.month,
      day = report.day,
      hour = report.hour,
      ts = date.getTimeInMillis,
      temperature = report.temperature,
      dewpoint = report.dewpoint,
      pressure = report.pressure,
      windDirection = report.windDirection,
      windSpeed = report.windSpeed,
      skyCondition = report.skyCondition,
      skyConditionText = report.skyConditionText,
      oneHourPrecip = report.oneHourPrecip,
      sixHourPrecip = report.sixHourPrecip
    )
  }

  def convertToGPB(string: String): Array[Byte] = {
    bos.reset
    convertToRecord(string).writeTo(bos)
    bos.toByteArray
  }
}
object DataConvertorImpl {
  val DEFAULT_HOUR = 0
  val DEFAULT_MIN = 0
}