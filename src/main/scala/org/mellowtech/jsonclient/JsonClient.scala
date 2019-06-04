package org.mellowtech.jsonclient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.Future
//import java.net.URI

import akka.http.scaladsl.unmarshalling.Unmarshal
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



case class JsonResponse[T](status: Int, body: Option[T])

class JsonClientException(msg: String, cause: Throwable) extends Exception(msg, cause)

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
  /*
  val httpClient: HttpClient =  HttpClient.newBuilder().version(Version.HTTP_1_1)
    .followRedirects(Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(20)).build()
  */

  //val asyncClient = new DefaultAsyncHttpClient(config)

  def delete[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonRequest(None, HttpMethods.DELETE, url)
    //jsonRequest(None,asyncClient.prepareDelete(url))

  def get[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonRequest(None, HttpMethods.GET, url)

  def post[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody),HttpMethods.POST, url)

  def put[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody), HttpMethods.PUT, url)

  def getString(url: String): Future[(Int, String)] = {
    val r: Future[(Int, String)] = for {
      res <- httpRequest(HttpMethods.GET, url)
      body: String <- Unmarshal(res.entity).to[String]

    } yield (res.status.intValue(), body)
    r
  }

  def jsonRequest[T: Manifest, P <: AnyRef](pBody: Option[P], method: HttpMethod, uri: String): Future[JsonResponse[T]] = {
    import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
    implicit val serialization = native.Serialization

    var req = HttpRequest(method = method, uri = uri)
    if(pBody.isDefined){
      req = req.withEntity(HttpEntity(contentType = ContentTypes.`application/json`,
        write[P](pBody.get).getBytes("UTF-8")))
    }
    /*
    val b = HttpRequest.newBuilder(new URI(uri))
    val bp = if(pBody.isDefined){
      b.setHeader("Content-Type", "application/json")
      HttpRequest.BodyPublishers.ofByteArray(write[P](pBody.get).getBytes("UTF-8"))
    } else {
      HttpRequest.BodyPublishers.noBody()
    }
    b.method(method, bp)
    */
    for {
      r <- Http().singleRequest(req) recover {
        case x: Exception => throw new JsonClientException(x.getMessage, x)
      }
      t <- Unmarshal(r.entity).to[T]

    } yield JsonResponse(r.status.intValue(), Some(t))
    /*
    for {
      f <- FutureConverters.toScala(httpClient.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString())) recover {
        case x: Exception => throw new JsonClientException(x.getMessage, x)
      }
      r <- toJsonResponse[T](f)
    } yield r
     */
  }

  def httpRequest(reqMethod: HttpMethod, url: String): Future[HttpResponse] = {
    val req = HttpRequest(method = reqMethod, uri = Uri(url))
    Http().singleRequest(req)

    /*val b = HttpRequest.newBuilder(new URI(url))
    b.method(method, HttpRequest.BodyPublishers.noBody())
    FutureConverters.toScala(httpClient.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString()))
     */
  }

  def close(): Unit = {
    ///httpClient.
  }



  /*
  def toJsonResponse[T: Manifest](r: HttpResponse[String]): Future[JsonResponse[T]] = {

    val p: Promise[JsonResponse[T]] = Promise()

    val length = OptionConverters.toScala(r.headers().firstValue(CONTENT_LENGTH)) match {
      case Some(c) => c.toInt
      case None => 0
    }
    val status = r.statusCode()
    val contentType = OptionConverters.toScala(r.headers().firstValue(CONTENT_TYPE))
    if(contentType.isEmpty || !contentType.get.equalsIgnoreCase("application/json") ){
      p.failure(new JsonClientException(s"wrong content type: $contentType", null))
    }
    else if (length <= 2 || !JsonClient.canHaveJsonBody(status))
      p.success(JsonResponse(status, None, r))
    else Try(read[T](r.body())) match {
      case Success(t) => p.success(JsonResponse(status, Some(t),r))
      case Failure(f) => p.failure(new JsonClientException("could not parse json", f))

    }
    p.future
  }
   */
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