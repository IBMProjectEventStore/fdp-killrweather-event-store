package com.lightbend.killrweather.app

import com.ibm.event.oltp.EventContext
import com.ibm.event.common.ConfigurationReader
import org.apache.spark.sql.ibm.event.EventSession
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.spark.{SparkConf, SparkContext}

object EventStoreDataReader {

  def main(args: Array[String]): Unit = {

    // Create context
    val settings = WeatherSettings()
    import settings._

    ConfigurationReader.setUseFrontendConnectionEndpoints(true)
    ConfigurationReader.setConnectionEndpoints(eventStoreConfig.endpoint)
    ConfigurationReader.setEventUser(eventStoreConfig.user)
    ConfigurationReader.setEventPassword(eventStoreConfig.password)

    val sc = new SparkContext(new SparkConf().setAppName("DataBase reader").setMaster("local[3]"))

    try {
      val sqlContext = new EventSession(sc, eventStoreConfig.database)
      sqlContext.openDatabase()

      println("Raw data")
      sqlContext.loadEventTable(eventStoreTables.rawWeather).createOrReplaceTempView(eventStoreTables.rawWeather)
      val raw_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.rawWeather}")
      raw_results.show()


      println("Daily temperature")
      sqlContext.loadEventTable(eventStoreTables.daylyTemperature).createOrReplaceTempView(eventStoreTables.daylyTemperature)
      val dt_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.daylyTemperature}")
      dt_results.show()
      println("Daily wind")
      sqlContext.loadEventTable(eventStoreTables.daylyWind).createOrReplaceTempView(eventStoreTables.daylyWind)
      val dw_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.daylyWind}")
      dw_results.show()
      println("Daily pressure")
      sqlContext.loadEventTable(eventStoreTables.dailyPressure).createOrReplaceTempView(eventStoreTables.dailyPressure)
      val dp_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.dailyPressure}")
      dp_results.show()
      println("Daily precipitation")
      sqlContext.loadEventTable(eventStoreTables.dailyPrecipitation).createOrReplaceTempView(eventStoreTables.dailyPrecipitation)
      val dpp_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.dailyPrecipitation}")
      dpp_results.show()


      println("Monthly temperature")
      sqlContext.loadEventTable(eventStoreTables.monthlyTemperature).createOrReplaceTempView(eventStoreTables.monthlyTemperature)
      val mt_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.monthlyTemperature}")
      mt_results.show()
      println("Monthly wind")
      sqlContext.loadEventTable(eventStoreTables.monthlyWind).createOrReplaceTempView(eventStoreTables.monthlyWind)
      val mw_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.monthlyWind}")
      mw_results.show()
      println("Monthly pressure")
      sqlContext.loadEventTable(eventStoreTables.monthlyPressure).createOrReplaceTempView(eventStoreTables.monthlyPressure)
      val mp_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.monthlyPressure}")
      mp_results.show()
      println("monthly precipitation")
      sqlContext.loadEventTable(eventStoreTables.monthlyPrecipitation).createOrReplaceTempView(eventStoreTables.monthlyPrecipitation)
      val mpp_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.monthlyPrecipitation}")
      mpp_results.show()

      println("Predicted temperature")
      sqlContext.loadEventTable(eventStoreTables.predictedTemperature).createOrReplaceTempView(eventStoreTables.predictedTemperature)
      val pt_results = sqlContext.sql(s"SELECT * FROM ${eventStoreTables.predictedTemperature}")
      pt_results.show()
    } catch {
      case e: Exception =>
        println("EXCEPTION: attempting to exit..." + e.getMessage)
        e.printStackTrace()
    }

    EventContext.cleanUp()
    sys.exit()
  }
}
