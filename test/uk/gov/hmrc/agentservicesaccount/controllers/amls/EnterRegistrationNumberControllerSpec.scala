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

import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc._
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.amlsJourneyKey
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.enter_registration_number
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EnterRegistrationNumberControllerSpec
extends PlaySpec
with DefaultAwaitTimeout
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsDetailsUK,
    newAmlsBody = Some("ACCA"),
    newRegistrationNumber = Some("XAML00000123456")
  )

  private val overseasAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsNonUK,
    newAmlsBody = Some("OS AMLS"),
    newRegistrationNumber = Some("AMLS123"),
    newExpirationDate = Some(LocalDate.parse("2024-10-10")),
    isRegistrationNumberStillTheSame = Some(true)
  )

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

    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockActions =
      new Actions(
        mockAgentAssuranceConnector,
        authActions,
        actionBuilder
      )

    implicit val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]
    protected val mockView: enter_registration_number = mock[enter_registration_number]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController
    extends EnterRegistrationNumberController(
      mockActions,
      mockSessionCacheService,
      mockView,
      cc
    )(mockAppConfig, ec)

  }

  "showPage" should {
    "display the page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockSessionCacheService.get(amlsJourneyKey)(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[String]], *[Boolean])(
        *[Request[Any]],
        *[Messages],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.showPage(cya = false)(fakeRequest)

      status(result) mustBe OK

    }

    "return Forbidden when the feature flag is off" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns false

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      val result: Future[Result] = TestController.showPage(cya = false)(fakeRequest)

      status(result) mustBe FORBIDDEN

    }
  }

  "onSubmit" should {

    "return 303 SEE_OTHER and store data for UK agent " in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockSessionCacheService.get(amlsJourneyKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

      mockSessionCacheService.put(
        amlsJourneyKey,
        ukAmlsJourney.copy(newRegistrationNumber = Some("XAML00000123456"))
      )(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Boolean])(
        *[Request[Any]],
        *[Messages],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] =
        TestController.onSubmit(cya = false)(
          FakeRequest("POST", "/").withFormUrlEncodedBody("number" -> "XAML00000123456")
        )

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/renewal-date"
    }

    "return 303 SEE_OTHER and store data for overseas agent " in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockSessionCacheService.get(amlsJourneyKey)(
        *[Reads[UpdateAmlsJourney]],
        *[Request[Any]]
      ) returns Future.successful(Some(overseasAmlsJourney))

      mockSessionCacheService.put(
        amlsJourneyKey,
        overseasAmlsJourney.copy(newRegistrationNumber = Some("AMLS123"))
      )(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Boolean])(
        *[Request[Any]],
        *[Messages],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] =
        TestController.onSubmit(cya = false)(
          FakeRequest("POST", "/").withFormUrlEncodedBody("number" -> "AMLS123")
        )

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe routes.CheckYourAnswersController.showPage.url
    }

    "return BadRequest when invalid form submission" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockSessionCacheService.get(amlsJourneyKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[String]], *[Boolean])(
        *[Request[Any]],
        *[Messages],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] =
        TestController.onSubmit(cya = false)(
          FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "")
        )

      status(result) mustBe BAD_REQUEST
    }
  }

}
