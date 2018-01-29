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

final object WeatherSettings{

  // val AppName: String = "KillrWeather"  // Don't use, as different apps use WeatherSettings

  val localAddress = "localhost" //InetAddress.getLocalHost.getHostAddress

  val SparkCleanerTtl = (3600 * 2)

  val SparkStreamingBatchInterval = 5000L

  val SparkCheckpointDir = "./checkpoints/"

  val kafkaBrokers = "localhost:9092"
  val KafkaGroupId = "killrweather.group"
  val KafkaTopicRaw = "killrweather.raw"

  // Event Store
  val DBNAME = "KillrWeather"

  val RAWWEATHER = "raw_weather_data"
  val SKYCONDITIONSLOOKUP = "sky_condition_lookup"

  val DAYLYTEMP = "daily_aggregate_temperature"
  val DAYLYWIND = "daily_aggregate_windspeed"
  val DAYLYPRESS = "daily_aggregate_pressure"
  val DAYLYPRECIP = "daily_aggregate_precip"

  val MONTHLYTEMP = "monthly_aggregate_temperature"
  val MONTHLYWIND = "monthly_aggregate_windspeed"
  val MONTHLYPRESS = "monthly_aggregate_pressure"
  val MONTHLYPRECIP = "monthly_aggregate_precip"

  val RETRIES = 5
}