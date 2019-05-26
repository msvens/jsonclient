package org.mellowtech.jsonclient



case class HttpHeader(val name: String, val value: String){
  override def toString: String = name + ": " + value
}

object HttpHeaders {
  val CONTENT_LENGTH = "Content-Length"
  val CONTENT_TYPE = "Content-Type"
}


