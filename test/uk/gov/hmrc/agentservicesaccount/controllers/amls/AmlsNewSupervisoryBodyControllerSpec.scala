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
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsStatus, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.new_supervisory_body
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.mongo.cache.DataKey

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class AmlsNewSupervisoryBodyControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()


  private val amlsDetails = AmlsDetails("HMRC")
  private val amlsDetailsResponse = Future.successful(amlsDetails)

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsDetailsUK,
    isAmlsBodyStillTheSame = Some(true),
    newAmlsBody = Some("ACCA")
  )

  private val overseasAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsNonUK,
    newAmlsBody = Some("OS AMLS"),
    newRegistrationNumber = Some("AMLS123"),
    newExpirationDate = Some(LocalDate.parse("2024-10-10"))
  )

  private val amlsBodies = Map(
    "ACCA" -> "Association of Certified Chartered Accountants",
    "HMRC" -> "HM Revenue and Customs (HMRC)"
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

    protected val mockAmlsJourneySessionRepository: UpdateAmlsJourneyRepository = mock[UpdateAmlsJourneyRepository]
    protected val mockAmlsLoader: AMLSLoader = mock[AMLSLoader]
    protected val mockView: new_supervisory_body = mock[new_supervisory_body]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey = DataKey[UpdateAmlsJourney]("amlsJourney")

    object TestController extends AmlsNewSupervisoryBodyController(mockActions, mockAmlsLoader, mockAmlsJourneySessionRepository, mockView, cc)(mockAppConfig, ec)
  }

  "showPage" should {
    "display the page for UK agent" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAgentAssuranceConnector.getAMLSDetails(arn.value)( *[ExecutionContext], *[HeaderCarrier]) returns amlsDetailsResponse

      mockAmlsJourneySessionRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]],*[Request[_]]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(cya = false)(fakeRequest)

      status(result) mustBe OK

    }

    "return Forbidden when the feature flag is off" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns false

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      val result: Future[Result] = TestController.showPage(false)(fakeRequest)

      status(result) mustBe FORBIDDEN

    }
  }

  "onSubmit" should {

    "return 303 SEE_OTHER and save new amls supervisory body to session" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAmlsJourneySessionRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

      mockAmlsJourneySessionRepository.putSession(
        dataKey, ukAmlsJourney.copy(newAmlsBody = Some("Association of Certified Chartered Accountants"), isAmlsBodyStillTheSame = Some(true)))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(cya = false)(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "ACCA"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-registration-number"
    }

    "return 303 SEE_OTHER for CYA" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAmlsJourneySessionRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAmlsJourneySessionRepository.putSession(
        dataKey, ukAmlsJourney.copy(newAmlsBody = Some("Association of Certified Chartered Accountants"), isAmlsBodyStillTheSame = Some(true)))(*[Writes[UpdateAmlsJourney]], *[Request[Any]])returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(cya = true)(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "ACCA"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe routes.CheckYourAnswersController.showPage.url
    }

    "return 303 SEE_OTHER to /enter-registration-number after CYA when body has changed to HMRC" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAmlsJourneySessionRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(ukAmlsJourney))

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAmlsJourneySessionRepository.putSession(
        dataKey, ukAmlsJourney.copy(
          newAmlsBody = Some("HM Revenue and Customs (HMRC)"),
          isAmlsBodyStillTheSame = Some(false))
      )(*[Writes[UpdateAmlsJourney]], *[Request[Any]]
      ) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(cya = true)(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "HMRC"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe routes.EnterRegistrationNumberController.showPage(true).url
    }

    "return 303 SEE_OTHER to /check-your-answers after CYA when body is still HMRC" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAmlsJourneySessionRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]
      ) returns Future.successful(Some(ukAmlsJourney.copy(newAmlsBody = Some("HMRC"))))

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAmlsJourneySessionRepository.putSession(
        dataKey, ukAmlsJourney.copy(
          newAmlsBody = Some("HM Revenue and Customs (HMRC)"),
          isAmlsBodyStillTheSame = Some(true))
      )(*[Writes[UpdateAmlsJourney]], *[Request[Any]]
      ) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(cya = true)(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "HMRC"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe routes.CheckYourAnswersController.showPage.url
    }

    "return 303 SEE_OTHER to /enter-registration-number for overseas agent" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAmlsJourneySessionRepository.getFromSession(dataKey)(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful(Some(overseasAmlsJourney))

      mockAmlsJourneySessionRepository.putSession(
        dataKey, overseasAmlsJourney.copy(newAmlsBody = Some("OS AMLS")))(*[Writes[UpdateAmlsJourney]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(cya = false)(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> "OS AMLS"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"
    }

    "return BadRequest when invalid form submission" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockAmlsLoader.load(*[String]) returns amlsBodies

      mockAmlsJourneySessionRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[Any]]) returns
        Future.successful(Some(ukAmlsJourney))

      mockView.apply(*[Form[String]], *[Map[String, String]], isUk = true, *[Boolean])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(cya = false)(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> ""))

      status(result) mustBe BAD_REQUEST
    }
  }

}
