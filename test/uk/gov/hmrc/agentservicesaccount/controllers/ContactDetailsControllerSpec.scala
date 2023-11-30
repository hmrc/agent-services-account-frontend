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

package uk.gov.hmrc.agentservicesaccount.controllers

import com.google.inject.AbstractModule
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.{ConfirmedResponseAddress, ConfirmedResponseAddressDetails, Country, JourneyConfigV2}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsControllerSpec extends UnitSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience with MockFactory {

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
                      |    "identifiers": [{ "key": "AgentReferenceNumber", "value": "XAARN0123456789" }]
                      |  }]
                      |
                      |}""".stripMargin)
    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(retrieval.reads.reads(authJson).get)
  }

  val overrides = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[AgentClientAuthorisationConnector]).toInstance(stub[AgentClientAuthorisationConnector])
      bind(classOf[AddressLookupConnector]).toInstance(stub[AddressLookupConnector])
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

    val controller: ContactDetailsController = app.injector.instanceOf[ContactDetailsController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]

    // make sure this value is cleared from the session
    sessionCache.delete(UPDATED_CONTACT_DETAILS)(fakeRequest()).futureValue
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri).withSession(
      SessionKeys.authToken -> "Bearer XYZ",
      SessionKeys.sessionId -> "session-x"
    )

  "GET /contact-details" should {
    "display the current details page" in new TestSetup {
      val result = controller.showCurrentContactDetails()(fakeRequest()).futureValue
      status(result) shouldBe OK
      contentAsString(result) should include("Contact details")
      contentAsString(result) should include("My Agency")
    }
    "clear any previous draft new contacts" in new TestSetup {
      implicit val request: Request[AnyContent] = fakeRequest()
      sessionCache.put(UPDATED_CONTACT_DETAILS, agencyDetails.copy(agencyName = Some("New and Improved Agency"))).futureValue

      val result = controller.showCurrentContactDetails(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("Contact details")
      contentAsString(result.futureValue) should include("My Agency")

      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue shouldBe None
    }
  }

  "GET /update-business-name" should {
    "display the enter business name page" in new TestSetup {
      val result = controller.showChangeBusinessName()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What is the name")
    }
  }

  "POST /update-business-name" should {
    "store the new name in session and redirect to review new details page" in new TestSetup {
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("name" -> "New and Improved Agency")
      val result = controller.submitChangeBusinessName()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(routes.ContactDetailsController.showCheckNewDetails.url)
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyName) shouldBe Some("New and Improved Agency")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("name" -> "&^%£$)(")
      val result = controller.submitChangeBusinessName()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyName) shouldBe None // new name not added to session
    }
  }

  "GET /update-email-address" should {
    "display the enter email address page" in new TestSetup {
      val result = controller.showChangeEmailAddress()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What is the email")
    }
  }

  "POST /update-email-address" should {
    "store the new email address in session and redirect to review new details page" in new TestSetup {
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("emailAddress" -> "new@email.com")
      val result = controller.submitChangeEmailAddress()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(routes.ContactDetailsController.showCheckNewDetails.url)
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe Some("new@email.com")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("emailAddress" -> "invalid at email dot com")
      val result = controller.submitChangeEmailAddress()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyEmail) shouldBe None // new email not added to session
    }
  }

  "GET /update-telephone-number" should {
    "display the enter telephone number page" in new TestSetup {
      val result = controller.showChangeTelephoneNumber()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What is the telephone")
    }
  }

  "POST /update-telephone-number" should {
    "store the new telephone number in session and redirect to review new details page" in new TestSetup {
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("telephoneNumber" -> "01234 567 890")
      val result = controller.submitChangeTelephoneNumber()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(routes.ContactDetailsController.showCheckNewDetails.url)
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyTelephone) shouldBe Some("01234 567 890")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("telephoneNumber" -> "0800 FAKE NO")
      val result = controller.submitChangeTelephoneNumber()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyTelephone) shouldBe None // new email not added to session
    }
  }

  "GET /update-address" should {
    "redirect to the external service to look up an address" in new TestSetup {
      val result = controller.startAddressLookup()(fakeRequest())
      status(result) shouldBe SEE_OTHER
      header("Location", result).get should include("mock-address-lookup-url")
    }
  }

  "GET /address-lookup-finish" should {
    "store the new address in session and redirect to review new details page" in new TestSetup {
      implicit val request = fakeRequest()
      val result = controller.finishAddressLookup(Some("foo"))(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(routes.ContactDetailsController.showCheckNewDetails.url)
      sessionCache.get(UPDATED_CONTACT_DETAILS).futureValue.flatMap(_.agencyAddress).map(_.addressLine1) shouldBe Some("26 New Street") // the new address
    }
  }

  "GET /check-contact-details" should {
    "display the review details page if there are any pending new details in session" in new TestSetup {
      implicit val request = fakeRequest()
      sessionCache.put(UPDATED_CONTACT_DETAILS, agencyDetails.copy(agencyName = Some("New and Improved Agency"))).futureValue
      val result = controller.showCheckNewDetails()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("Check your new contact details")
      contentAsString(result.futureValue) should include("New and Improved Agency")
    }

    "redirect to 'view current details' if there are no new details in session" in new TestSetup {
      implicit val request = fakeRequest()
      sessionCache.delete(UPDATED_CONTACT_DETAILS).futureValue
      val result = controller.showCheckNewDetails()(fakeRequest())
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(routes.ContactDetailsController.showCurrentContactDetails.url)
    }
  }
}
