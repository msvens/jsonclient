package org.mellowtech.jsonclient

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/**
  * @author msvens
  * @since 2016-12-25
  */
class JsonRequestSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  var server: TestServer = null
  //var jsonClient: JsonClient = null
  //implicit va as = ActorSystem()
  //implicit val mat


  import com.github.plokhotnyuk.jsoniter_scala.core._
  import com.github.plokhotnyuk.jsoniter_scala.macros._

  implicit val codec: JsonValueCodec[TestJson] = JsonCodecMaker.make[TestJson](CodecMakerConfig())
  implicit val wrongCodec: JsonValueCodec[WrongJson] = JsonCodecMaker.make[WrongJson](CodecMakerConfig())

  //implicit val as = ActorSystem()
  //implicit val mat = ActorMaterializer()
  implicit val jsonClient = JsonClient()

  val jsonUrl = "http://localhost:9050/json"
  val jsonErrorUrl = "http://localhost:9050/jsonmiss"
  val jsonWrongUrl = "http://localhost:9050/jsonwrong"
  val htmlUrl = "http://localhost:9050/html"
  val emptyUrl = "http://localhost:9050/empty"
  //val notFoundUrl = "http://localhost:"

  override def beforeAll(): Unit = {
    server = new TestServer

  }

  override def afterAll(): Unit = {
    Await.ready(server.shutdown(), 42.seconds)
    Await.ready(jsonClient.close(), 42.seconds)
    //as.terminate()
    //jsonClient.close
    super.afterAll()
  }

  behavior of "jsonrequest"

  it should "return Ok when getting a json object" in {
    JsonRequest.get[TestJson](jsonUrl).send().map(response => assert(response.status == 200))
  }

  it should "allow to send multiple times" in {
    val jsonRequest = JsonRequest.get[TestJson](jsonUrl)
    for {
      resp1 <- jsonRequest.send()
      resp2 <- jsonRequest.send()
    } yield {
      assert(resp1.status == 200)
      assert(resp2.status == 200)
    }
  }

  it should "return a TestJson object when getting a json object" in {
    JsonRequest.get[TestJson](jsonUrl).send().map(response => assert(response.body != null))
  }

  it should "return a TestJson with double values when posting an object" in {
    val testObj = TestJson("a", 1)
    JsonRequest.post[TestJson](jsonUrl).sendWithBody(testObj).map(response => {
      assert(response.body.m == "aa")
    })
  }

  it should "set status code to 500 when server internally fails" in {
    JsonRequest.get[TestJson](jsonErrorUrl).send().map(response => assert(false)).recover{
      case x: JsonClientException => assert(x.status == 500)
    }
  }

  it should "fail when trying to parse the wrong json object" in {
    recoverToSucceededIf[JsonClientException]{
      JsonRequest.get[TestJson](jsonWrongUrl).send()
    }
  }

  it should "fail when trying to parse a non json object" in {
    recoverToSucceededIf[JsonClientException]{
      JsonRequest.get[TestJson](htmlUrl).send()
    }
  }

  it should "fail when trying to access an errornous url" in {
    recoverToSucceededIf[JsonClientException]{
      JsonRequest.get[TestJson]("http://some/url").send()
    }
  }

}
