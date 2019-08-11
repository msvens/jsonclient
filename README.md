# Jsonclient #

[![Build Status](https://travis-ci.org/msvens/jsonclient.svg?branch=master)](https://travis-ci.org/msvens/jsonclient)
[![Maven Central](https://img.shields.io/maven-central/v/org.mellowtech/jsonclient_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/org.mellowtech/jsonclient_2.12)

## Overview

Dead simple Scala wrapper for [Akka Http Client API](https://doc.akka.io/docs/akka-http/current/client-side/index.html) that simplifies interacting with
JSON Based Http APIs. It is optimized for json usage
and will automatically convert responses and request to and from json. It uses
[jsoniter](https://github.com/plokhotnyuk/jsoniter-scala) for this. With minimal bolierplate you should be up and running in minutes

**Observe:** Previous versions < 0.5.0) used AsyncHttpClient or Java 11 HttpClient and Json4s. Version 0.5.0+ is based on AkkaHttp and Jsoniter

### 2 Minute Usage Guide

```scala

import org.mellowtech.jsonclient.{JsonClient,JsonResponse}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import scala.concurrent.Await
import scala.concurrent.duration._

case class ServerResponse(key: String, value: String)

object Test {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  implicit val jsonCodec = JsonCodecMaker.make[ServerResponse](CodecMakerConfig())
  
   val jc = JsonClient()
   
   val resp = jc.get[ServerResponse]("http://pathToServiceApi")
   var res: JsonResponse[ServerResponse] = Await.result(resp, 4 seconds)
   println(res.body)
   
   jc.close
  
}
```


## Todo

* ~~Better support for non json client requests~~
* ~~Proper testing~~
* Simpler configuration (scala.config)
* Chunked responses

## Version History

* 0.5.1 - Added example CLI and more documentation
* 0.5.0 - Using Akka Http Client as backend
* 0.4.0 - First version using Javas new HttpClient Api
* 0.3.0 - First production release. Scala 2.12 and testing
* 0.1.0-SNAPSHOT - initial snapshot release

## Using JsonClient

## Using JsonClientRequest

## Using The Example CLI




## Two Minute Intro to Jsoniter

JsonClient uses Jsoniter as its Json Parser backend. It has been chosen for its

* ease of use
* speed
* out of the box handling of java.time










