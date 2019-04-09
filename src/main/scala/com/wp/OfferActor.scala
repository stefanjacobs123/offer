package com.wp

import java.time.LocalDateTime
import java.util.UUID

import akka.Done
import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.PipeToSupport

import scala.concurrent.{ ExecutionContext, Future }

final case class Offer(description: String, price: Int, currency: String, expiryDate: LocalDateTime)

object OfferActor {

  final case class ActionPerformed(description: String)

  final case class CreateOffer(id: UUID, offer: Offer)
  final case class OfferCreated(id: UUID)

  final case class GetOffer(id: UUID)

  final case class CancelOffer(id: UUID)

  def props: Props = Props[OfferActor]

}

trait OfferDB {
  def persistOffer(id: UUID, offer: Offer): Future[Done]
  def readOffer(id: UUID): Future[Offer]
}

class OfferActor extends Actor with ActorLogging with PipeToSupport with OfferDB {
  import OfferActor._

  implicit val ec: ExecutionContext = context.dispatcher

  /**
   * Ideally we would use akka-persistence.
   * For one, it would allow taking snapshots.
   * It will also implement writing to and reading from db.
   */

  var dbSpoof: Offer = _

  //Spoof
  override def persistOffer(id: UUID, offer: Offer): Future[Done] = {
    dbSpoof = offer
    Future.successful(Done)
  }

  //Spoof
  override def readOffer(id: UUID): Future[Offer] = Future.successful(dbSpoof)

  def receive: Receive = {
    case CreateOffer(id, offer) =>
      persistOffer(id, offer).map { _ => OfferCreated(id) }.pipeTo(sender)

    case GetOffer(id) =>
      readOffer(id).map { offer =>
        if (offer.expiryDate.isAfter(LocalDateTime.now)) Some(offer)
        else None
      }.pipeTo(sender)

    case CancelOffer(id) =>
      readOffer(id).flatMap { offer =>
        persistOffer(id, offer.copy(expiryDate = LocalDateTime.MIN))
      }.pipeTo(sender)

  }
}
