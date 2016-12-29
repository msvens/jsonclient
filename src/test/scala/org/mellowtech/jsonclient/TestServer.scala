package org.mellowtech.jsonclient

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get}
import akka.stream.{ActorMaterializer, Materializer}
import akka.http.scaladsl.server.{Directives, Route}
import org.json4s.native

import scala.concurrent.{ExecutionContext, Future}

case class TestJson(m: String, i: Int)
case class WrongJson(m: String = "wrong", m1: String = "json")

/**
  * @author msvens
  * @since 2016-12-25
  */
class TestServer {

  implicit val actorSystem = ActorSystem()
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val binding: Future[ServerBinding] = {
    Http().bindAndHandle(route, "0.0.0.0", 9050)
  }

  def route(implicit m: Materializer): Route = {
    import Directives._
    jsonRoutes ~ rawRoutes

  }

  def jsonRoutes(implicit m: Materializer): Route = {
    import Directives._
    import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
    implicit val formats = org.json4s.DefaultFormats
    implicit val serialization = native.Serialization

    path("json"){
      get{
        complete(TestJson("message", 1))
      } ~
      post{
        entity(as[TestJson]) { tj =>
          val i2 = tj.i * 2
          val m2 = tj.m + tj.m
          complete(TestJson(m2, i2))
        }
      }
    } ~
    path("jsonmiss") {
      get{
        complete((InternalServerError,"could not compute"))
      }
    } ~
    path("jsonwrong") {
      get(complete(WrongJson()))
    }
  }

  def rawRoutes(implicit m: Materializer): Route = {
    import Directives._

    path("html") {
      get {
        complete(HttpEntity(`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    } ~
    path("empty") {
      get {
        complete(HttpEntity(`application/json`,""))
      }
    } ~
    path("rawmiss") {
      complete((InternalServerError,"could not compute"))
    }
  }

  def shutdown(): Future[Terminated] ={
    actorSystem.terminate()
  }

}
