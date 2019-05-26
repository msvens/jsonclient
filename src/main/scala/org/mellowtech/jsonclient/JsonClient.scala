package org.mellowtech.jsonclient

import java.net.URI
import java.net.http.HttpClient.{Redirect, Version}
import java.net.http.HttpRequest.BodyPublisher
import java.nio.charset.Charset
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration

import scala.concurrent.{ExecutionContext, Future, Promise}
import org.json4s._
import org.json4s.native.Serialization.{read, write}
import org.mellowtech.jsonclient.HttpMethod._

import scala.compat.java8.{FutureConverters, OptionConverters}
import scala.util.{Failure, Success, Try}

import HttpHeaders._
/**
  * @author msvens
  * @since 20/09/16
  */



case class JsonResponse[T](status: Int, body: Option[T], raw: HttpResponse[String])

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
  * @param ec execution context for this client
  * @param formats json formats for parsing
  */
class JsonClient()(implicit ec: ExecutionContext, formats: Formats) {


  val httpClient =  HttpClient.newBuilder().version(Version.HTTP_1_1)
    .followRedirects(Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(20)).build()


  //val asyncClient = new DefaultAsyncHttpClient(config)

  def delete[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonRequest(None, Methods.DELETE, url)
    //jsonRequest(None,asyncClient.prepareDelete(url))

  def get[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonRequest(None, Methods.GET, url)

  def post[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody),Methods.POST, url)

  def put[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody), Methods.PUT, url)

  def getString(url: String): Future[(Int, String)] = {
    httpRequest(Methods.GET, url).map(r => {
      val status = r.statusCode()
      val body = r.body()
      (status, body)
    })
  }

  def jsonRequest[T: Manifest, P <: AnyRef](pBody: Option[P], method: String, uri: String): Future[JsonResponse[T]] = {
    val b = HttpRequest.newBuilder(new URI(uri))
    val bp = if(pBody.isDefined){
      b.setHeader("Content-Type", "application/json")
      HttpRequest.BodyPublishers.ofByteArray(write[P](pBody.get).getBytes("UTF-8"))
    } else {
      HttpRequest.BodyPublishers.noBody()
    }
    b.method(method, bp)
    for {
      f <- FutureConverters.toScala(httpClient.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString())) recover {
        case x: Exception => throw new JsonClientException(x.getMessage, x)
      }
      r <- toJsonResponse[T](f)
    } yield r
  }

  def httpRequest(method: String, url: String): Future[HttpResponse[String]] = {
    val b = HttpRequest.newBuilder(new URI(url))
    b.method(method, HttpRequest.BodyPublishers.noBody())
    FutureConverters.toScala(httpClient.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString()))
  }

  def close(): Unit = {
    ///httpClient.
  }



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
      case Success(t) => {
        p.success(JsonResponse(status, Some(t),r))
      }
      case Failure(f) => {
        p.failure(new JsonClientException("could not parse json", f))
      }
    }
    p.future
  }
}

object Methods {
  val POST = "POST"
  val GET = "GET"
  val HEAD = "HEAD"
  val PUT = "PUT"
  val DELETE = "DELETE"
  val CONNECT = "CONNECT"
  val OPTIONS = "OPTIONS"
  val TRACE = "TRACE"

}


object JsonClient {

  //private def charset(resp: Response) = charset(resp.getContentType)
  def canHaveJsonBody(status: Int) = status match {
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