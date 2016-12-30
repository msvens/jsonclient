import Dependencies._
import sbt.Keys._
import sbt._

lazy val buildSettings = Seq(
  version := "0.3.1",
  organization := "org.mellowtech",
  scalaVersion := "2.12.1",
  publishArtifact in Test := false,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")
)


//Single Project Config
lazy val root = (project in file(".")).
  settings(buildSettings: _*).
  settings(
    name := "jsonclient",
    libraryDependencies ++= testDeps,
    libraryDependencies ++= jsonDeps,
    libraryDependencies += asyncHttpClient,
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  )
