import Dependencies._
import sbt.Keys._
import sbt._

ThisBuild / version := "0.5.1"
ThisBuild / organization := "org.mellowtech"
ThisBuild / scalaVersion := "2.13.0"
ThisBuild / Test / publishArtifact := false
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")

//Single Project Config
lazy val root = (project in file(".")).
  settings(
    name := "jsonclient",
    Test / parallelExecution := false,
    libraryDependencies ++= testDeps,
    libraryDependencies += scallop,
    libraryDependencies ++= akkaDeps
  )