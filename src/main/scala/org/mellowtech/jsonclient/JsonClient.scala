package org.mellowtech.jsonclient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec

import scala.concurrent.Future
//import java.net.URI

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshalling.Marshal
//import java.net.http.HttpClient.{Redirect, Version}
import java.nio.charset.Charset
//import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

import scala.concurrent.{ExecutionContext, Future, Promise}
import org.json4s._
import org.json4s.native.Serialization.{read, write}

//import scala.compat.java8.{FutureConverters, OptionConverters}
import scala.util.{Failure, Success, Try}

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

//case class RawResponse(status: Int, httpResponse: HttpEntity.Strict) extends JsonClientResponse

//case class RawResponse(status:Int, body: HttpResponse) extends JsonClientResponse

//case class JsonResponse[T](status: Int, body: Option[T])

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
  * @param formats json formats for parsing
  */
class JsonClient()(implicit val ec: ExecutionContext,
                   formats: Formats) {

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val serialization = native.Serialization
  /*
  val httpClient: HttpClient =  HttpClient.newBuilder().version(Version.HTTP_1_1)
    .followRedirects(Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(20)).build()
  */

  //val asyncClient = new DefaultAsyncHttpClient(config)

  def delete[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonResponse(None, HttpMethods.DELETE, url)
    //jsonRequest(None,asyncClient.prepareDelete(url))

  def get[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonResponse(None, HttpMethods.GET, url)

  def post[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonResponse(Some(pBody),HttpMethods.POST, url)

  def put[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonResponse(Some(pBody), HttpMethods.PUT, url)

  def getString(url: String): Future[StringResponse] = for {
      res: HttpResponse <- call(HttpRequest(method = HttpMethods.GET, uri = Uri(url)))
      body: String <- Unmarshal(res.entity).to[String]
  } yield StringResponse(res.status.intValue, body)

  def empty(method: HttpMethod, uri: String): Future[EmptyResponse] = empty(None, method, uri)


  def empty[P <: AnyRef](pBody: Option[P], method: HttpMethod, uri: String): Future[EmptyResponse] = {
    var req = HttpRequest(method = method, uri = uri)
    if(pBody.isDefined){
      req = req.withEntity(HttpEntity(contentType = ContentTypes.`application/json`,
        write[P](pBody.get).getBytes("UTF-8")))
    }
    for{
      r <- Http().singleRequest(req) recover {
        case x: Exception => throw new JsonClientException(500, x.getMessage, x)
      }
    } yield {
      r.discardEntityBytes()
      EmptyResponse(r.status.intValue)
    }
  }

  def jsonResponse[T: Manifest](method: HttpMethod, uri: String) = {
    jsonResponse[T, AnyRef](None, method, uri)
  }

  def jsonResponse[T: Manifest, P <: AnyRef](pBody: Option[P], method: HttpMethod, uri: String): Future[JsonResponse[T]] = {

    /*var req = HttpRequest(method = method, uri = uri)
    if(pBody.isDefined){
      req = req.withEntity(HttpEntity(contentType = ContentTypes.`application/json`,
        write[P](pBody.get).getBytes("UTF-8")))
    }*/
    for {
      req <- request(pBody, method, uri)
      r <- call(req)
      t <- read(r)
    } yield JsonResponse(r.status.intValue(), t)
  }

  private def request[P <: AnyRef](pBody: Option[P], method: HttpMethod, uri: String): Future[HttpRequest] = {
    import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

    if(pBody.isDefined) for {
      req <- Marshal(pBody.get).to[RequestEntity]
      hr <- Future.successful(HttpRequest(method = method, uri = uri, entity = req))
    } yield hr
    else
      Future.successful(HttpRequest(method = method, uri = uri))
  }

  private def read[T: Manifest](res: HttpResponse): Future[T] = {
    import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
    //implicit val serialization = native.Serialization
    Unmarshal(res.entity).to[T] recover {
      case x: Unmarshaller.UnsupportedContentTypeException => {
        throw new JsonClientException(res.status.intValue, "Unsupported Content Type: "+res.entity.contentType, x)
      }
      case x: MappingException => {
        throw new JsonClientException(res.status.intValue, "Could not map response", x)
      }
      case x: Exception => {
        throw new JsonClientException(res.status.intValue, res.status.defaultMessage(), x)
      }
    }
  }

  private def call(req: HttpRequest): Future[HttpResponse] = {
    for {
      res <- Http().singleRequest(req) recover {
        case x: Exception => throw new JsonClientException(-1, x.getMessage, x)
      }
      _ <- if(res.status.isSuccess())
        Future.successful(())
      else
        Future.failed(new JsonClientException(res.status.intValue(), res.status.defaultMessage(), null))
    } yield res
  }

  def close(): Unit = {
    as.terminate
  }
}

object JsonClient {

  def canHaveJsonBody(status: Int): Boolean = status match {
    case 200 | 201 | 202 | 203 | 206  => true
    case _ => false
  }

  def charset(contentType: Option[String]): Charset = contentType match {
    case Some(c) => Charset.forName(c)
    case None => Charset.forName("UTF-8")
  }

  def apply()(implicit ec: ExecutionContext, formats: Formats): JsonClient =
    new JsonClient()

  /*
  def apply(config: AsyncHttpClientConfig)(implicit ec: ExecutionContext, formats: Formats): JsonClient =
    new JsonClient(config)
    */
}