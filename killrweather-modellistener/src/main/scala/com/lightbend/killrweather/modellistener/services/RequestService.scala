package com.lightbend.killrweather.modellistener.services

import java.io.ByteArrayOutputStream

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import akka.kafka.scaladsl.Producer
import com.google.protobuf.ByteString
import com.lightbend.killrweather.modellistener.resources.ModelSubmissionData
import com.lightbend.killrweather.settings.WeatherSettings._
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.modeldescriptor.ModelDescriptor.ModelType

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by boris on 7/17/17.
 * based on
 *   https://github.com/DanielaSfregola/quiz-management-service/blob/master/akka-http-crud/src/main/scala/com/danielasfregola/quiz/management/services/QuestionService.scala
 */
class RequestService(implicit executionContext: ExecutionContext, materializer: ActorMaterializer, system: ActorSystem) {

  import RequestService._

  println(s"Running HTTP Client. Kafka: $kafkaBrokers")

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
    .withBootstrapServers(kafkaBrokers)

  def processRequest(model: ModelSubmissionData): Future[Unit] = Future {
    //    Source.single(report).runWith(Sink.foreach(println))
    println(s"Model Listener new request for wsid ${model.wsid} with PMML ${model.pmml}")
    val _ = Source.single(model).map { m =>
      new ProducerRecord[Array[Byte], Array[Byte]](KafkaTopicModel, convertModel(m))
    }.runWith(Producer.plainSink(producerSettings))
  }
}

object RequestService {

  private val bos = new ByteArrayOutputStream()

  def apply(implicit executionContext: ExecutionContext, materializer: ActorMaterializer, system: ActorSystem): RequestService = new RequestService()

  def convertModel(model: ModelSubmissionData): Array[Byte] = {
    bos.reset
    new ModelDescriptor().withName("Provided by DSX").withDescription("Provided by DSX")
        .withDataType(model.wsid).withModeltype(ModelType.PMML).withData(ByteString.copyFrom(model.pmml.getBytes)).writeTo(bos)
    bos.toByteArray
  }
}