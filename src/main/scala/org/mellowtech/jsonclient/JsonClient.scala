package org.mellowtech.jsonclient

import java.nio.charset.Charset

import org.asynchttpclient.{AsyncCompletionHandler, DefaultAsyncHttpClient, Response}

import scala.concurrent.{ExecutionContext, Future, Promise}

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

/**
  * @author msvens
  * @since 20/09/16
  */

case class JCResponse[T](statusCode: Int, body: Option[T])

class JsonClient(implicit ec: ExecutionContext) {

  val asyncClient = new DefaultAsyncHttpClient()

  implicit val formats = Serialization.formats(NoTypeHints)

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

  def close: Unit = {
    asyncClient.close()
  }
}

class DefaultCompletionHandler[T: Manifest](p: Promise[JCResponse[T]]) extends AsyncCompletionHandler[Response] {

  implicit val formats = Serialization.formats(NoTypeHints)

  override def onCompleted(response: Response): Response = {
    //println(response.getContentType)
    //println(response.getStatusCode)
    val json = response.getResponseBody(Charset.forName("UTF-8"))
    //println(json)
    p.success(JCResponse(response.getStatusCode,Some(read[T](json))))
    response
  }
  override def onThrowable(t: Throwable): Unit = {
    p failure t
  }
}

object JsonClient {

  def apply()(implicit ec: ExecutionContext): JsonClient = {
    new JsonClient
  }
}