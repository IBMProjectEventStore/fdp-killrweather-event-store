package com.lightbend.killrweather.app

import com.ibm.event.common.ConfigurationReader
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.ibm.event.EventSession
import org.apache.spark.sql.functions.{col, lag, round}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.ml.Pipeline
import org.jpmml.model.MetroJAXBUtil
import org.jpmml.sparkml.PMMLBuilder

object temperatureML {

  def main(args: Array[String]): Unit = {

    // Create context
    val settings = WeatherSettings()
    import settings._

    ConfigurationReader.setUseFrontendConnectionEndpoints(true)
    ConfigurationReader.setConnectionEndpoints(eventStoreConfig.endpoint)
    ConfigurationReader.setEventUser(eventStoreConfig.user)
    ConfigurationReader.setEventPassword(eventStoreConfig.password)

    val sc = new SparkContext(new SparkConf().setAppName("DataBase reader").setMaster("local[3]"))

    val sqlContext = new EventSession(sc, eventStoreConfig.database)
    sqlContext.sparkContext.setLogLevel("ERROR")

    sqlContext.openDatabase()
    val dfDailyTemp = sqlContext.loadEventTable(eventStoreTables.daylyTemperature)

    dfDailyTemp.show(5)

    val weatherStations = dfDailyTemp.select("wsid").distinct.collect.flatMap(_.toSeq)
    weatherStations.foreach(println(_))

    for (weatherStationID <- weatherStations) {

      //val weatherStationID = weatherStations(1)

      val dfDailyTempstation = dfDailyTemp.filter(s"wsid == $weatherStationID")
      //    dfDailyTempstation.show(10)

      val w = org.apache.spark.sql.expressions.Window.orderBy("year", "month", "day")
      val dfTrain = dfDailyTempstation.withColumn("day-1", lag(col("mean"), 1, null).over(w)).
        withColumn("day-2", lag(col("mean"), 2, null).over(w)).
        withColumn("day-3", lag(col("mean"), 3, null).over(w))

      dfTrain.select("mean", "day-1", "day-2", "day-3").show()

      val dfTrain2 = dfTrain.withColumn("day-1", round(col("day-1"), 1)).
        withColumn("day-2", round(col("day-2"), 1)).
        withColumn("day-3", round(col("day-3"), 1))

      val dfTrain3 = dfTrain2.na.drop()

      dfTrain3.select("day-1", "day-2", "day-3").show()

      val splits = dfTrain3.randomSplit(Array(0.8, 0.20), seed = 24L)
      val training_data = splits(0)
      val test_data = splits(1)

      val features_assembler = new VectorAssembler().
        setInputCols(Array("day-1", "day-2", "day-3")).
        setOutputCol("features")

      val lr = new LinearRegression().
        setMaxIter(50).
        setRegParam(0.3).
        setElasticNetParam(0.8).
        setLabelCol("mean").
        setFeaturesCol("features")

      val pipeline = new Pipeline().setStages(Array(features_assembler, lr))

      val linearRegressionModel = pipeline.fit(training_data)

      val lrModel = linearRegressionModel.stages(1).asInstanceOf[LinearRegressionModel]
      // Summarize the model over the training set and print out some metrics
      // Summarize the model over the training set and print out some metrics
      val summary = lrModel.summary
      println(s"Total iterations: ${summary.totalIterations}")
      println(s"Coefficients: ${lrModel.coefficients.toArray.mkString(",")}")

      val predictions = linearRegressionModel.transform(test_data)
      predictions.select("prediction").show()

      // PMML
      val pmml = new PMMLBuilder(training_data.schema, linearRegressionModel).build()
      println(s"PMML for the wither station $weatherStationID")
      MetroJAXBUtil.marshalPMML(pmml, System.out)
    }
    sc.stop()
  }
}