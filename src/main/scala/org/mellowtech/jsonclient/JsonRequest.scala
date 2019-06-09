package org.mellowtech.jsonclient

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.concurrent.{ExecutionContext, Future}

sealed trait JsonClientRequest {
  def jsonClient: JsonClient
  def httpRequest: HttpRequest

  def send(): Future[JsonClientResponse]
  def sendToSring(): Future[StringResponse] = jsonClient.getString(httpRequest)

}

class JsonRequest[A](val httpRequest: HttpRequest, val jsonClient: JsonClient)(implicit ec: ExecutionContext, codec: JsonValueCodec[A])
  extends JsonClientRequest {

  def send(): Future[JsonResponse[A]] = jsonClient.send(httpRequest)

  def sendWithBody[B](body: B)(implicit bodyCodec: JsonValueCodec[B]): Future[JsonResponse[A]] = {
    jsonClient.sendWithBody[A,B](httpRequest, body)
  }

}

class JsonEmptyResponseRequest(val httpRequest: HttpRequest, val jsonClient: JsonClient)(implicit ec: ExecutionContext)
  extends JsonClientRequest {

  def send(): Future[EmptyResponse] = jsonClient.sendNoResponse(httpRequest)

  def sendWithBody[A](body: A)(implicit bodyCodec: JsonValueCodec[A]): Future[EmptyResponse] = {
    jsonClient.sendWithBodyNoResponse[A](httpRequest, body)
  }

}

object JsonRequest {

  def get[A](uri: String)(implicit jsonClient: JsonClient, ec: ExecutionContext, codec: JsonValueCodec[A]) =
    apply(HttpRequest(HttpMethods.GET, uri))

  def post[A](uri: String)(implicit jsonClient: JsonClient, ec: ExecutionContext, codec: JsonValueCodec[A]) =
    apply(HttpRequest(HttpMethods.POST, uri))

  def apply[A](request: HttpRequest)(implicit jsonClient: JsonClient, ec: ExecutionContext, codec: JsonValueCodec[A]) =
    new JsonRequest[A](request, jsonClient)
}

