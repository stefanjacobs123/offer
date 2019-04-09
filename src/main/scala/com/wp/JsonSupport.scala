package com.wp

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.UUID

import com.wp.OfferActor.{ CancelOffer, OfferCreated }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat }

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit object LocalDateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {
    override def write(obj: LocalDateTime): JsValue = JsString(obj.format(ISO_LOCAL_DATE_TIME))
    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(s) => LocalDateTime.parse(s, ISO_LOCAL_DATE_TIME)
      case _ => throw DeserializationException("No string found")
    }
  }

  implicit object UUIDJsonFormat extends RootJsonFormat[UUID] {
    override def write(obj: UUID): JsValue = JsString(obj.toString)
    override def read(json: JsValue): UUID = json match {
      case JsString(s) => UUID.fromString(s)
      case _ => throw DeserializationException("Error info you want here ...")
    }
  }

  implicit val offerJsonFormat = jsonFormat4(Offer)
  implicit val offerCreatedJsonFormat = jsonFormat1(OfferCreated)
  implicit val cancelOfferJsonFormat = jsonFormat1(CancelOffer)

}
