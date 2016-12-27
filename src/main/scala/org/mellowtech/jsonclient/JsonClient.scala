package org.mellowtech.jsonclient

import java.nio.charset.Charset

import io.netty.handler.codec.http.HttpResponseStatus
import org.asynchttpclient.util.HttpUtils
import org.asynchttpclient.{AsyncCompletionHandler, DefaultAsyncHttpClient, Response}

import scala.concurrent.{ExecutionContext, Future, Promise}
import org.json4s._
import org.json4s.native.Serialization.{read, write}

import scala.util.{Failure, Success, Try}

/**
  * @author msvens
  * @since 20/09/16
  */

case class JCResponse[T](statusCode: Int, body: Option[T])

class JsonClientException(msg: String, cause: Throwable) extends Exception(msg, cause)

class JsonClient(implicit ec: ExecutionContext, formats: Formats) {

  val asyncClient = new DefaultAsyncHttpClient()


  def post[T: Manifest, P <: AnyRef](url: String, pBody: P): Future[JCResponse[T]] = {

    val promise = Promise[JCResponse[T]]
    val body: String = write[P](pBody)

    asyncClient.preparePost(url).
      setHeader("Content-Type", "application/json").
      setBody(body.getBytes("UTF-8")).execute(new DefaultCompletionHandler[T](promise))

    promise.future
  }

  def get[T: Manifest](url: String): Future[JCResponse[T]] = {
    val p = Promise[JCResponse[T]]
    asyncClient.prepareGet(url).execute(new DefaultCompletionHandler[T](p))
    p.future
  }

  def getRaw(url: String): Future[Response] = {
    val p = Promise[Response]
    asyncClient.prepareGet(url).execute(new AsyncCompletionHandler[Unit] {
      override def onCompleted(response: Response): Unit = p.success(response)
      override def onThrowable(t: Throwable): Unit = p.failure(t)
    })
    p future
  }

  def close: Unit = {
    asyncClient.close()
  }
}

class DefaultCompletionHandler[T: Manifest](p: Promise[JCResponse[T]])(implicit formats: Formats) extends AsyncCompletionHandler[Response] {

  import io.netty.handler.codec.http.HttpHeaderNames._

  override def onCompleted(response: Response): Response = {

    val length = Option(response.getHeaders.get(CONTENT_LENGTH)) match {
      case Some(c) => c.toInt
      case None => 0
    }
    val contentType = response.getContentType
    val charset = Option(HttpUtils.parseCharset(contentType)) match {
      case Some(c) => c
      case None => Charset.forName("UTF-8")
    }
    val body = response.getResponseBody(charset)

    if(length > 2 && response.getStatusCode == HttpResponseStatus.OK.code()) {
      Try(read[T](body)) match {
        case Success(t) => p.success(JCResponse(response.getStatusCode, Some(t)))
        case Failure(f) => {
          p failure new JsonClientException("failed to read", f)
        }
      }
    } else {
      p.success(JCResponse(response.getStatusCode, None))
    }
    response
  }
  override def onThrowable(t: Throwable): Unit = {
    p failure t
  }


}

object JsonClient {

  def apply()(implicit ec: ExecutionContext, formats: Formats): JsonClient = {
    new JsonClient
  }
}