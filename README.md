##Overview

Dead simple Scala wrapper for [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client). It is optimized for json usage
and will automatically convert responses and request to and from json. It uses
[json4s](https://github.com/json4s/json4s) for this.

###Usage

```scala

import org.mellowtech.jsonclient.{JsonClient,JCResponse,JavaTimeSerializers}
import scala.concurrent.Await
import scala.concurrent.duration._

case class ServerResponse(key: String, value: String)

object Test {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all
  
   val jc = JsonClient()
   val resp = jc.get[ServerResponse]("http://localhost:9000/timers")
   var res: JCResponse[ServerResponse] = Await.result(resp, 4 seconds)
   res.body match {
      case Some(sr) => println(sr)
      case None => println(res.statusCode)
   }
   jc.close
  
}
```


##Todo

* Better support for non json client requests
* Proper testing
* Simpler configuration (scala.config)
* Chunked responses

##Version History






