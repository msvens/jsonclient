package org.mellowtech.jsonclient

import java.nio.charset.Charset

import io.netty.handler.codec.http.HttpResponseStatus
import org.asynchttpclient.util.HttpUtils
import org.asynchttpclient.{AsyncCompletionHandler, BoundRequestBuilder, DefaultAsyncHttpClient, Response}

import scala.concurrent.{ExecutionContext, Future, Promise}
import org.json4s._
import org.json4s.native.Serialization.{read, write}

import scala.util.{Failure, Success, Try}

/**
  * @author msvens
  * @since 20/09/16
  */

//case class JCResponse[T](statusCode: Int, body: Option[T])



class JsonClientException(msg: String, cause: Throwable) extends Exception(msg, cause)

class JsonClient(implicit ec: ExecutionContext, formats: Formats) {


  val asyncClient = new DefaultAsyncHttpClient()


  def get[T: Manifest](url: String): Future[JCR[T]] =
    jsonRequest(None,asyncClient.prepareGet(url))

  def post[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JCR[T]] =
    jsonRequest(Some(pBody),asyncClient.preparePost(url))

  def put[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JCR[T]] =
    jsonRequest(Some(pBody), asyncClient.preparePut(url))

  def patch[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JCR[T]] =
    jsonRequest(Some(pBody), asyncClient.preparePatch(url))


  private def jsonRequest[T: Manifest, P <: AnyRef](pBody: Option[P],
                                                    builder: BoundRequestBuilder): Future[JCR[T]] = {
    val promise = Promise[JCR[T]]
    val b = {
      if(pBody.isDefined) builder.setBody(write[P](pBody.get).getBytes("UTF-8")).setHeader("Content-Type", "application/json")
      else
        builder
    }
    b.execute(new JsonCompletionHandler[T](promise))
    promise.future
  }

  def getResponse(url: String): Future[Response] = {
    val p = Promise[Response]
    asyncClient.prepareGet(url).execute(new AsyncCompletionHandler[Unit] {
      override def onCompleted(response: Response): Unit = p.success(response)
      override def onThrowable(t: Throwable): Unit = p.failure(t)
    })
    p future
  }

  def getString(url: String): Future[(Int,String)] = {
    val p = Promise[(Int,String)]
    asyncClient.prepareGet(url).execute(new AsyncCompletionHandler[Unit] {
      override def onCompleted(response: Response): Unit = {
        val status = response.getStatusCode
        val charset = JsonClient.charset(response.getContentType)
        val body = response.getResponseBody(charset)
        p success((status,body))
      }

      override def onThrowable(t: Throwable): Unit = p failure new JsonClientException("client failed", t)
    })
    p future
  }


  def close: Unit = {
    asyncClient.close()
  }




}

class JsonCompletionHandler[T: Manifest](p: Promise[JCR[T]])(implicit formats: Formats) extends AsyncCompletionHandler[Response] {

  import io.netty.handler.codec.http.HttpHeaderNames._

  override def onCompleted(response: Response): Response = {

    val length = Option(response.getHeaders.get(CONTENT_LENGTH)) match {
      case Some(c) => c.toInt
      case None => 0
    }

    val contentType: String = response.getContentType
    val charset = JsonClient.charset(contentType)
    val status = response.getStatusCode

    if(contentType != "application/json")
      p.failure(new JsonClientException(s"wrong content type: $contentType", null))
    else if(length <= 2 || status != HttpResponseStatus.OK.code())
      p.success((status, None))
    else Try(read[T](response.getResponseBody(charset))) match {
      case Success(t) => {
        p.success((status, Some(t)))
      }
      case Failure(f) => {
        p.failure(new JsonClientException("could not parse json", f))
      }
    }
    response
  }
  override def onThrowable(t: Throwable): Unit = {
    p failure new JsonClientException("client failed", t)
  }


}

object JsonClient {

  //private def charset(resp: Response) = charset(resp.getContentType)

  def charset(contentType: String): Charset = Option(HttpUtils.parseCharset(contentType)) match {
    case Some(c) => c
    case None => Charset.forName("UTF-8")
  }

  def apply()(implicit ec: ExecutionContext, formats: Formats): JsonClient = {
    new JsonClient
  }
}