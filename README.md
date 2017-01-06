# Jsonclient #

[![Build Status](https://travis-ci.org/msvens/jsonclient.svg?branch=master)](https://travis-ci.org/msvens/jsonclient)
[![Maven Central](https://img.shields.io/maven-central/v/org.mellowtech/jsonclient_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/org.mellowtech/jsonclient_2.12)

##Overview

Dead simple Scala wrapper for [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client). It is optimized for json usage
and will automatically convert responses and request to and from json. It uses
[json4s](https://github.com/json4s/json4s) for this.

###Usage

```scala

import org.mellowtech.jsonclient.{JsonClient,JsonResponse}
import scala.concurrent.Await
import scala.concurrent.duration._

case class ServerResponse(key: String, value: String)

object Test {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val formats = org.json4s.DefaultFormats
  
   val jc = JsonClient()
   val resp = jc.get[ServerResponse]("http://pathToServiceApi")
   var res: JsonResponse[ServerResponse] = Await.result(resp, 4 seconds)
   res.body match {
      case Some(sr) => println(sr)
      case None => println(res.status)
   }
   jc.close
  
}
```


##Todo

* ~~Better support for non json client requests~~
* ~~Proper testing~~
* Simpler configuration (scala.config)
* Chunked responses

##Version History

* 0.3.0 - First production release. Scala 2.12 and testing
* 0.1.0-SNAPSHOT - initial snapshot release





