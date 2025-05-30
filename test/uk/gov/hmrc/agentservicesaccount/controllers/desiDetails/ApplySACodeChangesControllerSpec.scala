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
import uk.gov.hmrc.agentservicesaccount.controllers.CURRENT_SELECTED_CHANGES
import uk.gov.hmrc.agentservicesaccount.controllers.DRAFT_NEW_CONTACT_DETAILS
import uk.gov.hmrc.agentservicesaccount.models.ApplySaCodeChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.apply_sa_code_changes
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ApplySACodeChangesControllerSpec
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
    protected val mockView: apply_sa_code_changes = mock[apply_sa_code_changes]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController
    extends ApplySACodeChangesController(
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

      mockSessionCache.get(CURRENT_SELECTED_CHANGES)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))

      mockSessionCache.get(DRAFT_NEW_CONTACT_DETAILS)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get.copy(agencyEmail = Some("new@test.com")),
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))
      // TODO update to return correct details

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockView.apply(*[Form[ApplySaCodeChanges]])(
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

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

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

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)(
        *[Reads[DesignatoryDetails]],
        *[Request[Any]]
      ) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))
      mockSessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES)(*[Reads[Set[String]]], *[Request[Any]]) returns Future.successful(Some(Set("email")))

      mockSessionCache.put[DesignatoryDetails](
        DRAFT_NEW_CONTACT_DETAILS,
        desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(saChanges = SaChanges(true, None)))
      )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[ApplySaCodeChanges]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("applyChanges" -> "true")
      )

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/contact-details/enter-SA-code"
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

      mockSessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)(
        *[Reads[DesignatoryDetails]],
        *[Request[Any]]
      ) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

      mockSessionCache.put[DesignatoryDetails](
        DRAFT_NEW_CONTACT_DETAILS,
        desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(saChanges = SaChanges(true, None)))
      )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[ApplySaCodeChanges]])(
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
