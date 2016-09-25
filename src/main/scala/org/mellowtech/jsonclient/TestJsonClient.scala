package org.mellowtech.jsonclient

import java.time.OffsetDateTime

import org.json4s.JsonAST.JValue

import scala.concurrent.Await
import scala.concurrent.duration._
import org.json4s._
import org.json4s.native.Serialization

import scala.util.{Failure, Success}


/**
  * @author msvens
  * @since 23/09/16
  */

case class Timer(id: Option[Int],
                 title: String,
                 start: Option[OffsetDateTime],
                 stop: Option[OffsetDateTime],
                 desc: Option[String])

case class AddTimer(title: String,
                    start: Option[OffsetDateTime] = None,
                    stop: Option[OffsetDateTime] = None,
                    seconds: Option[Int] = None,
                    desc: Option[String] = None)



case class ApiResponse(status: String = "SUCCESS", message: Option[String] = None, value: Option[JValue] = None)


object TestJsonClient extends App{

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val formats = org.json4s.DefaultFormats

  val jc = JsonClient()

  var resp = jc.get[ApiResponse]("http://localhost:9000/timer")

  var res = Await.result(resp, 2 seconds)

  res.body match {
      case Success(a) => println(Serialization.writePretty(a.value.get))
      case Failure(e) => {
        println("printing stacktrace "+res.statusCode)
        e.printStackTrace()
      }
  }


  /*val add = AddTimer("a new timer")

  resp = jc.post[ApiResponse,AddTimer]("http://localhost:9000/timers", add)

  res = Await.result(resp, 2 seconds)

  println(res)*/


  jc.close

  //sys.exit(0)

}
