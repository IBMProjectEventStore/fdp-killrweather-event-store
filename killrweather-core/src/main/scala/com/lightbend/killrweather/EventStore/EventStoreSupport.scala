package com.lightbend.killrweather.EventStore

import com.ibm.event.catalog.{ /*ColumnOrder, IndexSpecification, SortSpecification,*/ TableSchema }
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
    StructField("wsid", StringType, nullable = false),
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
    shardingColumns = Seq("year", "month", "day"),
    pkColumns = Seq("year", "month", "day", "hour"))
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
    StructField("wsid", StringType, nullable = false),
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
    shardingColumns = Seq("year", "month"),
    pkColumns = Seq("year", "month", "day"))

  val daily_aggregate_windspeed = TableSchema(DAYLYWIND, StructType(Array(
    StructField("wsid", StringType, nullable = false),
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
    shardingColumns = Seq("year", "month"),
    pkColumns = Seq("year", "month", "day"))

  val daily_aggregate_pressure = TableSchema(DAYLYPRESS, StructType(Array(
    StructField("wsid", StringType, nullable = false),
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
    shardingColumns = Seq("year", "month"),
    pkColumns = Seq("year", "month", "day"))

  val daily_aggregate_precip = TableSchema(DAYLYPRECIP, StructType(Array(
    StructField("wsid", StringType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("day", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("precipitation", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("year", "month"),
    pkColumns = Seq("year", "month", "day"))

  val monthly_aggregate_temperature = TableSchema(MONTHLYTEMP, StructType(Array(
    StructField("wsid", StringType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("year"),
    pkColumns = Seq("year", "month"))

  val monthly_aggregate_windspeed = TableSchema(MONTHLYWIND, StructType(Array(
    StructField("wsid", StringType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("year"),
    pkColumns = Seq("year", "month"))

  val monthly_aggregate_pressure = TableSchema(MONTHLYPRESS, StructType(Array(
    StructField("wsid", StringType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("year"),
    pkColumns = Seq("year", "month"))

  val monthly_aggregate_precip = TableSchema(MONTHLYPRECIP, StructType(Array(
    StructField("wsid", StringType, nullable = false),
    StructField("year", IntegerType, nullable = false),
    StructField("month", IntegerType, nullable = false),
    StructField("ts", LongType, nullable = false),
    StructField("high", DoubleType, nullable = false),
    StructField("low", DoubleType, nullable = false),
    StructField("mean", DoubleType, nullable = false),
    StructField("variance", DoubleType, nullable = false),
    StructField("stdev", DoubleType, nullable = false)
  )),
    shardingColumns = Seq("year"),
    pkColumns = Seq("year", "month"))

  val tables = Map( //"weather_station" -> weather_station,
    RAWWEATHER -> raw_weather_data,
    SKYCONDITIONSLOOKUP -> sky_condition_lookup,
    DAYLYTEMP -> daily_aggregate_temperature,
    DAYLYWIND -> daily_aggregate_windspeed,
    DAYLYPRESS -> daily_aggregate_pressure,
    DAYLYPRECIP -> daily_aggregate_precip,
    MONTHLYTEMP -> monthly_aggregate_temperature,
    MONTHLYWIND -> monthly_aggregate_windspeed,
    MONTHLYPRESS -> monthly_aggregate_pressure,
    MONTHLYPRECIP -> monthly_aggregate_precip
  )

  def createContext(connectionEndpoints: String): Option[EventContext] = {
    ConfigurationReader.setConnectionEndpoints(connectionEndpoints)
    ConfigurationReader.setUseFrontendConnectionEndpoints(true)
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
          println(s"Error getting existing tables")
          Thread.sleep(10)
      }
    }
    tables
  }

  def ensureTables(ctx: EventContext): Unit = {

    val existing = currentTables(ctx)
    println(s"Tables : ${existing.mkString(",")}")
    tables foreach (tabDef => {
      if (!existing.contains(tabDef._1)) {
        ctx.createTable(tabDef._2)
        println(s"Table ${tabDef._1} created")
      } else
        println(s"Table ${tabDef._1} exist")
    })
  }
}