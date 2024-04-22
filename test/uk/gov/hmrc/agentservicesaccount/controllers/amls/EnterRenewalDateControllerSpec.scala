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
import uk.gov.hmrc.agentservicesaccount.models.{AmlsStatus, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.enter_renewal_date
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.DataKey

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class EnterRenewalDateControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  private val ukUpdateAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsDetailsUK
  )

  private val overseasUpdateAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsNonUK
  )

  private val newExpirationDate = LocalDate.now.plusMonths(11)

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
    protected val mockView: enter_renewal_date = mock[enter_renewal_date]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey = DataKey[UpdateAmlsJourney]("amlsJourney")

    object TestController extends EnterRenewalDateController(mockActions, mockUpdateAmlsJourneyRepository, mockView, cc)(mockAppConfig, ec)
  }

  "showPage" should {
    "display the page for the first time" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(ukUpdateAmlsJourney))

      mockView.apply(*[Form[LocalDate]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "display the page when answer already provided" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(ukUpdateAmlsJourney.copy(newExpirationDate = Some(LocalDate.now().plusMonths(11)))))

      mockView.apply(*[Form[LocalDate]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "Forbidden for overseas agents" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(overseasUpdateAmlsJourney))

      mockView.apply(*[Form[LocalDate]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe FORBIDDEN
    }

    "onSubmit" should {

      "return 303 SEE_OTHER and redirect to /submit-supervision-details" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse

        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockUpdateAmlsJourneyRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukUpdateAmlsJourney))

        mockUpdateAmlsJourneyRepository.putSession(
          dataKey, ukUpdateAmlsJourney.copy(newExpirationDate = Some(newExpirationDate)))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

        mockView.apply(*[Form[LocalDate]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.onSubmit(
          FakeRequest("POST", "/").withFormUrlEncodedBody(
            "endDate.day"   -> newExpirationDate.getDayOfMonth.toString,
            "endDate.month" -> newExpirationDate.getMonthValue.toString,
            "endDate.year"  -> newExpirationDate.getYear.toString)
        )

        status(result) mustBe SEE_OTHER
        Helpers.redirectLocation(result).get mustBe routes.CheckYourAnswersController.showPage.url
      }


      "return BadRequest when invalid form submission" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse

        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns
          Future.successful(Some(ukUpdateAmlsJourney))

        mockView.apply(*[Form[LocalDate]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.onSubmit(
          FakeRequest("POST", "/").withFormUrlEncodedBody(
            "endDate.day"   -> "",
            "endDate.month" -> "",
            "endDate.year"  -> "")
        )

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}

