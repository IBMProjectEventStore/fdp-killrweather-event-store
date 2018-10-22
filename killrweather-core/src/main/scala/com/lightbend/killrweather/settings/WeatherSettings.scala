/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lightbend.killrweather.settings

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.duration.FiniteDuration

/**
  * Application settings. First attempts to acquire from the deploy environment.
  * If not exists, then from -D java system properties, else a default config.
  *
  * Settings in the environment such as: SPARK_HA_MASTER=local[10] is picked up first.
  *
  * Settings from the command line in -D will override settings in the deploy environment.
  * For example: sbt -Dspark.master="local[12]" run
  *
  * If you have not yet used Typesafe Config before, you can pass in overrides like so:
  *
  * {{{
  *   new Settings(ConfigFactory.parseString("""
  *      spark.master = "some.ip"
  *   """))
  * }}}
  *
  * Any of these can also be overridden by your own application.conf.
  *
  */


case class KafkaConfig(brokers: String, topic: String, group: String)
case class StreamingConfig(batchInterval: FiniteDuration, checkpointDir: String="./checkpoints")
case class EventStoreConfig(endpoint: String="localhost:1100", user: String="admin", password : String="password", database: String="KillrWeather", retries: Int=5)

case class EventStoreTables(
  rawWeather: String = "raw_weather_data",
  skyConditionsLookup: String = "sky_condition_lookup",

  daylyTemperature: String = "daily_aggregate_temperature",
  daylyWind: String = "daily_aggregate_windspeed",
  dailyPressure: String = "daily_aggregate_pressure",
  dailyPrecipitation: String = "daily_aggregate_precip",

  predictedTemperature: String = "daily_predicted_temperature",

  monthlyTemperature: String = "monthly_aggregate_temperature",
  monthlyWind: String = "monthly_aggregate_windspeed",
  monthlyPressure: String = "monthly_aggregate_pressure",
  monthlyPrecipitation: String = "monthly_aggregate_precip"
)

case class ModelListenerConfig(port : Int = 5000, host : String = "localhost")

case class LoaderConfig(publish_interval : String, data_dir : String, batch_size : Int)

case class KillrWeatherAppConfig(appName: String = "KillrWeatherEventStore", local: Boolean = true)


class WeatherSettings(val config: Config) extends Serializable {

  val eventStoreConfig: EventStoreConfig =
    try{
      config.as[EventStoreConfig]("eventstore")
    }
    catch{
      case _:Throwable => EventStoreConfig()
    }

  val kafkaRawConfig: KafkaConfig =
    try {
      config.as[KafkaConfig]("kafkaRaw")
    }
    catch{
      case _:Throwable => KafkaConfig("broker.kafka.l4lb.thisdcos.directory:9092", "killrweather.raw", "killrweather.rawgroup")
    }

  val kafkaDaylyConfig: KafkaConfig =
    try {
      config.as[KafkaConfig]("kafkaDayly")
    }
    catch{
      case _:Throwable => KafkaConfig("broker.kafka.l4lb.thisdcos.directory:9092", "killrweather.dayly", "killrweather.daylygroup")
    }
  val kafkaModelConfig: KafkaConfig =
    try {
      config.as[KafkaConfig]("kafkaModel")
    }
    catch{
      case _:Throwable => KafkaConfig("broker.kafka.l4lb.thisdcos.directory:9092", "killrweather.model", "killrweather.modelgroup")
    }

  val eventStoreTables: EventStoreTables =
    try {
      config.as[EventStoreTables]("tables")
    }
    catch{
      case _:Throwable => EventStoreTables()
    }

  val streamingConfig: StreamingConfig =
    try {
      config.as[StreamingConfig]("streamingConfig")
    }
    catch{
      case _:Throwable => StreamingConfig(new FiniteDuration(5, TimeUnit.SECONDS),"./chkp")
    }

  val killrWeatherAppConfig : KillrWeatherAppConfig =
    try {
      config.as[KillrWeatherAppConfig]("killrweatherApp")
    }
    catch{
      case _:Throwable => KillrWeatherAppConfig()
    }

  val modelListenerConfig : ModelListenerConfig =
    try{
      config.as[ModelListenerConfig]("modelListener")
    }
    catch{
      case _:Throwable => ModelListenerConfig()
    }

  val loaderConfig =
    try {
      config.as[LoaderConfig]("loader")
    }
    catch{
      case _:Throwable => LoaderConfig("1 second", "data/load/", 10)
    }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case w: WeatherSettings =>
        w.config == this.config
      case _:Throwable => false
    }
  }

  override def hashCode(): Int = config.hashCode()
}

object WeatherSettings {

  val baseConfig = ConfigFactory.load()

  def apply(): WeatherSettings = {
    new WeatherSettings(baseConfig)
  }
}