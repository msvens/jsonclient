package org.mellowtech.jsonclient

import java.nio.charset.Charset

import org.asynchttpclient.util.HttpUtils
import org.asynchttpclient._
import io.netty.handler.codec.http.HttpResponseStatus

import scala.concurrent.{ExecutionContext, Future, Promise}
import org.json4s._
import org.json4s.native.Serialization.{read, write}
import org.mellowtech.jsonclient.HttpMethod._

import scala.util.{Failure, Success, Try}

/**
  * @author msvens
  * @since 20/09/16
  */



case class JsonResponse[T](status: Int, body: Option[T], raw: Response)

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
  * @param config http client specific config
  * @param ec execution context for this client
  * @param formats json formats for parsing
  */
class JsonClient(config: AsyncHttpClientConfig)(implicit ec: ExecutionContext, formats: Formats) {



  val asyncClient = new DefaultAsyncHttpClient(config)

  def delete[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonRequest(None,asyncClient.prepareDelete(url))

  def get[T: Manifest](url: String): Future[JsonResponse[T]] =
    jsonRequest(None,asyncClient.prepareGet(url))

  def post[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody),asyncClient.preparePost(url))

  def put[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody), asyncClient.preparePut(url))

  def patch[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JsonResponse[T]] =
    jsonRequest(Some(pBody), asyncClient.preparePatch(url))


  def jsonRequest[T: Manifest, P <: AnyRef](pBody: Option[P],
                                                    builder: BoundRequestBuilder): Future[JsonResponse[T]] = {
    val promise = Promise[JsonResponse[T]]
    val b = {
      if(pBody.isDefined) builder.setBody(write[P](pBody.get).getBytes("UTF-8")).setHeader("Content-Type", "application/json")
      else
        builder
    }
    b.execute(new JsonCompletionHandler[T](promise))
    promise.future
  }

  def httpRequest(builder: BoundRequestBuilder): Future[Response] = {
    val p = Promise[Response]
    builder.execute(new AsyncCompletionHandler[Unit] {
      override def onCompleted(response: Response): Unit = p.success(response)
      override def onThrowable(t: Throwable): Unit = p.failure(new JsonClientException("client failed",t))
    })
    p.future
  }

  def httpRequest(url: String, method: HttpMethod): Future[Response] = httpRequest(requestBuilder(url, method))

  def requestBuilder(url: String, method: HttpMethod): BoundRequestBuilder = method match {
    case Connect => asyncClient.prepareConnect(url)
    case Delete => asyncClient.prepareDelete(url)
    case Get => asyncClient.prepareGet(url)
    case Head => asyncClient.prepareHead(url)
    case Options => asyncClient.prepareOptions(url)
    case Patch => asyncClient.preparePatch(url)
    case Post => asyncClient.preparePost(url)
    case Put => asyncClient.preparePut(url)
    case Trace => asyncClient.prepareTrace(url)
  }

  def getString(url: String): Future[(Int, String)] = {
    httpRequest(url, HttpMethod.Get).map(r => {
      val status = r.getStatusCode
      val charset = JsonClient.charset(r.getContentType)
      val body = r.getResponseBody(charset)
      (status, body)
    })
  }

  def close(): Unit = {
    asyncClient.close()
  }




}

class JsonCompletionHandler[T: Manifest](p: Promise[JsonResponse[T]])(implicit formats: Formats) extends AsyncCompletionHandler[Unit] {

  import io.netty.handler.codec.http.HttpHeaderNames._

  override def onCompleted(response: Response): Unit = {

    val length = Option(response.getHeaders.get(CONTENT_LENGTH)) match {
      case Some(c) => c.toInt
      case None => 0
    }

    val contentType: String = response.getContentType
    val charset = JsonClient.charset(contentType)
    val status = response.getStatusCode

    if (contentType != "application/json")
      p.failure(new JsonClientException(s"wrong content type: $contentType", null))
    else if (length <= 2 || !JsonClient.canHaveJsonBody(status))
      p.success(JsonResponse(status, None, response))
    else Try(read[T](response.getResponseBody(charset))) match {
      case Success(t) => {
        p.success(JsonResponse(status, Some(t),response))
      }
      case Failure(f) => {
        p.failure(new JsonClientException("could not parse json", f))
      }
    }
  }
  override def onThrowable(t: Throwable): Unit = {
    p failure new JsonClientException("client failed", t)
  }


}

object JsonClient {

  //private def charset(resp: Response) = charset(resp.getContentType)
  def canHaveJsonBody(status: Int) = status match {
    case 200 | 201 | 202 | 203 | 206  => true
    case _ => false
  }

  def charset(contentType: String): Charset = Option(HttpUtils.parseCharset(contentType)) match {
    case Some(c) => c
    case None => Charset.forName("UTF-8")
  }

  def apply()(implicit ec: ExecutionContext, formats: Formats): JsonClient =
    new JsonClient(new DefaultAsyncHttpClientConfig.Builder().build)

  def apply(config: AsyncHttpClientConfig)(implicit ec: ExecutionContext, formats: Formats): JsonClient =
    new JsonClient(config)
}