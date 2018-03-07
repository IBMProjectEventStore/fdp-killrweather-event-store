package com.lightbend.killrweather.daily.server.modelserver

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import akka.pattern.ask
import akka.stream.scaladsl.Sink
import com.lightbend.killrweather.daily.model.{DataRecord, ModelToServe, ServingResult}
import com.lightbend.killrweather.daily.server.actors.ModelServingManager
import com.lightbend.killrweather.daily.server.queryablestate.QueriesAkkaHttpResource
import com.lightbend.killrweather.settings.WeatherSettings._
import com.lightbend.scala.modelServer.model.ModelWithDescriptor

import scala.concurrent.duration._
import scala.util.Success

/**
 * Created by boris on 7/21/17.
 */
object AkkaModelServer {



  implicit val system = ActorSystem("ModelServing")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val askTimeout = Timeout(30.seconds)

  println(s"Using kafka brokers at ${kafkaBrokers} ")

  val dataConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(kafkaBrokers)
    .withGroupId(KafkaModelDataGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val modelConsumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(kafkaBrokers)
    .withGroupId(KafkaModelGroupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def main(args: Array[String]): Unit = {

    val modelserver = system.actorOf(ModelServingManager.props)

    // Model stream processing
    Consumer.atMostOnceSource(modelConsumerSettings, Subscriptions.topics(KafkaTopicDaily))
      .map(record => ModelToServe.fromByteArray(record.value)).collect { case Success(a) => a }
      .map(record => ModelWithDescriptor.fromModelToServe(record)).collect { case Success(a) => a }
      .mapAsync(1)(elem => modelserver ? elem)
      .runWith(Sink.ignore) // run the stream, we do not read the results directly

    // Data stream processing
    Consumer.atMostOnceSource(dataConsumerSettings, Subscriptions.topics(KafkaTopicModel))
      .map(record => DataRecord.fromByteArray(record.value)).collect { case Success(a) => a }
      .mapAsync(1)(elem => (modelserver ? elem).mapTo[ServingResult])
      .runForeach(result => {
        result.processed match {
          case true => {
            println(s"Calculated quality - ${result.result} calculated in ${result.duration} ms")
            // Write to ES
          }
          case _ => println ("No model available - skipping")
        }
      })

    // Rest Server
    startRest(modelserver)
  }

  // See http://localhost:5500/models
  // Then select a model shown and try http://localhost:5500/state/<model>, e.g., http://localhost:5500/state/wine
  def startRest(modelserver: ActorRef): Unit = {

    implicit val timeout = Timeout(10.seconds)
    val host = "127.0.0.1"
    val port = 5500
    val routes: Route = QueriesAkkaHttpResource.storeRoutes(modelserver)

    Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
        case ex =>
          println(s"Models observer could not bind to $host:$port - ${ex.getMessage}")
      }
  }
}