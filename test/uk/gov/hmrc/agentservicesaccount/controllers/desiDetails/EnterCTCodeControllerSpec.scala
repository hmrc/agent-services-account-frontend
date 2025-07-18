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

import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.enter_ct_code
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EnterCTCodeControllerSpec
extends PlaySpec
with DefaultAwaitTimeout
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  trait Setup {

    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions =
      new AuthActions(
        mockAppConfig,
        mockAuthConnector,
        mockEnvironment
      )

    protected val mockDraftDetailsService: DraftDetailsService = mock[DraftDetailsService]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockActions =
      new Actions(
        mockAgentAssuranceConnector,
        authActions,
        actionBuilder
      )

    protected val mockPendingChangeRequestRepository = mock[PendingChangeRequestRepository]
    protected val mockView: enter_ct_code = mock[enter_ct_code]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController
    extends EnterCTCodeController(
      mockActions,
      mockSessionCache,
      mockDraftDetailsService,
      mockView,
      cc
    )(
      mockAppConfig,
      ec,
      mockPendingChangeRequestRepository
    )

  }

  "showPage" should {
    "display the page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))

      mockSessionCache.get(draftNewContactDetailsKey)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get.copy(agencyEmail = Some("new@test.com")),
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))
      // TODO - update to return correct data

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK

    }

    "return Forbidden when the feature flag is off" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns false

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe NOT_FOUND

    }
  }

  "onSubmit" should {

    "return 303 SEE_OTHER and store data for UK agent " in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get(draftSubmittedByKey)(*[Reads[YourDetails]], *[RequestHeader]) returns Future.successful(None)

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](draftNewContactDetailsKey)(
        *[Reads[DesignatoryDetails]],
        *[Request[Any]]
      ) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

      mockSessionCache.put[DesignatoryDetails](
        draftNewContactDetailsKey,
        desiDetailsWithEmptyOtherServices.copy(otherServices =
          desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, Some(CtUtr("123456"))))
        )
      )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("ctCode" -> "123456")
      )

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/contact-details/your-details"
    }

    "return 303 SEE_OTHER and DO NOT store data for continueWithoutCode " in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get(draftSubmittedByKey)(*[Reads[YourDetails]], *[RequestHeader]) returns Future.successful(None)

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](draftNewContactDetailsKey)(
        *[Reads[DesignatoryDetails]],
        *[Request[Any]]
      ) returns Future.successful(Some(desiDetailsCtChangesOtherServices))

      mockSessionCache.put[DesignatoryDetails](draftNewContactDetailsKey, desiDetailsWithEmptyOtherServices)(
        *[Writes[DesignatoryDetails]],
        *[Request[Any]]
      ) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.continueWithoutCtCode(
        FakeRequest("GET", "/").withFormUrlEncodedBody("body" -> "")
      )

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/contact-details/your-details"
    }

    "return BadRequest when invalid form submission" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](draftNewContactDetailsKey)(
        *[Reads[DesignatoryDetails]],
        *[Request[Any]]
      ) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

      mockSessionCache.put[DesignatoryDetails](
        draftNewContactDetailsKey,
        desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None)))
      )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "")
      )

      status(result) mustBe BAD_REQUEST
    }
  }

}
