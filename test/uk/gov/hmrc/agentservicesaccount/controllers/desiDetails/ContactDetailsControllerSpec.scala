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
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector, EmailVerificationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.{CheckYourAnswers, ContactDetailsController}
import uk.gov.hmrc.agentservicesaccount.controllers.{DRAFT_NEW_CONTACT_DETAILS, EMAIL_PENDING_VERIFICATION, desiDetails}
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.{ConfirmedResponseAddress, ConfirmedResponseAddressDetails, Country, JourneyConfigV2}
import uk.gov.hmrc.agentservicesaccount.models.emailverification.{CompletedEmail, VerificationStatusResponse, VerifyEmailRequest, VerifyEmailResponse}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsControllerSpec extends UnitSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience with MockFactory {

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

  private val confirmedAddressResponse = ConfirmedResponseAddress(
    auditRef = "foo",
    id = Some("bar"),
    address = ConfirmedResponseAddressDetails(
      organisation = Some("My Agency"),
      lines = Some(Seq("26 New Street", "Telford")),
      postcode = Some("TF5 4AA"),
      country = Some(Country("GB", ""))
    )
  )

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
      bind(classOf[AgentClientAuthorisationConnector]).toInstance(stub[AgentClientAuthorisationConnector])
      bind(classOf[AddressLookupConnector]).toInstance(stub[AddressLookupConnector])
      bind(classOf[EmailVerificationConnector]).toInstance(stub[EmailVerificationConnector])
      bind(classOf[PendingChangeOfDetailsRepository]).toInstance(stub[PendingChangeOfDetailsRepository])
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

    val alfConnector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]
    (alfConnector.init(_: JourneyConfigV2)(_: HeaderCarrier)).when(*, *).returns(Future.successful("mock-address-lookup-url"))
    (alfConnector.getAddress(_: String)(_: HeaderCarrier)).when(*, *).returns(Future.successful(confirmedAddressResponse))

    val evConnector: EmailVerificationConnector = app.injector.instanceOf[EmailVerificationConnector]

    val contactDetailsController: ContactDetailsController = app.injector.instanceOf[ContactDetailsController]
    val checkYourAnswersController: CheckYourAnswers = app.injector.instanceOf[CheckYourAnswers]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]
    val pcodRepository: PendingChangeOfDetailsRepository = app.injector.instanceOf[PendingChangeOfDetailsRepository]

    def noPendingChangesInRepo(): Unit = {
      (pcodRepository.find(_: Arn)).when(*).returns(Future.successful(None))
    }
    def pendingChangesExistInRepo(): Unit = {
      (pcodRepository.find(_: Arn)).when(*).returns(Future.successful(Some(PendingChangeOfDetails(testArn, agencyDetails, agencyDetails, Instant.now()))))
    }

    (pcodRepository.insert(_: PendingChangeOfDetails)).when(*).returns(Future.successful(()))

    // make sure these values are cleared from the session
    sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)(fakeRequest()).futureValue
    sessionCache.delete(EMAIL_PENDING_VERIFICATION)(fakeRequest()).futureValue
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri).withSession(
      SessionKeys.authToken -> "Bearer XYZ",
      SessionKeys.sessionId -> "session-x"
    )

  "GET /manage-account/contact-details/view" should {
    "display the current details page normally if there is no change pending" in new TestSetup {
      noPendingChangesInRepo()
      val result = contactDetailsController.showCurrentContactDetails()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
    }
    "display the current details page with a warning and change locked-out if there is a change pending" in new TestSetup {
      pendingChangesExistInRepo()
      val result = contactDetailsController.showCurrentContactDetails()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
      contentAsString(result) should include("New contact details were submitted on")
    }
    "clear any previous draft new contacts" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: Request[AnyContent] = fakeRequest()
      sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, agencyDetails.copy(agencyName = Some("New and Improved Agency"))).futureValue

      val result = contactDetailsController.showCurrentContactDetails(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("Contact details")
      contentAsString(result.futureValue) should include("My Agency")

      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue shouldBe None
    }
  }

  "GET /update-business-name" should {
    "display the enter business name page" in new TestSetup {
      noPendingChangesInRepo()
      val result = contactDetailsController.showChangeBusinessName()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What is the name")
    }
  }

  "POST /update-business-name" should {
    "store the new name in session and redirect to apply SA code page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("name" -> "New and Improved Agency")
      val result = contactDetailsController.submitChangeBusinessName()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ApplySACodeChanges.showPage.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyName) shouldBe Some("New and Improved Agency")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("name" -> "&^%£$)(")
      val result = contactDetailsController.submitChangeBusinessName()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyName) shouldBe None // new name not added to session
    }
  }

  "GET /update-email-address" should {
    "display the enter email address page" in new TestSetup {
      noPendingChangesInRepo()
      val result = contactDetailsController.showChangeEmailAddress()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What is the email")
    }
  }

  "POST /update-email-address" should {
    "(if the email is already verified) store the new email address in session and redirect to review new details page" in new TestSetup {
      noPendingChangesInRepo()
      (evConnector.checkEmail(_: String)(_: HeaderCarrier, _: ExecutionContext)).when(*, *, *).returns(Future.successful(Some(
        VerificationStatusResponse(List(CompletedEmail("new@email.com", verified = true, locked = false)))
      )))
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("emailAddress" -> "new@email.com")
      val result = contactDetailsController.submitChangeEmailAddress()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.CheckYourAnswers.showPage.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe Some("new@email.com")
    }

    "(if the email is locked) redirect to the email-locked page" in new TestSetup {
      noPendingChangesInRepo()
      (evConnector.checkEmail(_: String)(_: HeaderCarrier, _: ExecutionContext)).when(*, *, *).returns(Future.successful(Some(
        VerificationStatusResponse(List(CompletedEmail("new@email.com", verified = false, locked = true)))
      )))
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("emailAddress" -> "new@email.com")
      val result = contactDetailsController.submitChangeEmailAddress()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ContactDetailsController.showEmailLocked.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe None // there should be no change here
    }

    "(if the email is unverified) redirect to the verify-email external journey" in new TestSetup {
      noPendingChangesInRepo()
      (evConnector.checkEmail(_: String)(_: HeaderCarrier, _: ExecutionContext)).when(*, *, *).returns(Future.successful(Some(
        VerificationStatusResponse(List.empty)
      )))
      (evConnector.verifyEmail(_: VerifyEmailRequest)(_: HeaderCarrier, _: ExecutionContext)).when(*, *, *).returns(Future.successful(Some(
        VerifyEmailResponse(redirectUri = "/fake-verify-email-journey")
      )))
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("emailAddress" -> "new@email.com")
      val result = contactDetailsController.submitChangeEmailAddress()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result).get should include("/fake-verify-email-journey")
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe None // there should be no change here
      sessionCache.get(EMAIL_PENDING_VERIFICATION).futureValue shouldBe Some("new@email.com")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("emailAddress" -> "invalid at email dot com")
      val result = contactDetailsController.submitChangeEmailAddress()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe None // new email not added to session
    }
  }

  "POST /email-verification-finish (successful verification journey has finished)" should {
    "store the new email address in session and redirect to review new details page (also clear email verification cache value)" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest()
      sessionCache.put(EMAIL_PENDING_VERIFICATION, "new@email.com").futureValue
      (evConnector.checkEmail(_: String)(_: HeaderCarrier, _: ExecutionContext)).when(*, *, *).returns(Future.successful(Some(
        VerificationStatusResponse(List(CompletedEmail("new@email.com", verified = true, locked = false)))
      )))
      val result = contactDetailsController.finishEmailVerification()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.CheckYourAnswers.showPage.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe Some("new@email.com")
      sessionCache.get(EMAIL_PENDING_VERIFICATION).futureValue shouldBe None
    }
  }

  "GET /update-telephone-number" should {
    "display the enter telephone number page" in new TestSetup {
      noPendingChangesInRepo()
      val result = contactDetailsController.showChangeTelephoneNumber()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What is the telephone")
    }
  }

  "POST /update-telephone-number" should {
    "store the new telephone number in session and redirect to apply SA code page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("telephoneNumber" -> "01234 567 890")
      val result = contactDetailsController.submitChangeTelephoneNumber()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ApplySACodeChanges.showPage.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyTelephone) shouldBe Some("01234 567 890")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("telephoneNumber" -> "0800 FAKE NO")
      val result = contactDetailsController.submitChangeTelephoneNumber()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyTelephone) shouldBe None // new email not added to session
    }
  }

  "GET /update-address" should {
    "redirect to the external service to look up an address" in new TestSetup {
      noPendingChangesInRepo()
      val result = contactDetailsController.startAddressLookup()(fakeRequest())
      status(result) shouldBe SEE_OTHER
      header("Location", result).get should include("mock-address-lookup-url")
    }
  }

  "GET /address-lookup-finish" should {
    "store the new address in session and redirect to review new details page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest()
      val result = contactDetailsController.finishAddressLookup(Some("foo"))(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.CheckYourAnswers.showPage.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyAddress).map(_.addressLine1) shouldBe Some("26 New Street") // the new address
    }
  }

  "GET /check-contact-details" should {
    "display the review details page if there are any pending new details in session" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest()
      sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, agencyDetails.copy(agencyName = Some("New and Improved Agency"))).futureValue
      val result = checkYourAnswersController.showPage()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("Check your new contact details")
      contentAsString(result.futureValue) should include("New and Improved Agency")
    }

    "redirect to 'view current details' if there are no new details in session" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest()
      sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS).futureValue
      val result = checkYourAnswersController.showPage()(fakeRequest())
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ContactDetailsController.showCurrentContactDetails.url)
    }
  }

  "POST /check-contact-details" should {
    "store the pending change of detail in repo and show the 'what happens next' page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST")
      val newDetails = agencyDetails.copy(agencyName = Some("New and Improved Agency"))
      sessionCache.put(DRAFT_NEW_CONTACT_DETAILS, newDetails).futureValue
      val result = checkYourAnswersController.onSubmit()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ContactDetailsController.showChangeSubmitted.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyTelephone) shouldBe None // the 'draft' details should be cleared from cache
      // should have stored the pending change in the repo
      (pcodRepository.insert(_: PendingChangeOfDetails)).verify(argAssert { pcod: PendingChangeOfDetails =>
        pcod.arn shouldBe testArn
        pcod.oldDetails shouldBe agencyDetails
        pcod.newDetails shouldBe newDetails
        Math.abs(Instant.now().toEpochMilli - pcod.timeSubmitted.toEpochMilli) should be < 5000L // approximate time comparison
      })
    }
  }

  "GET /contact-details-submitted" should {
    "display the enter telephone number page" in new TestSetup {
      pendingChangesExistInRepo()
      val result = contactDetailsController.showChangeSubmitted()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("Change of details submitted")
    }
  }

  "existing pending changes" should {
    "cause the user to be redirected away from any 'update' endpoints" in new TestSetup {
      def shouldRedirect(endpoint: Action[AnyContent]): Unit = {
        val result = endpoint(fakeRequest())
        status(result) shouldBe SEE_OTHER
        header("Location", result) shouldBe Some(desiDetails.routes.ContactDetailsController.showCurrentContactDetails.url)
      }

      pendingChangesExistInRepo()
      shouldRedirect(contactDetailsController.showChangeBusinessName())
      shouldRedirect(contactDetailsController.submitChangeBusinessName())
      shouldRedirect(contactDetailsController.showChangeEmailAddress())
      shouldRedirect(contactDetailsController.submitChangeEmailAddress())
      shouldRedirect(contactDetailsController.finishEmailVerification())
      shouldRedirect(contactDetailsController.showChangeTelephoneNumber())
      shouldRedirect(contactDetailsController.submitChangeTelephoneNumber())
      shouldRedirect(contactDetailsController.startAddressLookup())
      shouldRedirect(contactDetailsController.finishAddressLookup(None))
      shouldRedirect(checkYourAnswersController.showPage)
      shouldRedirect(checkYourAnswersController.onSubmit)
    }
  }
}

