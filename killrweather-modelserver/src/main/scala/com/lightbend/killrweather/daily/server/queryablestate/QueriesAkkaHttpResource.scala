package com.lightbend.killrweather.daily.server.queryablestate

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask
import com.lightbend.killrweather.daily.model.ModelToServeStats
import com.lightbend.killrweather.daily.server.actors.{GetModelsResult, GetState, GetModels}
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object QueriesAkkaHttpResource extends JacksonSupport {

  implicit val askTimeout = Timeout(30.seconds)

  def storeRoutes(modelserver: ActorRef): Route =
    get {
      path("state"/Segment) { datatype =>
        onSuccess(modelserver ? GetState(datatype)) {
          case info: ModelToServeStats =>
            complete(info)
        }
      } ~
        path("models") {
          onSuccess(modelserver ? GetModels()) {
            case models: GetModelsResult =>
              complete(models)
          }
        }
    }
}
