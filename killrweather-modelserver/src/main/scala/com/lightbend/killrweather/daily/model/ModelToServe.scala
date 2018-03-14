package com.lightbend.killrweather.daily.model

import java.util.Calendar

import com.lightbend.model.modeldescriptor.ModelDescriptor

import scala.util.Try

/**
 * Created by boris on 5/8/17.
 */
object ModelToServe {
  def fromByteArray(message: Array[Byte]): Try[ModelToServe] = Try {
    val m = ModelDescriptor.parseFrom(message)
    m.messageContent.isData match {
      case true => new ModelToServe(m.name, m.description, m.modeltype, m.getData.toByteArray, m.dataType)
      case _ => throw new Exception("Location based is not yet supported")
    }
  }
}

case class ModelToServe(name: String, description: String,
  modelType: ModelDescriptor.ModelType, model: Array[Byte], dataType: Long) {}

case class ServingResult(processed : Boolean, result: Double = .0, duration: Long = 0l, wsid : Long = 0l, ts : Long = 0l)

object ServingResult{
  val DAY = 1000 * 3600 * 24        // Day in millisec
  val noModel = ServingResult(false)
  def apply(result: Double, duration: Long, wsid : Long, ts : Long): ServingResult =
    ServingResult(true, result, duration, wsid, ts)
  def convertTS(result : ServingResult) : (Int, Int, Int, Long) = {
    val date = Calendar.getInstance()
    date.setTimeInMillis(result.ts + DAY)
    (date.get(Calendar.YEAR),date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), date.getTimeInMillis)
  }
}