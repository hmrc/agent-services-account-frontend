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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsBeforeYouStartControllerSpec extends UnitSpec
  with Matchers
  with GuiceOneAppPerSuite
  with ScalaFutures
  with IntegrationPatience
  with MockFactory {

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
      "GB"))
  )

  private val stubAuthConnector = new AuthConnector {
    private val authJson = Json.parse(
      s"""{
         |  "internalId": "some-id",
         |  "affinityGroup": "Agent",
         |  "credentialRole": "User",
         |  "allEnrolments": [{
         |    "key": "HMRC-AS-AGENT",
         |    "identifiers": [{ "key": "AgentReferenceNumber", "value": "${testArn.value}" }]
         |  }],
         |  "optionalCredentials": {
         |    "providerId": "foo",
         |    "providerType": "bar"
         |  }
         |}""".stripMargin)

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(retrieval.reads.reads(authJson).get)
  }


  val overrides = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[AgentClientAuthorisationConnector]).toInstance(stub[AgentClientAuthorisationConnector])
      bind(classOf[AuthConnector]).toInstance(stubAuthConnector)
    }
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(
    "auditing.enabled" -> false,
    "metrics.enabled" -> false,
    "suspendedContactDetails.sendEmail" -> false
  ).overrides(overrides).build()


  trait TestSetup {
    val acaConnector: AgentClientAuthorisationConnector = app.injector.instanceOf[AgentClientAuthorisationConnector]
    (acaConnector.getSuspensionDetails()(_: HeaderCarrier, _: ExecutionContext)).when(*, *).returns(Future.successful(SuspensionDetails(false, None)))
    (acaConnector.getAgencyDetails()(_: HeaderCarrier, _: ExecutionContext)).when(*, *).returns(Future.successful(Some(agencyDetails)))

    val controller: ContactDetailsBeforeYouStartController = app.injector.instanceOf[ContactDetailsBeforeYouStartController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]

    // make sure these values are cleared from the session
    sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)(fakeRequest()).futureValue
    sessionCache.delete(EMAIL_PENDING_VERIFICATION)(fakeRequest()).futureValue
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri).withSession(
      SessionKeys.authToken -> "Bearer XYZ",
      SessionKeys.sessionId -> "session-x"
    )


  "GET /manage-account/contact-details/start-update" should {
    "display the current details page normally if there is no change pending" in new TestSetup {
      val result = controller.showBeforeYouStartPage()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
    }
  }
}