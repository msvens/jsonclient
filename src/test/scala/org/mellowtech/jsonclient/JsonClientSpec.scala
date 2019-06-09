package org.mellowtech.jsonclient


//import io.netty.handler.codec.http.HttpResponseStatus
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
/**
  * @author msvens
  * @since 2016-12-25
  */
class JsonClientSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  var server: TestServer = null
  var jsonClient: JsonClient = null

  import com.github.plokhotnyuk.jsoniter_scala.core._
  import com.github.plokhotnyuk.jsoniter_scala.macros._

  implicit val codec: JsonValueCodec[TestJson] = JsonCodecMaker.make[TestJson](CodecMakerConfig())
  implicit val wrongCodec: JsonValueCodec[WrongJson] = JsonCodecMaker.make[WrongJson](CodecMakerConfig())

  val jsonUrl = "http://localhost:9050/json"
  val jsonErrorUrl = "http://localhost:9050/jsonmiss"
  val jsonWrongUrl = "http://localhost:9050/jsonwrong"
  val htmlUrl = "http://localhost:9050/html"
  val emptyUrl = "http://localhost:9050/empty"
  //val notFoundUrl = "http://localhost:"

  override def beforeAll(): Unit = {
    server = new TestServer
    jsonClient = JsonClient()
  }

  override def afterAll(): Unit = {
    Await.ready(server.shutdown(), 42.seconds)
    Await.ready(jsonClient.close, 42.seconds)
    super.afterAll()
  }

  behavior of "jsonclient"

  it should "return Ok when getting a json object" in {
    jsonClient.get[TestJson](jsonUrl).map(response => assert(response.status == 200))
  }

  it should "return a TestJson object when getting a json object" in {
    jsonClient.get[TestJson](jsonUrl).map(response => assert(response.body != null))
  }

  it should "return a TestJson with double values when posting an object" in {
    val testObj = TestJson("a", 1)
    jsonClient.post[TestJson, TestJson](jsonUrl, testObj).map(response => {
     assert(response.body.m == "aa")
    })
  }

  it should "return an empty body (Optional) when getting an empty json" in {
    val httpRequest = jsonClient.getRequest(emptyUrl)
    jsonClient.sendRequest(httpRequest).map(response => {
      val emptyResponse = jsonClient.discardResponseBody(response)
      assert(emptyResponse.status == 200)
    })

  }

  it should "return the correct body as a string when getting a url as text" in {
    jsonClient.getString(htmlUrl).map(ss => {
      assert(ss.body == "<h1>Say hello to akka-http</h1>")
    })
  }

  it should "set status code to 500 when server internally fails" in {
    jsonClient.get[TestJson](jsonErrorUrl).map(jc => {
      assert(false)
    }) recover {
      case x: JsonClientException => {
        assert(x.status == 500)
      }
    }
  }

  it should "fail when trying to parse the wrong json object" in {
    recoverToSucceededIf[JsonClientException]{
      jsonClient.get[TestJson](jsonWrongUrl)
    }
  }

  it should "fail when trying to parse a non json object" in {
    recoverToSucceededIf[JsonClientException]{
      jsonClient.get[TestJson](htmlUrl)
    }
  }

  it should "fail when trying to access an errornous url" in {
    recoverToSucceededIf[JsonClientException]{
      jsonClient.get[TestJson]("http://some/url")
    }
  }

}
