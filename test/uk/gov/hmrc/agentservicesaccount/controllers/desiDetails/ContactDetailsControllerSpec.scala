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
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.AddressLookupConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.connectors.EmailVerificationConnector
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.ConfirmedResponseAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.ConfirmedResponseAddressDetails
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.Country
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.JourneyConfigV2
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ContactDetailsControllerSpec
extends UnitSpec
with Matchers
with GuiceOneAppPerSuite
with ScalaFutures
with IntegrationPatience
with MockFactory
with TestConstants {

  private val testArn: Arn = Arn("XXARN0123456789")

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

  private val stubAuthConnector: AuthConnector =
    new AuthConnector {
      private val authJson: JsValue = Json.parse(s"""{
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
      def authorise[A](
        predicate: Predicate,
        retrieval: Retrieval[A]
      )(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[A] = Future.successful(retrieval.reads.reads(authJson).get)
    }

  val overrides: AbstractModule =
    new AbstractModule() {
      override def configure(): Unit = {
        bind(classOf[AgentAssuranceConnector]).toInstance(stub[AgentAssuranceConnector])
        bind(classOf[AddressLookupConnector]).toInstance(stub[AddressLookupConnector])
        bind(classOf[EmailVerificationConnector]).toInstance(stub[EmailVerificationConnector])
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
    (agentAssuranceConnector.getAgentRecord(_: RequestHeader)).when(*).returns(Future.successful(agentRecord))

    val alfConnector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]
    (alfConnector.init(_: JourneyConfigV2)(_: RequestHeader)).when(*, *).returns(Future.successful("mock-address-lookup-url"))
    (alfConnector.getAddress(_: String)(_: RequestHeader)).when(*, *).returns(Future.successful(confirmedAddressResponse))

    val evConnector: EmailVerificationConnector = app.injector.instanceOf[EmailVerificationConnector]

    val contactDetailsController: ContactDetailsController = app.injector.instanceOf[ContactDetailsController]
    val updateNameController: UpdateNameController = app.injector.instanceOf[UpdateNameController]
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
        )
      )))
    }

    (pcodRepository.insert(_: PendingChangeRequest)(_: RequestHeader)).when(*, *).returns(Future.successful(()))

    // make sure these values are cleared from the session
    sessionCache.delete(draftNewContactDetailsKey)(fakeRequest()).futureValue
    sessionCache.delete(draftSubmittedByKey)(fakeRequest()).futureValue
    sessionCache.delete(emailPendingVerificationKey)(fakeRequest()).futureValue
    sessionCache.delete(currentSelectedChangesKey)(fakeRequest()).futureValue

  }

  "GET /manage-account/contact-details/new-address" should {
    "redirect to the external service to look up an address" in new TestSetup {
      noPendingChangesInRepo()
      val result: Future[Result] = contactDetailsController.startAddressLookup()(fakeRequest())
      status(result) shouldBe SEE_OTHER
      header("Location", result).get should include("mock-address-lookup-url")
    }
  }

  "GET /contact-details/address-lookup-finish" should {
    "store the new address in session and redirect to review new details page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest()
      val result: Future[Result] = contactDetailsController.finishAddressLookup(Some("foo"))(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ApplySACodeChangesController.showPage.url)
      sessionCache.get(draftNewContactDetailsKey).futureValue.flatMap(_.agencyDetails.agencyAddress).map(_.addressLine1) shouldBe Some("26 New Street") // the new address
    }
  }

  "GET /manage-account/contact-details/confirmation" should {
    "display the confirmation page" in new TestSetup {
      pendingChangesExistInRepo()
      val result: Future[Result] = contactDetailsController.showChangeSubmitted()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("You have submitted new contact details")
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
      shouldRedirect(contactDetailsController.startAddressLookup())
      shouldRedirect(contactDetailsController.finishAddressLookup(None))
    }
  }

}
