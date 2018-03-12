package com.lightbend.killrweather.daily.model

import com.lightbend.killrweather.WeatherClient.TemperatureDailyRecord

import scala.util.Try

/**
 * Created by boris on 5/8/17.
 */
object DataRecord {

  def fromByteArray(message: Array[Byte]): Try[TemperatureDailyRecord] = Try {
    TemperatureDailyRecord.parseFrom(message) match {
      case r if(r.wsid.isEmpty) => throw new Exception("got empty record")
      case r => r
    }
  }
}
