/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.controllers.subscriptions

import com.google.inject.AbstractModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import stubs.AgentServicesAccountStubs._
import support.BaseISpec
import support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.CheckYourAnswersController
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionRequest
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CheckYourAnswersControllerISpec
extends BaseISpec
with UnitSpec
with Matchers
with GuiceOneAppPerSuite
with ScalaFutures
with IntegrationPatience
with MockFactory {

  private val legacyRegimes = List(CT, PAYE, SA)

  class TestSetup(legacyRegime: LegacyRegime) {

    private val testArn = "TARN0000001"

    private val stubAuthConnector =
      new AuthConnector {
        private val authJson = Json.obj(
          "allEnrolments" -> Json.arr(
            Json.obj(
              "key" -> "HMRC-AS-AGENT",
              "identifiers" -> Json.arr(
                Json.obj("key" -> "AgentReferenceNumber", "value" -> testArn)
              )
            )
          ),
          "affinityGroup" -> "Agent"
        )

        override def authorise[A](
          predicate: Predicate,
          retrieval: Retrieval[A]
        )(
          implicit
          hc: HeaderCarrier,
          ec: ExecutionContext
        ): Future[A] = Future.successful(retrieval.reads.reads(authJson).get)
      }

    implicit val ec: ExecutionContext = ExecutionContext.global

    val agentServicesAccountConnector: AgentServicesAccountConnector =
      new AgentServicesAccountConnector(
        http = stub[HttpClientV2],
        appConfig = appConfig
      ) {
        override def getAgentRecord(implicit rh: RequestHeader): Future[AgentDetailsDesResponse] = Future.successful(
          AgentDetailsDesResponse(
            uniqueTaxReference = Some(Utr("0123456789")),
            agencyDetails = Some(
              AgencyDetails(
                agencyName = None,
                agencyEmail = None,
                agencyTelephone = Some("1234567890"),
                agencyAddress = None
              )
            ),
            suspensionDetails = Some(SuspensionDetails(suspensionStatus = false, None))
          )
        )

        override def submitLegacySubscriptionRequest(
          subscriptionRequest: SubscriptionRequest,
          legacyRegime: LegacyRegime
        )(implicit hc: HeaderCarrier): Future[Unit] = Future.successful(())
      }

    val overrides: AbstractModule =
      new AbstractModule {
        override def configure(): Unit = {
          bind(classOf[AuthConnector]).toInstance(stubAuthConnector)
          bind(classOf[AgentServicesAccountConnector]).toInstance(agentServicesAccountConnector)
        }
      }

    implicit lazy val app: Application = new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "metrics.enabled" -> false
      )
      .overrides(overrides)
      .build()

    val controller: CheckYourAnswersController = app.injector.instanceOf[CheckYourAnswersController]

    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]

    val session: Map[String, String] = Map("sessionId" -> "test-session")

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(session.toSeq: _*)

    def cacheJourney(journey: SubscriptionJourney): Unit = {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
      implicit val writes: OWrites[SubscriptionJourney] = Json.writes[SubscriptionJourney]

      sessionCache.put(subscriptionJourneyKey(legacyRegime), journey).futureValue
    }

  }

  legacyRegimes.foreach(legacyRegime => {

    s"GET /subscription/$legacyRegime/check-your-answers" should {
      //    TODO: 11188 Fix for all
      "return OK and render page when valid data present" in new TestSetup(legacyRegime) {

        cacheJourney(subscriptionFullJourney(legacyRegime))

        val result = controller.showPage(legacyRegime)(fakeRequest).futureValue

        status(result) shouldBe OK
        val body = contentAsString(result)

        if (legacyRegime == PAYE) {
          body should include("Contact name")
          body should include("My Name")
          body should not include("Business name")
          body should not include("Custom name")
        } else {
          body should include("Business name")
          body should include("Custom name")
          body should not include("Contact name")
          body should not include("My Name")
        }
        body should include("Telephone number")
        body should include("123456")
        body should include("Email address")
        body should include("custom@test.com")
        body should include("Address")
        body should include("Line 1")
      }

      "return BAD_REQUEST when journey data missing" in new TestSetup(legacyRegime) {
        val invalidJourney = SubscriptionJourney(
          asaDetails = AgencyDetails(
            agencyName = None,
            agencyEmail = None,
            agencyTelephone = None,
            agencyAddress = None
          ),
          useCustomBusinessName = Some(true),
          businessNameAnswer = None,
          useCustomPhoneNumber = Some(true),
          phoneNumberAnswer = None,
          useCustomEmail = Some(true),
          emailAnswer = None,
          useCustomAddress = Some(true),
          addressAnswer = None
        )

        cacheJourney(invalidJourney)

        val result = controller.showPage(legacyRegime)(fakeRequest).futureValue

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("missing Legacy Subscription CYA data")
      }
    }

    s"POST /subscription/$legacyRegime/check-your-answers" should {
      //    TODO: 11188 Fix for all
      "redirect when submission succeeds" in new TestSetup(legacyRegime) {

        cacheJourney(subscriptionFullJourney(legacyRegime))
        givenStartLegacySubscriptionResponse(legacyRegime, OK)

        val result = controller.onSubmit(legacyRegime)(fakeRequest).futureValue

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value should include("/confirmation")
      }

      "return BAD_REQUEST when journey data missing" in new TestSetup(legacyRegime) {
        cacheJourney(subscriptionBaseJourney)

        val result = controller.onSubmit(legacyRegime)(fakeRequest).futureValue

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("missing Legacy Subscription CYA data")
      }
    }

  })

}
