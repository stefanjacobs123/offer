package com.wp

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTest, RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import com.wp.OfferActor.{CancelOffer, CreateOffer, OfferCreated}
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class OfferRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with RouteTest
    with OfferRoutes with JsonSupport {

  override val ec: ExecutionContext = system.dispatcher
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.seconds)

  lazy val routes: Route = offerRoutes

  "OfferRoutes" should {

    "be able to create offers (POST /offers)" in {

      val offer = Offer("Shopper friendly descirption.", 42, "GBP", LocalDateTime.MAX)
      val offerEntity = Marshal(offer).to[MessageEntity].futureValue //futureValue is from ScalaFutures

      val request = Post("/offers").withEntity(offerEntity)

      //Test
      request ~> routes ~> check {
        status should === (StatusCodes.Created)

        contentType should === (ContentTypes.`application/json`)

        val id = entityAs[OfferCreated].id
        assert(id.isInstanceOf[UUID])

      }
    }

    "complete with OK and Offer when querying an offer that exists (GET /offers/{id})" in {

      //Manually create offer
      val offerId = UUID.randomUUID()
      val offerActor = system.actorOf(Props[OfferActor], offerId.toString)
      val offer = Offer("Shopper friendly descirption", 43, "GBP", LocalDateTime.MAX)
      Await.result(offerActor.ask(CreateOffer(offerId, offer)), 5.seconds)

      //Query offer
      val request = HttpRequest(uri = s"/offers/$offerId")

      //Test
      request ~> routes ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`application/json`)

        entityAs[Offer] should === (offer)
      }

    }

    "complete with NotFound when Offer does not exist (GET /offers/{id})" in {

      //Create random Offer id
      val uuidString: String = UUID.randomUUID().toString

      //Query Offer we know does not exist
      val request = HttpRequest(uri = s"/offers/$uuidString")

      //Test
      request ~> routes ~> check {
        status should ===(StatusCodes.NotFound)

        contentType should ===(ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should ===(s"Offer not found")
      }
    }

    "complete with Gone when querying an offer that exists but has expired (GET /offers/{id})" in {

      //Manually create offer that is expired - LocalDateTime.MIN
      val offerId = UUID.randomUUID()
      val offerActor = system.actorOf(Props[OfferActor], offerId.toString)
      val offer = Offer("Shopper friendly descirption", 43, "GBP", LocalDateTime.MIN)
      Await.result(offerActor.ask(CreateOffer(offerId, offer)), 5.seconds)

      //Query expired offer
      val request = HttpRequest(uri = s"/offers/$offerId")

      //Test
      request ~> routes ~> check {
        status should === (StatusCodes.Gone)

        contentType should === (ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should === ("Offer expired")
      }

    }

    "cancel offer (POST /offers)" in {

      //Manually create offer that is not expired
      val offerId = UUID.randomUUID()
      val offerActor = system.actorOf(Props[OfferActor], offerId.toString)
      val offer = Offer("Shopper friendly descirption", 43, "GBP", LocalDateTime.MAX)
      Await.result(offerActor.ask(CreateOffer(offerId, offer)), 5.seconds)

      //Cancel the offer
      val cancelOffer: CancelOffer = CancelOffer(offerId)
      val cancelOfferEntity = Marshal(cancelOffer).to[MessageEntity].futureValue
      val cancelRequest = Post("/offers").withEntity(cancelOfferEntity)

      //Test
      cancelRequest ~> routes ~> check {
        status should === (StatusCodes.OK)

        contentType should === (ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should === ("Offer cancelled")
      }

      //Verify cancellation
      val verifyCancellationRequest = HttpRequest(uri = s"/offers/$offerId")

      verifyCancellationRequest ~> routes ~> check {
        status should === (StatusCodes.Gone)

        contentType should === (ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should === ("Offer expired")
      }
    }

    "cancel offer that does not exist (POST /offers)" in {

      //Manually create offer that is not expired
      val offerId = UUID.randomUUID()

      //Cancel the offer
      val cancelOffer: CancelOffer = CancelOffer(offerId)
      val cancelOfferEntity = Marshal(cancelOffer).to[MessageEntity].futureValue
      val cancelRequest = Post("/offers").withEntity(cancelOfferEntity)

      //Test
      cancelRequest ~> routes ~> check {
        status should === (StatusCodes.NotFound)

        contentType should === (ContentTypes.`text/plain(UTF-8)`)

        entityAs[String] should === ("Offer not found")
      }

    }

  }
}
