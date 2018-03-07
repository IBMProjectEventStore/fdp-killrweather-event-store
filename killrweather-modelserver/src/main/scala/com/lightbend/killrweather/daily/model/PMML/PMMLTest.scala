package com.lightbend.killrweather.daily.model.PMML

import java.nio.file.{Files, Paths}

import com.lightbend.killrweather.daily.model.TemperaturePredictionInput

object PMMLTest {
  val modelFile = "ml-model/weather-prediction.pmml"

  def main(args: Array[String]) {
    val byteArray = Files.readAllBytes(Paths.get(modelFile))
    val model = new PMMLModel(byteArray)

    for (i <- 1 to 5){
      val inputs = Array(i*3.0, (i+5)*2.0)
      val prediction = model.score(TemperaturePredictionInput(inputs)).asInstanceOf[Double]
      println(s"Scoring input ${inputs(0)},${inputs(1)} - result $prediction")
    }
  }
}
