package com.lightbend.killrweather.EventStore

import com.ibm.event.catalog.{ColumnOrder, IndexSpecification, SortSpecification, TableSchema}
import com.ibm.event.common.ConfigurationReader
import com.ibm.event.oltp.EventContext
import org.apache.spark.sql.types._

object EventStoreSupport {

  import com.lightbend.killrweather.settings.WeatherSettings._
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
  val raw_weather_data = TableSchema(RAWWEATHER, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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
  val sky_condition_lookup = TableSchema(SKYCONDITIONSLOOKUP, StructType(Array(
    StructField("code", IntegerType, nullable = false),
    StructField("condition", StringType, nullable = false)
  )),
    shardingColumns = Seq("code"),
    pkColumns = Seq("code"))

  val daily_aggregate_temperature = TableSchema(DAYLYTEMP, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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

  val daily_predicted_temperature = TableSchema(PREDICTTEMP, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("prediction", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_predicted_temperature_index = IndexSpecification("DailyPreTempIndex", daily_predicted_temperature, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("prediction"))

  val daily_aggregate_windspeed = TableSchema(DAYLYWIND, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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

  val daily_aggregate_pressure = TableSchema(DAYLYPRESS, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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

  val daily_aggregate_precip = TableSchema(DAYLYPRECIP, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("precipitation", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("ts", "wsid"),
    pkColumns = Seq("ts", "wsid"))
  val daily_aggregate_precip_index = IndexSpecification("DailyAggPrecipIndex", daily_aggregate_precip, equalColumns = Seq("wsid"), sortColumns = Seq(SortSpecification("ts", ColumnOrder.AscendingNullsLast)), includeColumns = Seq("precipitation"))

  val monthly_aggregate_temperature = TableSchema(MONTHLYTEMP, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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

  val monthly_aggregate_windspeed = TableSchema(MONTHLYWIND, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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

  val monthly_aggregate_pressure = TableSchema(MONTHLYPRESS, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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

  val monthly_aggregate_precip = TableSchema(MONTHLYPRECIP, StructType(Array(
    StructField("wsid", IntegerType, nullable = false),
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
    (RAWWEATHER, (raw_weather_data, raw_weather_data_index)),
    (SKYCONDITIONSLOOKUP, (sky_condition_lookup, emptyIndex)),
    (DAYLYTEMP, (daily_aggregate_temperature, daily_aggregate_temperature_index)),
    (DAYLYWIND, (daily_aggregate_windspeed, daily_aggregate_windspeed_index)),
    (DAYLYPRESS, (daily_aggregate_pressure, daily_aggregate_pressure_index)),
    (DAYLYPRECIP, (daily_aggregate_precip, daily_aggregate_precip_index)),
    (MONTHLYTEMP, (monthly_aggregate_temperature, monthly_aggregate_temperature_index)),
    (MONTHLYWIND, (monthly_aggregate_windspeed, monthly_aggregate_windspeed_index)),
    (MONTHLYPRESS, (monthly_aggregate_pressure, monthly_aggregate_pressure_index)),
    (MONTHLYPRECIP, (monthly_aggregate_precip, monthly_aggregate_precip_index)),
    (PREDICTTEMP, (daily_predicted_temperature, daily_predicted_temperature_index))
  )

  def createContext(connectionEndpoints: String, user: String, password: String): Option[EventContext] = {
    ConfigurationReader.setUseFrontendConnectionEndpoints(true)
    ConfigurationReader.setConnectionEndpoints(connectionEndpoints)
    ConfigurationReader.setEventUser(user)
    ConfigurationReader.setEventPassword(password)
    try {
      Some(EventContext.createDatabase(DBNAME))
    } catch {
      case e: Throwable => {
        try {
          EventContext.openDatabase(DBNAME)
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
    while (!success || (attempts > RETRIES)) {
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