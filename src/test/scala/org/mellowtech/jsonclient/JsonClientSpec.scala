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
  val emptyUrl = "http://localhost:9050/empty"

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

  it should "return an empty body (Optional) when getting an empty json" in {
    getEmpty.map(jc => {
      val tj = jc.body
      assert(tj.isEmpty)
    })
  }

  it should "it should fail when trying to parse a non json object" in {
    recoverToSucceededIf[JsonClientException](getHtml)
  }

  def getJson: Future[JCResponse[TestJson]] = {
    jsonClient.get[TestJson](jsonUrl)
  }

  def getEmpty: Future[JCResponse[TestJson]] = {
    jsonClient.get[TestJson](emptyUrl)
  }

  def getHtml: Future[JCResponse[TestJson]] = {
    jsonClient.get[TestJson](htmlUrl)
  }

}
