import Dependencies._
import sbt.Keys._
import sbt._

lazy val scala212 = "2.12.9"
lazy val scala213 = "2.13.1"
lazy val supportedScalaVersions = List(scala212, scala213)

ThisBuild / version := "0.5.2-SNAPSHOT"
ThisBuild / organization := "org.mellowtech"
ThisBuild / scalaVersion := scala213
ThisBuild / Test / publishArtifact := false
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")

//Single Project Config
lazy val root = (project in file(".")).
  settings(
    crossScalaVersions := supportedScalaVersions,
    name := "jsonclient",
    Test / parallelExecution := false,
    libraryDependencies ++= testDeps,
    libraryDependencies += scallop,
    libraryDependencies ++= akkaDeps
  )