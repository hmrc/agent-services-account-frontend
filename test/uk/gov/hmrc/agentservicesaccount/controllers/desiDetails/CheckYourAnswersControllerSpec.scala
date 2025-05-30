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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import com.google.inject.AbstractModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, DRAFT_NEW_CONTACT_DETAILS, DRAFT_SUBMITTED_BY, EMAIL_PENDING_VERIFICATION, desiDetails}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, PendingChangeRequest}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.{TestConstants, UnitSpec}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersControllerSpec extends UnitSpec
  with Matchers
  with GuiceOneAppPerSuite
  with ScalaFutures
  with IntegrationPatience
  with MockFactory
  with TestConstants {

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

  val overrides: AbstractModule = new AbstractModule() {
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

    (agentAssuranceConnector.getAgentRecord(_:RequestHeader)).when(*).returns(Future.successful(agentRecord))
    (agentAssuranceConnector.postDesignatoryDetails(_: Arn, _: String)(_: RequestHeader)).when(*, *, *).returns(Future.successful(()))

    val checkYourAnswersController: CheckYourAnswersController = app.injector.instanceOf[CheckYourAnswersController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]
    val pcodRepository: PendingChangeRequestRepository = app.injector.instanceOf[PendingChangeRequestRepository]

    def noPendingChangesInRepo(): Unit = {
      (pcodRepository.find(_: Arn)(_: RequestHeader)).when(*, *).returns(Future.successful(None))
    }
    def pendingChangesExistInRepo(): Unit = {
      (pcodRepository.find(_: Arn)(_: RequestHeader)).when(*, *).returns(Future.successful(Some(
        PendingChangeRequest(
          testArn,
          Instant.now()
        ))))
    }

    (pcodRepository.insert(_: PendingChangeRequest)(_: RequestHeader)).when(*, *).returns(Future.successful(()))

    // make sure these values are cleared from the session
    sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)(fakeRequest()).futureValue
    sessionCache.delete(DRAFT_SUBMITTED_BY)(fakeRequest()).futureValue
    sessionCache.delete(EMAIL_PENDING_VERIFICATION)(fakeRequest()).futureValue
    sessionCache.delete(CURRENT_SELECTED_CHANGES)(fakeRequest()).futureValue
  }

  "GET /manage-account/contact-details/check-your-answers" should {
    "display the review details page if there are no pending new details in session" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest()
      sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, details.copy(
        agencyDetails = details.agencyDetails.copy(agencyName = Some("New and Improved Agency"))
      )).futureValue
      sessionCache.put(DRAFT_SUBMITTED_BY, submittedByDetails).futureValue
      sessionCache.put(CURRENT_SELECTED_CHANGES, Set("businessName"))
      val result: Future[Result] = checkYourAnswersController.showPage()(fakeRequest())
      status(result) shouldBe OK
      val resultAsString: String = contentAsString(result.futureValue)
      resultAsString should include("Check your answers")
      resultAsString should include("Business name shown to clients")
      resultAsString should include("New and Improved Agency")
      resultAsString should include("Update other services")
      resultAsString should include("Your details")
    }

    "redirect to 'view current details' if there are no new details in session" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest()
      sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS).futureValue
      sessionCache.delete(DRAFT_SUBMITTED_BY).futureValue
      sessionCache.delete(CURRENT_SELECTED_CHANGES).futureValue
      val result: Future[Result] = checkYourAnswersController.showPage()(fakeRequest())
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ViewContactDetailsController.showPage.url)
    }
  }

  "POST /manage-account/contact-details/check-your-answers" should {
    "store the pending change of detail in repo and show the 'what happens next' page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest("POST")
      val newDetails: AgencyDetails = agencyDetails.copy(agencyName = Some("New and Improved Agency"))
      sessionCache.put(CURRENT_SELECTED_CHANGES, Set("businessName"))
      sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, DesignatoryDetails(newDetails,emptyOtherServices)).futureValue
      sessionCache.put(DRAFT_SUBMITTED_BY, submittedByDetails).futureValue
      val result: Future[Result] = checkYourAnswersController.onSubmit()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ContactDetailsController.showChangeSubmitted.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyDetails.agencyTelephone) shouldBe None // the 'draft' details should be cleared from cache
      // should have stored the pending change in the repo
      (pcodRepository.insert(_: PendingChangeRequest)(_: RequestHeader)).verify(argAssert { pcod: PendingChangeRequest =>
        pcod.arn shouldBe testArn
        Math.abs(Instant.now().toEpochMilli - pcod.timeSubmitted.toEpochMilli) should be < 5000L // approximate time comparison
      }, *)
    }
  }

  "existing pending changes" should {
    "cause the user to be redirected away from any 'update' endpoints" in new TestSetup {
      def shouldRedirect(endpoint: Action[AnyContent]): Unit = {
        val result: Future[Result] = endpoint(fakeRequest())
        status(result) shouldBe SEE_OTHER
        header("Location", result) shouldBe Some(desiDetails.routes.ViewContactDetailsController.showPage.url)
      }

      pendingChangesExistInRepo()
      shouldRedirect(checkYourAnswersController.showPage)
      shouldRedirect(checkYourAnswersController.onSubmit)
    }
  }
}

