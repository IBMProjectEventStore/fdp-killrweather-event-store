package com.lightbend.killrweather.daily.model.PMML

/**
 * Created by boris on 5/9/17.
 *
 * Class for PMML model
 */

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.dmg.pmml.{FieldName, PMML}
import org.jpmml.evaluator.visitors._
import org.jpmml.evaluator.{Computable, FieldValue, ModelEvaluatorFactory, TargetField}
import org.jpmml.model.PMMLUtil
import com.lightbend.killrweather.daily.model.{Model, ModelFactory, ModelToServe, TemperaturePredictionInput}
import com.lightbend.model.modeldescriptor.ModelDescriptor

import scala.collection.JavaConversions._
import scala.collection._

/**
 * Handle models provided using PMML, to score "Records".
 */
class PMMLModel(inputStream: Array[Byte]) extends Model {

  import PMMLModel._

  var arguments = mutable.Map[FieldName, FieldValue]()

  // Marshall PMML
  val pmml = PMMLUtil.unmarshal(new ByteArrayInputStream(inputStream))

  PMMLB

  // Optimize model// Optimize model
  PMMLModel.optimize(pmml)

  // Create and verify evaluator
  val evaluator = ModelEvaluatorFactory.newInstance.newModelEvaluator(pmml)
  evaluator.verify()

  // Get input/target fields
  val inputFields = evaluator.getInputFields
  val target: TargetField = evaluator.getTargetFields.get(0)
  val tname = target.getName

  override def score(input : Any): Any = {
    val record = input.asInstanceOf[TemperaturePredictionInput]
    arguments.clear()
    inputFields.foreach(field => arguments.put(field.getName, field.prepare(getValue(field.getName.getValue, record.temps))))

    // Calculate Output// Calculate Output
    val result = evaluator.evaluate(arguments)

    // Prepare output
    result.get(tname) match {
      case c: Computable => c.getResult.toString.toDouble
      case v => v
    }
  }

  override def cleanup(): Unit = {}

  override def toBytes: Array[Byte] = {
    var stream = new ByteArrayOutputStream()
    PMMLUtil.marshal(pmml, stream)
    stream.toByteArray
  }

  override def getType: Long = ModelDescriptor.ModelType.PMML.value.asInstanceOf[Long]
}

object PMMLModel extends ModelFactory {

  private val optimizers = Array(new ExpressionOptimizer, new FieldOptimizer, new PredicateOptimizer,
    new GeneralRegressionModelOptimizer, new NaiveBayesModelOptimizer, new RegressionModelOptimizer)
  def optimize(pmml: PMML) = this.synchronized {
    optimizers.foreach(opt =>
      try {
        opt.applyTo(pmml)
      } catch {
        case t: Throwable => {
          println(s"Error optimizing model for optimizer $opt")
          t.printStackTrace()
          throw t
        }
      })
  }

  def getValue(name : String, temps : Array[Double]) = name match {
    case n if (n.equals("day-1")) => temps(0)
    case n if (n.equals("day-2")) => temps(1)
    case n if (n.equals("day-3")) => temps(2)
    case _ => 0.0
  }

  override def create(input: ModelToServe): Model = {
      new PMMLModel(input.model)
  }

  override def restore(bytes: Array[Byte]): Model = new PMMLModel(bytes)
}
