import sbt._
import Keys._

object Dependencies {

  val json4sVersion = "3.6.4"
  val akkaHttpVersion = "10.1.8"
  val akkaVersion = "2.5.23"
  val scallopVersion = "3.3.1"

  //for testing
  val junit = "junit" % "junit" % "4.12" % "test"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.8" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.4.2" % "test"
  val testDeps = Seq(junit,scalatest, pegdown) //no ikoseeberger

  val akkaJsonIter = "de.heikoseeberger" %% "akka-http-jsoniter-scala" % "1.27.0"

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion


  val akkaDeps = Seq(akkaHttp, akkaStream, akkaJsonIter)
  val scallop = "org.rogach" %% "scallop" % scallopVersion

}
