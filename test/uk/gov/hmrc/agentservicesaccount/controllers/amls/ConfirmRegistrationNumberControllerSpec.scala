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
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsStatuses, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.confirm_registration_number
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

class ConfirmRegistrationNumberControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  private val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = Some("7"))
  private val amlsDetailsResponse = Future.successful(amlsDetails)

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsDetailsUK
  )

  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockActions =
      new Actions(mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockUpdateAmlsJourneyRepository: UpdateAmlsJourneyRepository = mock[UpdateAmlsJourneyRepository]
    protected val mockView: confirm_registration_number = mock[confirm_registration_number]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey = DataKey[UpdateAmlsJourney]("amlsJourney")

    object TestController extends ConfirmRegistrationNumberController(mockActions, mockUpdateAmlsJourneyRepository, mockView, cc)(mockAppConfig, ec)
  }

  "showPage" should {
    "display the page if the user has an AMLS reg number" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[Boolean]],*[String])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "display the page if the user has an AMLS reg number and previously answered the question" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns
        Future.successful(Some(ukAmlsJourney.copy(isRegistrationNumberStillTheSame = Some(true))))

      mockView.apply(*[Form[Boolean]], *[String])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "redirect the user to /new-registration-number if they do not have an AMLS reg number" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns
        Future.successful(AmlsDetails(supervisoryBody = "HMRC"))

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"
    }
  }

  "onSubmit" should {
    "redirect the user to /new-registration-number if they do not have an AMLS reg number" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns
        Future.successful(AmlsDetails(supervisoryBody = "HMRC"))

      val result: Future[Result] = TestController.onSubmit(fakeRequest)

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"
    }

    "return 303 SEE_OTHER and redirect to /new-registration-number when YES is selected" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

      mockUpdateAmlsJourneyRepository.putSession(
        dataKey, ukAmlsJourney.copy(isRegistrationNumberStillTheSame = Some(true), newRegistrationNumber = Some("7")))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[Boolean]], "7")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("accept" -> "true"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/renewal-date"
    }

    "return 303 SEE_OTHER and redirect to /not-implemented when NO is selected" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

      mockUpdateAmlsJourneyRepository.putSession(
        dataKey, ukAmlsJourney.copy(isRegistrationNumberStillTheSame = Some(false)))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[Boolean]], "7")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("accept" -> "false"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"
    }

    "return BadRequest when invalid form submission" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[RequestHeader]) returns amlsDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[Boolean]], "7")(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(FakeRequest("POST", "/"))

      status(result) mustBe BAD_REQUEST
    }
  }

}
