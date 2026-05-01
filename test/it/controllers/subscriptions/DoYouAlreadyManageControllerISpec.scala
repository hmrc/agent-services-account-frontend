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
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.DoYouAlreadyManageController
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.{routes => subscriptionRoutes}
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DoYouAlreadyManageControllerISpec
extends BaseISpec
with UnitSpec
with Matchers
with GuiceOneAppPerSuite
with ScalaFutures
with IntegrationPatience
with MockFactory
with TestConstants {

  private val legacyRegimes = List(CT, SA, PAYE)

  class TestSetup(legacyRegime: LegacyRegime) {

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

    val controller: DoYouAlreadyManageController = app.injector.instanceOf[DoYouAlreadyManageController]

    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]

    val session: Map[String, String] = Map("sessionId" -> "test-session")

    def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(session.toSeq: _*)

    def cacheJourney(journey: SubscriptionJourney): Unit = {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
      implicit val writes: OWrites[SubscriptionJourney] = Json.writes[SubscriptionJourney]
      sessionCache.put(subscriptionJourneyKey(legacyRegime), journey).futureValue
    }

  }

  legacyRegimes.foreach { regime =>
    s"GET /subscription/$regime/do-you-already-manage" should {

      "render page with empty form" in new TestSetup(regime) {
        cacheJourney(subscriptionBaseJourney)

        val result = controller.showPage(regime)(fakeRequest).futureValue

        status(result) shouldBe OK
      }

      "render pre-filled form when journey has value" in new TestSetup(regime) {
        val journey = subscriptionBaseJourney.copy(
          doYouAlreadyManage = Some(true)
        )

        cacheJourney(journey)

        val result = controller.showPage(regime)(fakeRequest).futureValue

        status(result) shouldBe OK
        contentAsString(result) should include("""value="true"""")
      }
    }
  }

  legacyRegimes.foreach { regime =>
    s"POST /subscription/$regime/do-you-already-manage" should {

      "return BAD_REQUEST when no option selected" in new TestSetup(regime) {
        cacheJourney(subscriptionBaseJourney)

        val request = FakeRequest(POST, "/")
          .withSession(session.toSeq: _*)
          .withFormUrlEncodedBody(
            "doYouAlreadyManage" -> ""
          )

        val result = controller.onSubmit(regime)(request).futureValue

        status(result) shouldBe BAD_REQUEST
      }

      "redirect correctly when YES selected" in new TestSetup(regime) {
        cacheJourney(subscriptionBaseJourney)

        val request = FakeRequest(POST, "/")
          .withSession(session.toSeq: _*)
          .withFormUrlEncodedBody(
            "doYouAlreadyManage" -> "true"
          )

        implicit val implicitRequest: FakeRequest[AnyContentAsFormUrlEncoded] = request

        val result = controller.onSubmit(regime)(request).futureValue

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(subscriptionRoutes.UpdateBusinessNameController.showPage(regime).url)

        val updated = sessionCache.get[SubscriptionJourney](subscriptionJourneyKey(regime)).futureValue
        updated shouldBe defined
        updated.value.doYouAlreadyManage shouldBe Some(true)
      }

      "redirect correctly when NO selected (PAYE)" in new TestSetup(PAYE) {
        cacheJourney(subscriptionBaseJourney)

        val request = FakeRequest(POST, "/")
          .withSession(session.toSeq: _*)
          .withFormUrlEncodedBody(
            "doYouAlreadyManage" -> "false"
          )

        implicit val implicitRequest: FakeRequest[AnyContentAsFormUrlEncoded] = request

        val result = controller.onSubmit(PAYE)(request).futureValue

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(subscriptionRoutes.PayeUpdateContactNameController.showPage.url)

        val updated = sessionCache.get[SubscriptionJourney](subscriptionJourneyKey(PAYE)).futureValue
        updated shouldBe defined
        updated.value.doYouAlreadyManage shouldBe Some(false)
      }

      "redirect correctly when NO selected (non-PAYE)" in new TestSetup(CT) {
        cacheJourney(subscriptionBaseJourney)

        val request = FakeRequest(POST, "/")
          .withSession(session.toSeq: _*)
          .withFormUrlEncodedBody(
            "doYouAlreadyManage" -> "false"
          )

        val result = controller.onSubmit(CT)(request).futureValue

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(subscriptionRoutes.UpdateBusinessNameController.showPage(CT).url)
      }
    }
  }

}
