package org.mellowtech.jsonclient.cmd

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mellowtech.jsonclient.{JsonClient, JsonResponse}
import org.rogach.scallop._
import org.rogach.scallop.exceptions.{Help, ScallopException, ScallopResult, Version}

import scala.concurrent.Await

class Conf(arguments: Seq[String]) extends ScallopConf(arguments){
  version("0.5.0 2019 Mellowtech")

  val server = new Subcommand("server") {
    val stop = opt[Boolean]("stop")
    val port = opt[Int](default=Some(10))
    val endpoint = trailArg[String]("endpoint")
  }
  val get = new Subcommand("get") {
    val raw = toggle("raw")
    val kv = props[String]('P')
    val url = trailArg[String](required = true)
  }
  val post = new Subcommand("post") {
    val key = opt[String]("key")
    val value = opt[String]("value")
    val url = trailArg[String](required = true)
  }

  val exit = new Subcommand("exit"){}

  addSubcommand(server)
  addSubcommand(get)
  addSubcommand(post)
  addSubcommand(exit)

  override protected def onError(e: Throwable): Unit = e match {
    case r: ScallopResult if !throwError.value => r match {
      case Help("") =>
        builder.printHelp
        //sys.exit(0)
      case Help(subname) =>
        builder.findSubbuilder(subname).get.printHelp
        verified = false
        //sys.exit(0)
      case Version =>
        builder.vers.foreach(println)
        //sys.exit(0)
      case ScallopException(message) => errorMessageHandler(message)
    }
    case e => {
      Console.println("throwing error")
      throw e
    }
  }

}

class Tool(jc: JsonClient){

  import scala.concurrent.duration._
  import JsonCodecs._

  def exec(conf: Conf): Unit = {
    conf.subcommand match {
      case None => conf.printHelp()
      case Some(cmd) => cmd match {
        case conf.exit => System.exit(0)
        case conf.get => {
          conf.get.raw() match {
            case true => {
              val s = Await.result(jc.getString(conf.get.url()), 10.seconds)
              Console.println(s)
            }
            case false => { //Json Call
              Console.println("to be implemented")
            }
          }
        }
        case conf.post => {
          val k = conf.post.key()
          val v = conf.post.value()
          val jr = JsonKeyValue(k,v)
          val r: JsonResponse[JsonKeyValue] = Await.result(jc.post(null,jr), 10.seconds)
        }
      }
      case _ => conf.printHelp()
    }
  }
}

object Main {

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global

  val jsonClient = JsonClient()
  val tool = new Tool(jsonClient)


  val exitCmd = (c: ScallopConfBase) => {
    System.exit(0)
  }

  //tool.registerHandler(c.exit, exitCmd)


  def main(args: Array[String]): Unit = {

    while(true){
      Console.print("jsh> ")
      val cmd = io.StdIn.readLine().split("\\s+")
      val c = new Conf(cmd)
      c.verify()
      if(!c.verified) {
        c.printHelp()
      }
      else {
        tool.exec(c)
      }
    }
    throw new Exception("should not happen")

  }
}
