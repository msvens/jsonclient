import Dependencies._
import sbt.Keys._
import sbt._

lazy val buildSettings = Seq(
  version := "0.5.0",
  organization := "org.mellowtech",
  scalaVersion := "2.12.8",
  publishArtifact in Test := false,
  parallelExecution in Test := false,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")
)


//Single Project Config
lazy val root = (project in file(".")).
  settings(buildSettings: _*).
  settings(
    name := "jsonclient",
    libraryDependencies ++= testDeps,
    //libraryDependencies ++= jsonDeps,
    libraryDependencies ++= akkaDeps
  )