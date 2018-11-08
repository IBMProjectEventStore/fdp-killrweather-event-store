package com.lightbend.killrweather.modellistener

/**
 * Created by boris on 7/17/17.
 *
 * based
 *   https://github.com/DanielaSfregola/quiz-management-service/blob/master/akka-http-crud/src/main/scala/com/danielasfregola/quiz/management/Main.scala
 */

import java.net.InetAddress

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.lightbend.killrweather.modellistener.resources.TemperaturePredictionModelResource
import com.lightbend.killrweather.modellistener.services.RequestService
import com.lightbend.killrweather.settings.WeatherSettings

import scala.concurrent.duration._

object TemperaturePredictionModel extends TemperaturePredictionModelResource {

  def main(args: Array[String]) {

    val settings = WeatherSettings()
    import settings._

    val host = modelListenerConfig.host
    val port = modelListenerConfig.port

    implicit val system = ActorSystem("WeatherDataIngester")
    implicit val materializer = ActorMaterializer()

    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10 seconds)

    println(s"Starting MOdel listener on host $host, port $port, publishing to ")

    val routes: Route = requestRoutes(new RequestService)

    val _ = Http().bindAndHandle(routes, host, port) map {
      binding => println(s"REST interface bound to ${binding.localAddress}") } recover {
      case ex =>
        println(s"REST interface could not bind to $host:$port - ${ex.getMessage}")
    }
  }
}
