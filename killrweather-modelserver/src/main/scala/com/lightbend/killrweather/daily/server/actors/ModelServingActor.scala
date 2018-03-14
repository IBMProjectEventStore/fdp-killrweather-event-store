package com.lightbend.killrweather.daily.server.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import com.lightbend.killrweather.WeatherClient.TemperatureDailyRecord
import com.lightbend.killrweather.daily.model._
import com.lightbend.scala.modelServer.model.ModelWithDescriptor

// Workhorse - doing model serving for a given data type

class ModelServingActor(dataType : String) extends Actor {

  println(s"Creating model serving actor $dataType")
  private var currentModel: Option[Model] = None
  private var newModel: Option[Model] = None
  private var currentState: Option[ModelToServeStats] = None
  private var newState: Option[ModelToServeStats] = None
  private val temperatures = new Array[Option[Double]](3)
  temperatures(0) = None
  temperatures(1) = None
  temperatures(2) = None

  private def updateTemperatures(temperature : Double) : Unit = {
    temperatures(2) = temperatures(1)
    temperatures(1) = temperatures(0)
    temperatures(0) = Some(temperature)
  }

  private def haveTemperatures() : Boolean =
    temperatures(2).isDefined && temperatures(1).isDefined && temperatures(0).isDefined

  override def receive = {
    case model : ModelWithDescriptor =>
      // Update model
      println(s"Updated model: $model")
      newState = Some(ModelToServeStats(model.descriptor))
      newModel = Some(model.model)
      sender() ! "Done"

    case record : TemperatureDailyRecord =>
      // Process data
      newModel.foreach { model =>
        // Update model
        // close current model first
        currentModel.foreach(_.cleanup())
        // Update model
        currentModel = newModel
        currentState = newState
        newModel = None
      }

      updateTemperatures(record.temperature)
      currentModel match {
        case Some(model) if(haveTemperatures()) =>
          val start = System.nanoTime()
          val prediction = model.score(TemperaturePredictionInput(temperatures.map(_.get))).asInstanceOf[Double]
          val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
          currentState = currentState.map(_.incrementUsage(duration))
          sender() ! ServingResult(prediction, duration, record.wsid, record.ts)

        case _ =>
          sender() ! ServingResult.noModel
      }

    case request : GetState => {
      // State query
      sender() ! currentState.getOrElse(ModelToServeStats.empty)
    }
  }
}

object ModelServingActor{
  def props(dataType : String) : Props = Props(new ModelServingActor(dataType))
}

case class GetState(dataType : String)
