package org.mellowtech.jsonclient


import io.netty.handler.codec.http.HttpResponseStatus
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
    jsonClient.close
    super.afterAll()
  }

  behavior of "jsonclient"

  it should "return Ok when getting a json object" in {
    getJson.map(jc => assert(jc._1 == HttpResponseStatus.OK.code()))
  }

  it should "return a TestJson object when getting a json object" in {
    getJson.map(jc => {
      val tj = jc._2
      assert(tj.isDefined)
    })
  }

  it should "return a TestJson with double values when posting an object" in {
    val testObj = TestJson("a", 1)
   postJson(testObj).map(jc => {
     val tj1 = jc._2.get
     assert(tj1.m == "aa")
   })
  }

  it should "return an empty body (Optional) when getting an empty json" in {
    getEmpty.map(jc => {
      val tj = jc._2
      assert(tj.isEmpty)
    })
  }

  it should "return the correct body as a string when getting a url as text" in {
    jsonClient.getString(htmlUrl).map(ss => {
      assert(ss._2 == "<h1>Say hello to akka-http</h1>")
    })
  }

  it should "return the correct response when getting a url" in {
    jsonClient.getResponse(htmlUrl).map(r => {
      assert(r.getResponseBody == "<h1>Say hello to akka-http</h1>")
    })
  }

  it should "set status code to 500 when server internally fails" in {
    getJsonError.map(jc => {
      assert(jc._1 == HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
    })
  }

  it should "fail when trying to parse the wrong json object" in {
    recoverToSucceededIf[JsonClientException](getWrongJson)
  }

  it should "fail when trying to parse a non json object" in {
    recoverToSucceededIf[JsonClientException](getHtml)
  }

  it should "fail when trying to access an errornous url" in {
    recoverToSucceededIf[JsonClientException](jsonClient.get[TestJson]("http://some/url"))
  }

  def getWrongJson: Future[JCR[TestJson]] = {
    jsonClient.get[TestJson](jsonWrongUrl)
  }

  def getJson: Future[JCR[TestJson]] = {
    jsonClient.get[TestJson](jsonUrl)
  }

  def postJson(p: TestJson): Future[JCR[TestJson]] = {
    jsonClient.post[TestJson,TestJson](jsonUrl,p)
  }

  def getJsonError: Future[JCR[TestJson]] = {
    jsonClient.get[TestJson](jsonErrorUrl)
  }

  def getEmpty: Future[JCR[TestJson]] = {
    jsonClient.get[TestJson](emptyUrl)
  }

  def getHtml: Future[JCR[TestJson]] = {
    jsonClient.get[TestJson](htmlUrl)
  }

}
