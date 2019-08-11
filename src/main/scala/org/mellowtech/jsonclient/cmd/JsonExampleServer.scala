package org.mellowtech.jsonclient.cmd

import akka.actor.{ActorSystem, Terminated}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ContentTypes.{`application/json`, `text/html(UTF-8)`}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.{ActorMaterializer, Materializer}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

import scala.concurrent.{Await, ExecutionContext, Future}


case class JsonKeyValue(key: String, value: String)
case class Responses(responses: Seq[JsonKeyValue])

object JsonCodecs {
  implicit val codec: JsonValueCodec[JsonKeyValue] = JsonCodecMaker.make[JsonKeyValue](CodecMakerConfig())
  implicit val responsesCodec: JsonValueCodec[Responses] = JsonCodecMaker.make[Responses](CodecMakerConfig())

}

class JsonExampleServer(port: Int = 9060)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {

  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)

  var responses: Map[String, String] = Map()

  val binding: Future[ServerBinding] = {
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  def route(implicit m: Materializer): Route = {
    import Directives._
    jsonRoutes ~ rawRoutes
  }

  def jsonRoutes(implicit m: Materializer): Route = {
    import Directives._
    import de.heikoseeberger.akkahttpjsoniterscala.JsoniterScalaSupport._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import com.github.plokhotnyuk.jsoniter_scala.macros._

    import JsonCodecs._

    pathPrefix("json") {
      pathEndOrSingleSlash {
        get {
          val resp = responses.map[JsonKeyValue](kv => {JsonKeyValue(kv._1, kv._2)}).toSeq
          complete(Responses(resp))
        } ~
        post {
          entity(as[JsonKeyValue]){e => {
            responses += ((e.key, e.value))
            complete(e)
          }}
        }
      }
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

  def shutdown(): Future[Http.HttpTerminated] = {
    import scala.concurrent.duration._
    Await.result(binding, 18.seconds).terminate(hardDeadline = 3.seconds)
    /*Await.result(binding, 10.seconds)
      .terminate(hardDeadline = 3.seconds).flatMap(_ => actorSystem.terminate())*/
  }
}
