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
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.BaseISpec
import support.TestConstants
import support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.UpdateBusinessNameController
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpdateBusinessNameControllerISpec
extends BaseISpec
with UnitSpec
with Matchers
with GuiceOneAppPerSuite
with ScalaFutures
with IntegrationPatience
with MockFactory
with TestConstants {

  class TestSetup {

    val legacyRegime: LegacyRegime = CT

    private val testArn = "TARN0000001"

    private val stubAuthConnector =
      new AuthConnector {
        private val authJson = Json.obj(
          "allEnrolments" -> Json.arr(
            Json.obj(
              "key" -> "HMRC-AS-AGENT",
              "identifiers" -> Json.arr(
                Json.obj(
                  "key" -> "AgentReferenceNumber",
                  "value" -> testArn
                )
              )
            )
          ),
          "affinityGroup" -> "Agent",
          "credentialRole" -> "User",
          "optionalCredentials" -> Json.obj(
            "providerId" -> "foo",
            "providerType" -> "bar"
          )
        )

        override def authorise[A](
          predicate: Predicate,
          retrieval: Retrieval[A]
        )(implicit
          hc: HeaderCarrier,
          ec: ExecutionContext
        ): Future[A] = Future.successful(retrieval.reads.reads(authJson).get)
      }

    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
    val agentServicesAccountConnector: AgentServicesAccountConnector =
      new AgentServicesAccountConnector(
        http = stub[HttpClientV2],
        appConfig = appConfig
      ) {
        override def getAgentRecord(implicit rh: RequestHeader): Future[AgentDetailsDesResponse] = Future.successful(
          uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse(
            uniqueTaxReference = Some(uk.gov.hmrc.agentservicesaccount.models.Utr("0123456789")),
            agencyDetails = Some(
              uk.gov.hmrc.agentservicesaccount.models.AgencyDetails(
                agencyName = Some("Test Agency"),
                agencyEmail = None,
                agencyTelephone = None,
                agencyAddress = None
              )
            ),
            suspensionDetails = Some(uk.gov.hmrc.agentservicesaccount.models.SuspensionDetails(suspensionStatus = false, None))
          )
        )
      }

    val overrides: AbstractModule =
      new AbstractModule() {
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

    val controller: UpdateBusinessNameController = app.injector.instanceOf[UpdateBusinessNameController]

    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]

    val session: Map[String, String] = Map("sessionId" -> "test-session")

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(session.toSeq: _*)

    def cacheJourney(journey: CtJourney): Unit = {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
      implicit val writes: OWrites[CtJourney] = Json.writes[CtJourney]
      sessionCache.put(ctJourneyKey, journey).futureValue
    }

  }

  "GET /update-business-name" should {

    "render empty form on first visit" in new TestSetup {
      cacheJourney(ctSubscriptionBaseJourney)

      private val result = controller.showPage(legacyRegime)(FakeRequest()).futureValue

      status(result) shouldBe OK
      contentAsString(result) should include("Test Agency")
    }

    "render pre-filled form when journey has existing answers" in new TestSetup {
      private val journey = ctSubscriptionBaseJourney.copy(
        useCustomBusinessName = Some(true),
        businessNameAnswer = Some("Custom Name Ltd")
      )

      cacheJourney(journey)

      private val result = controller.showPage(legacyRegime)(FakeRequest()).futureValue

      status(result) shouldBe OK
      private val content = contentAsString(result)

      content should include("""value="true"""")
      content should include("businessNameNew")
    }
  }

  "POST /update-business-name" should {

    "return BAD_REQUEST when form is invalid" in new TestSetup {
      cacheJourney(ctSubscriptionBaseJourney)

      private val request = FakeRequest().withSession(session.toSeq: _*).withFormUrlEncodedBody(
        "useAsaData" -> ""
      )

      private val result = controller.onSubmit(legacyRegime)(request).futureValue

      status(result) shouldBe BAD_REQUEST
    }

    val journeyWithRedirectLocations = List(
      (ctSubscriptionBaseJourney, "phone-number", "not complete"),
      (ctSubscriptionFullJourney, "check-your-answers", "complete")
    )

    journeyWithRedirectLocations.foreach(journeyWithRedirectLocation => {
      s"update journey and redirect to ${journeyWithRedirectLocation._2}" +
        s"when using ASA business name and journey ${journeyWithRedirectLocation._3}" in new TestSetup {
          private val request = FakeRequest(POST, "/")
            .withSession(session.toSeq: _*)
            .withFormUrlEncodedBody(
              "businessNameUseAsaData" -> "true"
            )

          implicit val implicitRequest: FakeRequest[AnyContentAsFormUrlEncoded] = request

          cacheJourney(journeyWithRedirectLocation._1)

          private val result = controller.onSubmit(legacyRegime)(request).futureValue
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe
            Some(s"/agent-services-account/subscription/${journeyWithRedirectLocation._2}/$legacyRegime")

          val updated: Option[CtJourney] = sessionCache.get[CtJourney](ctJourneyKey).futureValue
          updated shouldBe defined
          updated.get.useCustomBusinessName shouldBe Some(false)
          updated.value.businessNameAnswer shouldBe None
        }

      s"update journey and redirect to ${journeyWithRedirectLocation._2}" +
        s"when using custom business name and journey ${journeyWithRedirectLocation._3}" in new TestSetup {
          private val request = FakeRequest(POST, "/")
            .withSession(session.toSeq: _*)
            .withFormUrlEncodedBody(
              "businessNameUseAsaData" -> "false",
              "businessNameNew" -> "My Custom Ltd"
            )

          implicit val implicitRequest: FakeRequest[AnyContentAsFormUrlEncoded] = request

          cacheJourney(journeyWithRedirectLocation._1)

          private val result = controller.onSubmit(legacyRegime)(request).futureValue
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe
            Some(s"/agent-services-account/subscription/${journeyWithRedirectLocation._2}/$legacyRegime")

          val updated: Option[CtJourney] = sessionCache.get[CtJourney](ctJourneyKey).futureValue
          updated shouldBe defined
          updated.value.useCustomBusinessName shouldBe Some(true)
          updated.value.businessNameAnswer shouldBe Some("My Custom Ltd")
        }
    })
  }

}
