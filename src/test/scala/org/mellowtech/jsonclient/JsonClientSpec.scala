package org.mellowtech.jsonclient

import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
/**
  * @author msvens
  * @since 2016-12-25
  */
class JsonClientSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterAll {

  var server: TestServer = null
  var jsonClient: JsonClient = null
  implicit val formats = org.json4s.DefaultFormats

  val jsonUrl = "http://localhost:9050/json"
  val htmlUrl = "http://localhost:9050/html"

  override def beforeAll(): Unit = {
    server = new TestServer
    jsonClient = JsonClient()
  }

  override def afterAll(): Unit = {
    Await.ready(server.shutdown(), 42.seconds)
    jsonClient.close
    super.afterAll()
  }

  behavior of "jsonclient"

  it should "return 200 when getting a json object" in {
    getJson.map(jc => assert(jc.statusCode == 200))
  }

  it should "return a TestJson object when getting a json object" in {
    getJson.map(jc => {
      val tj = jc.body
      assert(tj.isDefined)
    })
  }

  def getJson: Future[JCResponse[TestJson]] = {
    jsonClient.get[TestJson](jsonUrl)
  }

}
