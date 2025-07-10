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
import play.api.Environment
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.update_phone
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpdateTelephoneControllerSpec
extends UnitSpec
with DefaultAwaitTimeout
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val testArn = Arn("XXARN0123456789")

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
    protected val mockUpdatePhoneView: update_phone = mock[update_phone]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController
    extends UpdateTelephoneController(
      mockActions,
      mockSessionCache,
      mockDraftDetailsService,
      mockUpdatePhoneView
    )(
      mockAppConfig,
      cc,
      ec,
      mockPendingChangeRequestRepository
    )

  }

  "GET /manage-account/contact-details/new-telephone" should {
    "display the enter telephone number page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("telephone")))

      mockSessionCache.get(draftNewContactDetailsKey)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(None)

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockUpdatePhoneView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.showPage()(fakeRequest())

      status(result) shouldBe OK
    }

    "existing pending changes" should {
      "cause the user to be redirected away from any 'update' endpoints" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns authResponse

        mockAppConfig.enableChangeContactDetails returns true

        mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("telephone")))

        mockSessionCache.get(draftNewContactDetailsKey)(
          *[Reads[DesignatoryDetails]],
          *[RequestHeader]
        ) returns Future.successful(Some(desiDetailsSaChangesOtherServices))

        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

        mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(Some(PendingChangeRequest(arn, Instant.now)))

        val result: Future[Result] = TestController.showPage()(fakeRequest())

        status(result) shouldBe SEE_OTHER
        header("Location", result) shouldBe Some(desiDetails.routes.ViewContactDetailsController.showPage.url)
      }
    }
  }

  "POST /manage-account/contact-details/new-telephone" should {
    "store the new telephone number in session and redirect to apply SA code page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("telephone")))

      mockSessionCache.get(draftNewContactDetailsKey)(
        *[Reads[DesignatoryDetails]],
        *[RequestHeader]
      ) returns Future.successful(Some(desiDetailsSaChangesOtherServices))

      mockSessionCache.get(draftSubmittedByKey)(*[Reads[YourDetails]], *[RequestHeader]) returns Future.successful(None)

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      val result: Future[Result] = TestController.onSubmit()(FakeRequest(POST, "/").withFormUrlEncodedBody("telephoneNumber" -> "01234 567 890"))

      status(result) shouldBe SEE_OTHER
      header("Location", result) shouldBe Some(desiDetails.routes.ApplySACodeChangesController.showPage.url)
    }

    "display an error if the data submitted is invalid" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("telephone")))

      mockSessionCache.get(draftNewContactDetailsKey)(
        *[Reads[DesignatoryDetails]],
        *[RequestHeader]
      ) returns Future.successful(Some(desiDetailsSaChangesOtherServices))

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockUpdatePhoneView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.onSubmit()(FakeRequest(POST, "/").withFormUrlEncodedBody("telephoneNumber" -> "0800 FAKE NO"))

      status(result) shouldBe BAD_REQUEST
    }

    "existing pending changes" should {
      "cause the user to be redirected away from any 'update' endpoints" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns authResponse

        mockAppConfig.enableChangeContactDetails returns true

        mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("telephone")))

        mockSessionCache.get(draftNewContactDetailsKey)(
          *[Reads[DesignatoryDetails]],
          *[RequestHeader]
        ) returns Future.successful(Some(desiDetailsSaChangesOtherServices))

        mockSessionCache.get(draftSubmittedByKey)(*[Reads[YourDetails]], *[RequestHeader]) returns Future.successful(None)

        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

        mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(Some(PendingChangeRequest(arn, Instant.now)))

        val result: Future[Result] = TestController.onSubmit()(FakeRequest(POST, "/").withFormUrlEncodedBody("telephoneNumber" -> "01234 567 890"))

        status(result) shouldBe SEE_OTHER
        header("Location", result) shouldBe Some(desiDetails.routes.ViewContactDetailsController.showPage.url)
      }
    }
  }

}
