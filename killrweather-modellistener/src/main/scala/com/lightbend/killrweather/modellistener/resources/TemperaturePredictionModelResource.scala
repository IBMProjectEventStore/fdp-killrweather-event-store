package com.lightbend.killrweather.modellistener.resources

/**
 * Created by boris on 7/17/17.
 *
 * based on https://github.com/DanielaSfregola/quiz-management-service/blob/master/akka-http-crud/src/main/scala/com/danielasfregola/quiz/management/resources/QuestionResource.scala
 */

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes._
import com.lightbend.killrweather.modellistener.routing.JSONResource
import com.lightbend.killrweather.modellistener.services.RequestService

import scala.concurrent.ExecutionContext

trait TemperaturePredictionModelResource extends JSONResource {

  def requestRoutes(requestService: RequestService)(implicit executionContext: ExecutionContext):
  Route = pathPrefix("model") {
    pathEnd {
      post {
        entity(as[ModelSubmissionData]) { request =>
          complete(requestService.processRequest(request).map(_ => OK))
        }
      }
    }
  }
}

case class ModelSubmissionData(wsid: Long, pmml : String)
