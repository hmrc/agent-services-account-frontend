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
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector, EmailVerificationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, DRAFT_NEW_CONTACT_DETAILS, DRAFT_SUBMITTED_BY, desiDetails}
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.{ConfirmedResponseAddress, ConfirmedResponseAddressDetails, Country, JourneyConfigV2}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeOfDetails
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.{TestConstants, UnitSpec}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class UpdateNameControllerSpec extends UnitSpec
  with Matchers
  with GuiceOneAppPerSuite
  with ScalaFutures
  with IntegrationPatience
  with MockFactory
  with TestConstants {

  private val testArn = Arn("XXARN0123456789")

  private val emptyOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = false,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = false,
      ctAgentReference = None
    )
  )

  private val submittedByDetails = YourDetails(
    fullName = "John Tester",
    telephone = "01903 209919"
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
    (acaConnector.getAgentRecord()(_: HeaderCarrier, _: ExecutionContext)).when(*, *).returns(Future.successful(agentRecord))

    val alfConnector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]
    (alfConnector.init(_: JourneyConfigV2)(_: HeaderCarrier)).when(*, *).returns(Future.successful("mock-address-lookup-url"))
    (alfConnector.getAddress(_: String)(_: HeaderCarrier)).when(*, *).returns(Future.successful(confirmedAddressResponse))

    val evConnector: EmailVerificationConnector = app.injector.instanceOf[EmailVerificationConnector]

    val controller: UpdateNameController = app.injector.instanceOf[UpdateNameController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]
    val pcodRepository: PendingChangeOfDetailsRepository = app.injector.instanceOf[PendingChangeOfDetailsRepository]

    def noPendingChangesInRepo(): Unit = {
      (pcodRepository.find(_: Arn)).when(*).returns(Future.successful(None))
    }
    def pendingChangesExistInRepo(): Unit = {
      (pcodRepository.find(_: Arn)).when(*).returns(Future.successful(Some(
        PendingChangeOfDetails(
          testArn,
          agentDetails,
          agentDetails,
          emptyOtherServices,
          Instant.now(),
          submittedByDetails
        ))))
    }

    (pcodRepository.insert(_: PendingChangeOfDetails)).when(*).returns(Future.successful(()))

    // make sure these values are cleared from the session
    sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)(fakeRequest()).futureValue
    sessionCache.delete(DRAFT_SUBMITTED_BY)(fakeRequest()).futureValue
    sessionCache.delete(CURRENT_SELECTED_CHANGES)(fakeRequest()).futureValue
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri).withSession(
      SessionKeys.authToken -> "Bearer XYZ",
      SessionKeys.sessionId -> "session-x"
    )

  "GET /manage-account/contact-details/new-name" should {
    "display the enter business name page" in new TestSetup {
      noPendingChangesInRepo()
      val result = controller.showPage()(fakeRequest())
      status(result) shouldBe OK
//      contentAsString(result.futureValue) should include("What's the new name") TODO
    }
  }

  "POST /manage-account/contact-details/new-name" should {
    "store the new name in session and redirect to apply SA code page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("name" -> "New and Improved Agency")
      val result = controller.onSubmit()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ApplySACodeChangesController.showPage.url)
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyDetails.agencyName) shouldBe Some("New and Improved Agency")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request = fakeRequest("POST").withFormUrlEncodedBody("name" -> "&^%£$)(")
      val result = controller.onSubmit()(request)
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(DRAFT_NEW_CONTACT_DETAILS).futureValue.flatMap(_.agencyDetails.agencyName) shouldBe None // new name not added to session
    }
  }

  "existing pending changes" should {
    "cause the user to be redirected away from any 'update' endpoints" in new TestSetup {
      def shouldRedirect(endpoint: Action[AnyContent]): Unit = {
        val result = endpoint(fakeRequest())
        status(result) shouldBe SEE_OTHER
        header("Location", result) shouldBe Some(desiDetails.routes.ViewContactDetailsController.showPage.url)
      }

      pendingChangesExistInRepo()
      shouldRedirect(controller.showPage())
      shouldRedirect(controller.onSubmit())
    }
  }
}
