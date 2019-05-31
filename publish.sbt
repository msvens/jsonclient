organizationName := "Mellowtech"
organizationHomepage := Some(url("http://www.mellowtech.org/"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/msvens/jsonclient.git"),
    "scm:git:git@github.com:msvens/jsonclient.git",
    "scm:git:git@github.com:msvens/jsonclient.git"
  )
)

developers := List(
  Developer(
    id    = "msvens",
    name  = "Martin Svensson",
    email = "msvens@gmail.com",
    url   = url("http://www.mellowtech.org/")
  )
)

description := "Simple Json Http Client"
licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/msvens/jsonclient"))

pomIncludeRepository := { _ => false }
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true

// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "org.mellowtech"


// To sync with Maven central, you need to supply the following information:
/*
pomExtra in Global := {
  <url>https://github.com/msvens/jsonclient</url>
    <scm>
      <developerConnection>scm:git:git@github.com:msvens/jsonclient.git</developerConnection>
      <connection>scm:git:git@github.com:msvens/jsonclient.git</connection>
      <url>git@github.com:msvens/jsonclient.git</url>
    </scm>
    <licenses>
      <license>
        <name>The Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <name>Martin Svensson</name>
        <email>msvens@gmail.com</email>
        <organization>Mellowtech</organization>
        <organizationUrl>http://www.mellowtech.org/</organizationUrl>
      </developer>
    </developers>
}
*/