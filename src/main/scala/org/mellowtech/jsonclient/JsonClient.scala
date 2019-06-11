package org.mellowtech.jsonclient

import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshalling.Marshal
import java.nio.charset.Charset


import scala.concurrent.{ExecutionContext, Future, Promise}


import HttpHeaders._
/**
  * @author msvens
  * @since 20/09/16
  */


sealed trait JsonClientResponse {
  def status: Int
}

case class StringResponse(status: Int, body: String) extends JsonClientResponse

case class EmptyResponse(status: Int) extends JsonClientResponse

case class JsonResponse[T](status: Int, body: T) extends JsonClientResponse

class JsonClientException(val status: Int,val msg: String, val cause: Throwable) extends Exception(msg, cause)



/**
  * HttpClient that simplifies json requests and responses by automatically serialize/deserialize
  * native objects to and from json. JsonClient is fully asynchronous.
  *
  * {{{
  * import org.mellowtech.jsonclient.{JsonClient,JsonResponse}
  * import scala.concurrent.Await
  * import scala.concurrent.duration._
  *
  * case class ServerResponse(key: String, value: String)
  *
  * object Test {
  *   import scala.concurrent.ExecutionContext.Implicits.global
  *   implicit val formats = org.json4s.DefaultFormats
  *   val jc = JsonClient()
  *   val resp = jc.get[ServerResponse]("http://pathToServiceApi")
  *   var res = Await.result(resp, 4 seconds)
  *
  *   res.body match {
  *     case Some(sr) => println(sr)
  *     case None => println(res.statusCode
  *   }
  *   jc.close
  * }
  * }}}
  */
class JsonClient()(implicit val ec: ExecutionContext, as: ActorSystem, mat: ActorMaterializer) {

  import de.heikoseeberger.akkahttpjsoniterscala.JsoniterScalaSupport._
  import com.github.plokhotnyuk.jsoniter_scala.core._


  def postRequest(uri: String): HttpRequest = HttpRequest(HttpMethods.POST, uri)
  def getRequest(uri: String): HttpRequest = HttpRequest(HttpMethods.GET, uri)
  def putRequest(uri: String): HttpRequest = HttpRequest(HttpMethods.PUT, uri)
  def deleteRequest(uri: String): HttpRequest = HttpRequest(HttpMethods.DELETE, uri)

  //short cuts
  def get[A](uri: String)(implicit codec: JsonValueCodec[A]): Future[JsonResponse[A]] = {
    send[A](getRequest(uri))
  }

  def post[A,B](uri: String, requestBody: B)
               (implicit requestBodyCodec: JsonValueCodec[B],
                responseBodyCodec: JsonValueCodec[A]): Future[JsonResponse[A]] = {
    sendWithBody[A,B](postRequest(uri), requestBody)
  }


  //Combination Methods
  def send[A](httpRequest: HttpRequest)(implicit codec: JsonValueCodec[A]): Future[JsonResponse[A]] = for {
    httpResponse <- sendRequest(httpRequest)
    jsonResponse <- getResponseBody[A](httpResponse)
  } yield jsonResponse

  def sendNoResponse(httpRequest: HttpRequest): Future[EmptyResponse] = for {
    httpResponse <- sendRequest(httpRequest)
  } yield discardResponseBody(httpResponse)

  def sendWithBody[A,B](request: HttpRequest, requestBody: B)
                           (implicit requestBodyCodec: JsonValueCodec[B],
                            responseBodyCodec: JsonValueCodec[A]): Future[JsonResponse[A]] = for {
    httpRequest <- addBodyToRequest[B](requestBody, request)
    httpResponse <- sendRequest(httpRequest)
    jsonResponse <- getResponseBody[A](httpResponse)
  } yield jsonResponse

  def sendWithBodyNoResponse[A](request: HttpRequest, requestBody: A)(implicit codec: JsonValueCodec[A]): Future[EmptyResponse] = for {
    httpRequest <- addBodyToRequest[A](requestBody, request)
    httpResponse <- sendRequest(httpRequest)
  } yield discardResponseBody(httpResponse)


  def getString(url: String): Future[StringResponse] = getString(HttpRequest(HttpMethods.GET, url))

  def getString(httpRequest: HttpRequest): Future[StringResponse] = for {
    response <- sendRequest(httpRequest)
    body <- Unmarshal(response.entity).to[String]
  } yield StringResponse(response.status.intValue, body)

  def addBodyToRequest[A](body: A, request: HttpRequest)(implicit codec: JsonValueCodec[A]) = for {
    entity <- Marshal(body).to[RequestEntity]
  } yield request.withEntity(entity)

  def sendRequest(request: HttpRequest): Future[HttpResponse] = Http().singleRequest(request) recover {
      case e: Exception => throw new JsonClientException(-1, e.getMessage, e)
  }

  def discardResponseBody(response: HttpResponse): EmptyResponse = {
    response.discardEntityBytes()
    EmptyResponse(response.status.intValue)
  }

  def getResponseBody[A](response: HttpResponse)(implicit codec: JsonValueCodec[A]): Future[JsonResponse[A]] = {
    Unmarshal(response.entity).to[A].map(respBody => JsonResponse(response.status.intValue, respBody)) recover {
      case x: Unmarshaller.UnsupportedContentTypeException => {
        throw new JsonClientException(response.status.intValue, "Unsupported Content Type: "+response.entity.contentType, x)
      }
      case x: JsonReaderException => {
        throw new JsonClientException(response.status.intValue, "Could not read response", x)
      }
      case x: Exception => {
        throw new JsonClientException(response.status.intValue, response.status.defaultMessage, x)
      }
    }
  }

  def close(): Future[Terminated] = {
    as.terminate()
  }

}

object JsonClient {


  def charset(contentType: Option[String]): Charset = contentType match {
    case Some(c) => Charset.forName(c)
    case None => Charset.forName("UTF-8")
  }

  def withDefaultActorSystem()(implicit ec: ExecutionContext): JsonClient = {
    implicit val as = ActorSystem()
    implicit val am = ActorMaterializer()
    apply()
  }


  def apply()(implicit ec: ExecutionContext, as: ActorSystem, mat: ActorMaterializer): JsonClient =
    new JsonClient()

}