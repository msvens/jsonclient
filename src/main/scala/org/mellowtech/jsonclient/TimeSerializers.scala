package org.mellowtech.jsonclient

import java.time._

import org.json4s._

/**
  * @author msvens
  * @since 27/09/16
  */
object JavaTimeSerializers {

  def all = List(
    OffsetDateTimeSerializer,
    PeriodSerializer,
    DurationSerializer,
    InstantSerializer
  )

}

case object OffsetDateTimeSerializer extends CustomSerializer[OffsetDateTime](format => (
  {
    case JString(t) => OffsetDateTime.parse(t)
    case JNull => null
  },
  {
    case t: OffsetDateTime => JString(t.toString)
  }
))

case object PeriodSerializer extends CustomSerializer[Period](format => (
  {
    case JString(p) => Period.parse(p)
    case JNull => null
  },
  {
    case p: Period => JString(p.toString)
  }
))

case object DurationSerializer extends CustomSerializer[Duration]( format => (
  {
    case JInt(d) => Duration.ofMillis(d.toLong)
    case JNull => null
  },
  {
    case d: Duration => JInt(d.toMillis)
  }
))

case object InstantSerializer extends CustomSerializer[Instant]( format => (
  {
    case JInt(d) => Instant.ofEpochMilli(d.toLong)
    case JNull => null
  },
  {
    case i: Instant => JInt(i.toEpochMilli)
  }
))