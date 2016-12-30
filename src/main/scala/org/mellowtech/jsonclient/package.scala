package org.mellowtech

/**
  * @author msvens
  * @since 2016-12-28
  */
package object jsonclient {

  object HttpMethod extends Enumeration {
    type HttpMethod = Value
    val Connect, Delete, Get, Head, Options, Patch, Post, Put, Trace = Value
  }

}
