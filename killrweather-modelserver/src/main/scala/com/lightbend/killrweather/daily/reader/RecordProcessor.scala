package com.lightbend.killrweather.daily.reader

import com.lightbend.killrweather.WeatherClient.TemperatureDailyRecord
import com.lightbend.killrweather.kafka.RecordProcessorTrait
import org.apache.kafka.clients.consumer.ConsumerRecord

class RecordProcessor extends RecordProcessorTrait[Array[Byte], Array[Byte]] {
  override def processRecord(record: ConsumerRecord[Array[Byte], Array[Byte]]): Unit = {
    println(s"Retrieved message  value = ${TemperatureDailyRecord.parseFrom(record.value)}")
  }
}
