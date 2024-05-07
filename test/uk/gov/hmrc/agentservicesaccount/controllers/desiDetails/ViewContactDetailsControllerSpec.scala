/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import com.google.inject.AbstractModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.{DRAFT_NEW_CONTACT_DETAILS, DRAFT_SUBMITTED_BY}
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{DesignatoryDetails, YourDetails}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.{TestConstants, UnitSpec}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ViewContactDetailsControllerSpec extends UnitSpec
  with Matchers
  with GuiceOneAppPerSuite
  with ScalaFutures
  with IntegrationPatience
  with MockFactory
  with TestConstants{

  private val testArn = Arn("XXARN0123456789")


  private val submittedByDetails = YourDetails(
    fullName = "John Tester",
    telephone = "01903 209919"
  )

  private val details = DesignatoryDetails(agencyDetails, emptyOtherServices)

  private val stubAuthConnector = new AuthConnector {
    private val authJson = Json.parse(s"""{
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
      bind(classOf[AgentAssuranceConnector]).toInstance(stub[AgentAssuranceConnector])
      bind(classOf[PendingChangeRequestRepository]).toInstance(stub[PendingChangeRequestRepository])
      bind(classOf[AuthConnector]).toInstance(stubAuthConnector)
    }
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(
    "auditing.enabled" -> false,
    "metrics.enabled" -> false,
    "suspendedContactDetails.sendEmail" -> false
  ).overrides(overrides).build()

  trait TestSetup {
    val agentAssuranceConnector: AgentAssuranceConnector = app.injector.instanceOf[AgentAssuranceConnector]
    (agentAssuranceConnector.getAgentRecord(_: HeaderCarrier, _: ExecutionContext)).when(*, *).returns(Future.successful(agentRecord))

    val controller: ViewContactDetailsController = app.injector.instanceOf[ViewContactDetailsController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]
    val pcodRepository: PendingChangeRequestRepository = app.injector.instanceOf[PendingChangeRequestRepository]
    def noPendingChangesInRepo(): Unit = {
      (pcodRepository.find(_: Arn)).when(*).returns(Future.successful(None))
    }
    def pendingChangesExistInRepo(): Unit = {
      (pcodRepository.find(_: Arn)).when(*).returns(Future.successful(
        Some(PendingChangeRequest(
          testArn,
          Instant.now()
        ))))
    }

    (pcodRepository.insert(_: PendingChangeRequest)).when(*).returns(Future.successful(()))

    // make sure these values are cleared from the session
    sessionCache.delete(DRAFT_SUBMITTED_BY)(fakeRequest()).futureValue
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri).withSession(
      SessionKeys.authToken -> "Bearer XYZ",
      SessionKeys.sessionId -> "session-x"
    )

  "GET /manage-account/contact-details/view" should {
    "display the current details page normally if there is no change pending" in new TestSetup {
      noPendingChangesInRepo()
      val result: Result = controller.showPage()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
    }
    "display the current details page with a warning and change locked-out if there is a change pending" in new TestSetup {
      pendingChangesExistInRepo()
      val result: Result = controller.showPage()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
      contentAsString(result) should include("New contact details were submitted on")
    }
    "clear any previous draft new contacts" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: Request[AnyContent] = fakeRequest()
      sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, details.copy(
        agencyDetails = details.agencyDetails.copy(agencyName = Some("New and Improved Agency"))
      )).futureValue

      val result: Future[Result] = controller.showPage(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("Contact details")
      contentAsString(result.futureValue) should include("My Agency")

      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue shouldBe None
    }
  }


}

