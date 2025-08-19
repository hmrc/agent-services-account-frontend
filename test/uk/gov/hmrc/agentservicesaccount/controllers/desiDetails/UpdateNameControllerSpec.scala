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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.AddressLookupConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.connectors.EmailVerificationConnector
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
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

class UpdateNameControllerSpec
extends UnitSpec
with GuiceOneAppPerSuite
with IntegrationPatience
with MockFactory
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

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

  private val stubAuthConnector =
    new AuthConnector {
      private val authJson = Json.parse(s"""{
                                           |  "internalId": "some-id",
                                           |  "affinityGroup": "Agent",
                                           |  "credentialRole": "User",
                                           |  "allEnrolments": [{
                                           |    "key": "HMRC-AS-AGENT",
                                           |    "identifiers": [{ "key": "AgentReferenceNumber", "value": "${arn.value}" }]
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

    val controller: UpdateNameController = app.injector.instanceOf[UpdateNameController]
    val sessionCache: SessionCacheService = app.injector.instanceOf[SessionCacheService]
    val pcodRepository: PendingChangeRequestRepository = app.injector.instanceOf[PendingChangeRequestRepository]

    def noPendingChangesInRepo(): Unit = {
      (pcodRepository.find(_: Arn)(_: RequestHeader)).when(*, *).returns(Future.successful(None))
    }
    def pendingChangesExistInRepo(): Unit = {
      (pcodRepository.find(_: Arn)(_: RequestHeader)).when(*, *).returns(Future.successful(Some(
        PendingChangeRequest(
          arn,
          Instant.now()
        )
      )))
    }

    (pcodRepository.insert(_: PendingChangeRequest)(_: RequestHeader)).when(*, *).returns(Future.successful(()))

    // make sure these values are cleared from the session
    sessionCache.delete(draftNewContactDetailsKey)(fakeRequest()).futureValue
    sessionCache.delete(draftSubmittedByKey)(fakeRequest()).futureValue
    sessionCache.delete(currentSelectedChangesKey)(fakeRequest()).futureValue

  }

  "GET /manage-account/contact-details/new-name" should {
    "display the enter business name page" in new TestSetup {
      noPendingChangesInRepo()
      sessionCache.put(currentSelectedChangesKey, Set("businessName")).futureValue
      val result: Future[Result] = controller.showPage()(fakeRequest())
      status(result) shouldBe OK
      contentAsString(result.futureValue) should include("What’s the new name")
    }
  }

  "POST /manage-account/contact-details/new-name" should {
    "store the new name in session and redirect to apply SA code page" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest("POST").withFormUrlEncodedBody("name" -> "New and Improved Agency")
      sessionCache.put(currentSelectedChangesKey, Set("businessName")).futureValue
      val result: Future[Result] = controller.onSubmit()(request)
      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ApplySACodeChangesController.showPage.url)
      sessionCache.get(draftNewContactDetailsKey).futureValue.flatMap(_.agencyDetails.agencyName) shouldBe Some("New and Improved Agency")
    }

    "display an error if the data submitted is invalid" in new TestSetup {
      noPendingChangesInRepo()
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest("POST").withFormUrlEncodedBody("name" -> "&^%£$)(")
      sessionCache.put(currentSelectedChangesKey, Set("businessName")).futureValue
      val result: Future[Result] = controller.onSubmit()(request)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result.futureValue) should include("There is a problem")
      sessionCache.get(draftNewContactDetailsKey).futureValue.flatMap(_.agencyDetails.agencyName) shouldBe None // new name not added to session
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
      shouldRedirect(controller.showPage())
      shouldRedirect(controller.onSubmit())
    }
  }

}
