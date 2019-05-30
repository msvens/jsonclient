import sbt._
import Keys._

object Dependencies {

  val json4sVersion = "3.6.6"

  //for testing
  val junit = "junit" % "junit" % "4.12" % "test"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  val akkajson = "de.heikoseeberger" %% "akka-http-json4s" % "1.22.0" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.4.2" % "test"
  val testDeps = Seq(junit,scalatest, akkajson, pegdown)

  val json4s = "org.json4s" %% "json4s-native" % json4sVersion
  val json4sext = "org.json4s" %% "json4s-ext" % json4sVersion

  val java8compat = "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.9.0"

  val jsonDeps = Seq(json4s,json4sext)

  val asyncHttpClient = "org.asynchttpclient" % "async-http-client" % "2.1.0-alpha1"


}
