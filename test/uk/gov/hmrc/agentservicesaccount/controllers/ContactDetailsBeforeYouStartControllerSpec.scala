/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers

import com.google.inject.AbstractModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.models.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.models.Utr
import uk.gov.hmrc.agentservicesaccount.connectors
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.ContactDetailsController
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ContactDetailsBeforeYouStartControllerSpec
extends UnitSpec
with Matchers
with GuiceOneAppPerSuite
with ScalaFutures
with IntegrationPatience
with MockFactory {

  class TestSetup(isAssistant: Boolean = false) {

    private val testArn = Arn("XXARN0123456789")

    private val agencyDetails = AgencyDetails(
      agencyName = Some("My Agency"),
      agencyEmail = Some("abc@abc.com"),
      agencyTelephone = Some("07345678901"),
      agencyAddress = Some(BusinessAddress(
        "25 Any Street",
        Some("Central Grange"),
        Some("Telford"),
        None,
        Some("TF4 3TR"),
        "GB"
      ))
    )

    private val suspensionDetails = SuspensionDetails(suspensionStatus = false, regimes = None)

    private val agentRecord = AgentDetailsDesResponse(
      uniqueTaxReference = Some(Utr("0123456789")),
      agencyDetails = Some(agencyDetails),
      suspensionDetails = Some(suspensionDetails)
    )

    private val stubAuthConnector =
      new AuthConnector {
        private val authJson = Json.obj(
          "allEnrolments" -> Json.arr(
            Json.obj(
              "key" -> "HMRC-AS-AGENT",
              "identifiers" -> Json.arr(
                Json.obj(
                  "key" -> "AgentReferenceNumber",
                  "value" -> testArn.value
                )
              )
            )
          ),
          "affinityGroup" -> "Agent",
          "credentialRole" -> {
            if (isAssistant)
              "Assistant"
            else
              "User"
          },
          "optionalCredentials" -> Json.obj(
            "providerId" -> "foo",
            "providerType" -> "bar"
          )
        )

        def authorise[A](
          predicate: Predicate,
          retrieval: Retrieval[A]
        )(implicit
          hc: HeaderCarrier,
          ec: ExecutionContext
        ): Future[A] = Future.successful(retrieval.reads.reads(authJson).get)
      }

    val overrides =
      new AbstractModule() {
        override def configure(): Unit = {
          bind(classOf[connectors.AgentAssuranceConnector]).toInstance(stub[AgentAssuranceConnector])
          bind(classOf[AuthConnector]).toInstance(stubAuthConnector)
        }
      }

    implicit lazy val app: Application = new GuiceApplicationBuilder().configure(
      "auditing.enabled" -> false,
      "metrics.enabled" -> false,
      "suspendedContactDetails.sendEmail" -> false
    ).overrides(overrides).build()

    val agentAssuranceConnector: AgentAssuranceConnector = app.injector.instanceOf[AgentAssuranceConnector]

    (agentAssuranceConnector.getAgentRecord(_: RequestHeader)).when(*).returns(Future.successful(agentRecord))

    val controller: ContactDetailsController = app.injector.instanceOf[ContactDetailsController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]

    // make sure these values are cleared from the session
    sessionCache.delete(draftNewContactDetailsKey)(fakeRequest()).futureValue
    sessionCache.delete(emailPendingVerificationKey)(fakeRequest()).futureValue

  }

  "GET /manage-account/contact-details/start-update" should {
    "display the current details page normally" in new TestSetup {
      val result = controller.showBeforeYouStartPage()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
    }
  }
  "GET /manage-account/contact-details/start-update" should {
    "display the forbidden page for standard users" in new TestSetup(isAssistant = true) {
      val result = controller.showBeforeYouStartPage()(fakeRequest()).futureValue
      status(result) shouldBe FORBIDDEN
    }
  }

}
