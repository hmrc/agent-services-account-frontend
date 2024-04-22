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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, Result}
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsStatus, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.confirm_supervisory_body
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.DataKey

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ConfirmSupervisoryBodyControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  private val amlsDetails = AmlsDetails(supervisoryBody = "HMRC")
  private val amlsDetailsResponse = Future.successful(amlsDetails)

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsDetailsUK
  )

  private val overseasAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsNonUK,
    newAmlsBody = Some("OS AMLS"),
    newRegistrationNumber = Some("AMLS123"),
    newExpirationDate = Some(LocalDate.parse("2024-10-10"))
  )

  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockAgentClientAuthorisationConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockActions =
      new Actions(mockAgentClientAuthorisationConnector, mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockUpdateAmlsJourneyRepository: UpdateAmlsJourneyRepository = mock[UpdateAmlsJourneyRepository]
    protected val mockView: confirm_supervisory_body = mock[confirm_supervisory_body]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey = DataKey[UpdateAmlsJourney]("amlsJourney")

    object TestController extends ConfirmSupervisoryBodyController(mockActions, mockUpdateAmlsJourneyRepository, mockView, cc)(mockAppConfig, ec)
  }

  "showPage" should {
    "display the page with an empty form if first time" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[Boolean]], *[String])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "display the page with a filled out form if user is revisiting" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(ukAmlsJourney.copy(isAmlsBodyStillTheSame = Some(false))))

      mockView.apply(*[Form[Boolean]], *[String])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "onSubmit" should {

      "return 303 SEE_OTHER and redirect to /confirm-registration-number when YES is selected" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse

        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

        mockUpdateAmlsJourneyRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

        mockUpdateAmlsJourneyRepository.putSession(
          dataKey, ukAmlsJourney.copy(isAmlsBodyStillTheSame = Some(true), newAmlsBody = Some("HMRC")))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

        mockView.apply(*[Form[Boolean]], "HMRC")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.onSubmit(
          FakeRequest("POST", "/").withFormUrlEncodedBody("accept" -> "true"))

        status(result) mustBe SEE_OTHER
        Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-registration-number"
      }

      "return 303 SEE_OTHER and redirect to /enter-registration-number when YES is selected for overseas agent" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse

        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

        mockUpdateAmlsJourneyRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(overseasAmlsJourney))

        mockUpdateAmlsJourneyRepository.putSession(
          dataKey, overseasAmlsJourney.copy(isAmlsBodyStillTheSame = Some(true), newAmlsBody = Some("HMRC")))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

        mockView.apply(*[Form[Boolean]], "HMRC")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.onSubmit(
          FakeRequest("POST", "/").withFormUrlEncodedBody("accept" -> "true"))

        status(result) mustBe SEE_OTHER
        Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"
      }

      "return 303 SEE_OTHER and redirect to /new-supervisory-body when NO is selected" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse

        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

        mockUpdateAmlsJourneyRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

        mockUpdateAmlsJourneyRepository.putSession(
          dataKey, ukAmlsJourney.copy(isAmlsBodyStillTheSame = Some(false)))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

        mockView.apply(*[Form[Boolean]], "HMRC")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.onSubmit(
          FakeRequest("POST", "/").withFormUrlEncodedBody("accept" -> "false"))

        status(result) mustBe SEE_OTHER
        Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-supervisory-body"
      }


      "return BadRequest when invalid form submission" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse

        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

        mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns
          Future.successful(Some(ukAmlsJourney))

        mockView.apply(*[Form[Boolean]], "HMRC")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.onSubmit(
          FakeRequest("POST", "/").withFormUrlEncodedBody("accept" -> ""))

        status(result) mustBe BAD_REQUEST
      }
    }
  }


}
