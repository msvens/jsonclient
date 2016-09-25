package org.mellowtech.jsonclient

import java.time.OffsetDateTime

import org.json4s.JsonAST.JValue

import scala.concurrent.Await
import scala.concurrent.duration._
import org.json4s._
import org.json4s.native.Serialization


/**
  * @author msvens
  * @since 23/09/16
  */

case class Timer(id: Option[Int],
                 title: String,
                 start: Option[OffsetDateTime],
                 stop: Option[OffsetDateTime],
                 desc: Option[String])


case class ApiResponse(status: String = "SUCCESS", message: Option[String] = None, value: Option[JValue] = None)


object TestJsonClient extends App{

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val formats = org.json4s.DefaultFormats

  val jc = JsonClient()

  val resp = jc.get[ApiResponse]("http://localhost:9000/timers")

  val res = Await.result(resp, 2 seconds)

  println(Serialization.writePretty(res.body.get.value.get))

  jc.close

  //sys.exit(0)

}
