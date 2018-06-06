package com.lightbend.killrweather.EventStore

import com.ibm.event.catalog.{ColumnOrder, IndexSpecification, SortSpecification, TableSchema}
import com.ibm.event.common.ConfigurationReader
import com.ibm.event.oltp.EventContext
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.spark.sql.types._

object EventStoreSupport {

  val settings = WeatherSettings()
  import settings._

  /*
  val weather_station = TableSchema("rweather_station", StructType(Array(
    StructField("id", StringType, nullable = false),
    StructField("name", StringType, nullable = false),
    StructField("country_code", StringType, nullable = false),
    StructField("state_cod", StringType, nullable = false),
    StructField("call_sign", StringType, nullable = false),
    StructField("lat", DoubleType, nullable = false),
    StructField("long", DoubleType, nullable = false),
    StructField("elevation", DoubleType, nullable = false)
    )),
    shardingColumns = Seq("id"),
    pkColumns = Seq("id")
  )
*/
  val raw_weather_data = TableSchema(eventStoreTables.rawWeather, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("hour", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("temperature", DoubleType, nullable = false),
    StructField("dewpoint", DoubleType, nullable = false),
    StructField("pressure", DoubleType, nullable = false),
    StructField("wind_direction", IntegerType, nullable = false),
    StructField("wind_speed", DoubleType, nullable = false),
    StructField("sky_condition", IntegerType, nullable = false),
    StructField("sky_condition_text", StringType, nullable = false),
    StructField("one_hour_precip", DoubleType, nullable = false),
    StructField("six_hour_precip", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
    val raw_weather_data_index = IndexSpecification("RawWeatherDataIndex", raw_weather_data, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("temperature", "dewpoint", "pressure"))

  /*
  val indexSpec = IndexSpecification("pkindex",
    raw_weather_data,
    equalColumns= Seq("deviceId","metricId"),
    sortColumns=Seq(SortSpecification("timeStamp", ColumnOrder.AscendingNullsLast)),
    includeColumns=Seq("metricValue"))
*/
  val sky_condition_lookup = TableSchema(eventStoreTables.skyConditionsLookup, StructType(Array(
    StructField("code", IntegerType, nullable = false),
    StructField("condition", StringType, nullable = false)
  )),
    shardingColumns = Seq("code"),
    pkColumns = Seq("code"))

  val daily_aggregate_temperature = TableSchema(eventStoreTables.daylyTemperature, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_aggregate_temperature_index = IndexSpecification("DailyAggTempIndex", daily_aggregate_temperature, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val daily_predicted_temperature = TableSchema(eventStoreTables.predictedTemperature, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("prediction", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_predicted_temperature_index = IndexSpecification("DailyPreTempIndex", daily_predicted_temperature, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("prediction"))

  val daily_aggregate_windspeed = TableSchema(eventStoreTables.daylyWind, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_aggregate_windspeed_index = IndexSpecification("DailyAggWindIndex", daily_aggregate_windspeed, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val daily_aggregate_pressure = TableSchema(eventStoreTables.dailyPressure, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_aggregate_pressure_index = IndexSpecification("DailyAggPressureIndex", daily_aggregate_pressure, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val daily_aggregate_precip = TableSchema(eventStoreTables.dailyPrecipitation, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("precipitation", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_aggregate_precip_index = IndexSpecification("DailyAggPrecipIndex", daily_aggregate_precip, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("precipitation"))

  val monthly_aggregate_temperature = TableSchema(eventStoreTables.monthlyTemperature, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val monthly_aggregate_temperature_index = IndexSpecification("MonthlyAggTempIndex", monthly_aggregate_temperature, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val monthly_aggregate_windspeed = TableSchema(eventStoreTables.monthlyWind, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val monthly_aggregate_windspeed_index = IndexSpecification("MonthlyAggWindIndex", monthly_aggregate_windspeed, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val monthly_aggregate_pressure = TableSchema(eventStoreTables.monthlyPressure, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val monthly_aggregate_pressure_index = IndexSpecification("MonthlyAggPressureIndex", monthly_aggregate_pressure, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val monthly_aggregate_precip = TableSchema(eventStoreTables.monthlyPrecipitation, StructType(Array(
    StructField("wsid", LongType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val monthly_aggregate_precip_index = IndexSpecification("MonthlyAggPrecipIndex", monthly_aggregate_precip, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("high", "low", "mean"))

  val emptyIndex: IndexSpecification = null
  val tables = Map [String, (TableSchema, IndexSpecification)] (
    (eventStoreTables.rawWeather, (raw_weather_data, raw_weather_data_index)),
    (eventStoreTables.skyConditionsLookup, (sky_condition_lookup, emptyIndex)),
    (eventStoreTables.daylyTemperature, (daily_aggregate_temperature, daily_aggregate_temperature_index)),
    (eventStoreTables.daylyWind, (daily_aggregate_windspeed, daily_aggregate_windspeed_index)),
    (eventStoreTables.dailyPressure, (daily_aggregate_pressure, daily_aggregate_pressure_index)),
    (eventStoreTables.dailyPrecipitation, (daily_aggregate_precip, daily_aggregate_precip_index)),
    (eventStoreTables.monthlyTemperature, (monthly_aggregate_temperature, monthly_aggregate_temperature_index)),
    (eventStoreTables.monthlyWind, (monthly_aggregate_windspeed, monthly_aggregate_windspeed_index)),
    (eventStoreTables.monthlyPressure, (monthly_aggregate_pressure, monthly_aggregate_pressure_index)),
    (eventStoreTables.monthlyPrecipitation, (monthly_aggregate_precip, monthly_aggregate_precip_index)),
    (eventStoreTables.predictedTemperature, (daily_predicted_temperature, daily_predicted_temperature_index))
  )

  def createContext(connectionEndpoints: String, user: String, password: String): Option[EventContext] = {
    ConfigurationReader.setUseFrontendConnectionEndpoints(true)
    ConfigurationReader.setConnectionEndpoints(connectionEndpoints)
    ConfigurationReader.setEventUser(user)
    ConfigurationReader.setEventPassword(password)
    try {
      Some(EventContext.createDatabase(eventStoreConfig.database))
    } catch {
      case e: Throwable => {
        try {
          EventContext.openDatabase(eventStoreConfig.database)
          Some(EventContext.getEventContext)
        }catch {
          case e: Throwable => None
        }
      }
    }
  }

  private def currentTables(ctx: EventContext) : List[String] = {

    var tables : List[String] = List()
    var success = false
    var attempts = 0
    while (!success || (attempts > eventStoreConfig.retries)) {
      try {
        attempts = attempts + 1
        tables = ctx.getNamesOfTables.toList
        success = true
      }
      catch {
        case t: Throwable =>
          println(s"Error getting existing tables with message: ${t.getMessage}")
          Thread.sleep(10)
      }
    }
    tables
  }

  def ensureTables(ctx: EventContext): Unit = {

    val existing = currentTables(ctx)
    println(s"Tables : ${existing.mkString(",")}")
    tables foreach (tabDef => {
      val tableSchema = tabDef._2._1
      val index = tabDef._2._2
      if (!existing.contains(tabDef._1) && index != null) {
        ctx.createTableWithIndex(tableSchema, index)
        println(s"Table ${tabDef._1} created with Index")
      } else if (!existing.contains(tabDef._1)) {
        ctx.createTable(tableSchema)
        println(s"Table ${tabDef._1} created")
      } else {
        println(s"Table ${tabDef._1} exist")
      }
    })
  }
}