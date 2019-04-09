package com.wp

import java.util.UUID

import akka.Done
import akka.actor.{ ActorNotFound, ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete

import scala.concurrent.{ ExecutionContext, Future }
import com.wp.OfferActor._
import akka.pattern.{ AskTimeoutException, ask }
import akka.stream.ActorMaterializer
import akka.util.Timeout

trait OfferRoutes extends JsonSupport {

  implicit val system: ActorSystem
  implicit val ec: ExecutionContext
  implicit val materializer: ActorMaterializer

  lazy val log = Logging(system, classOf[OfferRoutes])

  implicit lazy val timeout: Timeout = Timeout(1.seconds)

  /**
   * Return OfferActor's ActorRef given an UUID.
   *
   * Complete with ActorNotFound if actor does not exist, or if identification didn't complete in 2 seconds.
   *
   * @param id used to lookup actor
   * @return Future of ActorRef
   */
  def getOfferActorRef(id: UUID): Future[ActorRef] = {
    system.actorSelection(s"/user/${id.toString}")
      .resolveOne(2.seconds)
  }

  /**
   * Handle Exceptions:
   *  - AskTimeOutException - for cases where Offer Actor does not reply.
   *  - Exception - any other exceptions, signal internal server error to client.
   *  - ActorNotFound - when Actor's ref can't be found.
   *
   * NOTE: ActorNotFound Ex should ideally be handled locally. This exception is very specific to GET offer.
   *
   */

  val exHandler = ExceptionHandler {
    case ate: AskTimeoutException =>
      extractUri { uri =>
        log.error(s"Request to $uri couldn't be handled normally with exception: $ate")
        complete(StatusCodes.RequestTimeout)
      }
    case anf: ActorNotFound =>
      extractUri { uri =>
        log.error(s"Request to $uri couldn't be handled normally with exception: $anf")
        complete((StatusCodes.NotFound, s"Offer not found"))
      }
    case e: Exception =>
      extractUri { uri =>
        log.error(s"Request to $uri couldn't be handled normally with exception: $e")
        complete(StatusCodes.InternalServerError)
      }
  }

  lazy val offerRoutes: Route =
    handleExceptions(exHandler) {
      pathPrefix("offers" / JavaUUID) { id =>
        concat(
          pathEnd {
            concat(
              get {
                val offerFut: Future[Option[Offer]] =
                  getOfferActorRef(id).flatMap(_.ask(GetOffer(id))).mapTo[Option[Offer]]

                onSuccess(offerFut) {
                  case Some(offer) =>
                    log.debug("Offer queried successfully.")
                    complete((StatusCodes.OK, offer))
                  case None =>
                    log.debug("OfferExpired.")
                    complete((StatusCodes.Gone, "Offer expired"))
                }
              })
          })
      } ~
        pathPrefix("offers") {
          post {
            entity(as[Offer]) { offer =>
              val offerId: UUID = UUID.randomUUID()
              val offerActor = system.actorOf(OfferActor.props, offerId.toString)

              val offerCreatedFut: Future[OfferCreated] =
                offerActor.ask(CreateOffer(offerId, offer)).mapTo[OfferCreated]

              onSuccess(offerCreatedFut) { offerCreated =>
                complete((StatusCodes.Created, offerCreated))
              }
            }
          } ~
            post {
              entity(as[CancelOffer]) { cancelOffer =>

                val cancelOfferFut: Future[Done] =
                  getOfferActorRef(cancelOffer.id).flatMap(_.ask(cancelOffer)).mapTo[Done]

                onSuccess(cancelOfferFut) { _ =>
                  complete((StatusCodes.OK, "Offer cancelled"))
                }
              }
            }
        }
    }
}
