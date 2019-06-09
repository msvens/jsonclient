import sbt._
import Keys._

object Dependencies {

  val json4sVersion = "3.6.4"
  val akkaHttpVersion = "10.1.8"
  val akkaVersion = "2.5.23"

  //for testing
  val junit = "junit" % "junit" % "4.12" % "test"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.4.2" % "test"
  val testDeps = Seq(junit,scalatest, pegdown) //no ikoseeberger

  val json4s = "org.json4s" %% "json4s-native" % json4sVersion
  val json4sext = "org.json4s" %% "json4s-ext" % json4sVersion
  val akkaJsonIter = "de.heikoseeberger" %% "akka-http-jsoniter-scala" % "1.26.0"

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion


  val java8compat = "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.9.0"

  //val jsonDeps = Seq(json4s,json4sext)
  val akkaDeps = Seq(akkaHttp, akkaStream, akkaJsonIter)


}
