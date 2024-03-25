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
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsStatus, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.view_details
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

class ViewDetailsControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar {


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  private val arn: Arn = Arn("arn")
  private val credentialRole: User.type = User
  private val agentEnrolment: Set[Enrolment] = Set(
    Enrolment("HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None))
  private val ggCredentials: Credentials =
    Credentials("ggId", "GovernmentGateway")

  private val authResponse: Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[User.type]] =
    Future.successful(new~(new~(new~(new~(
      Enrolments(agentEnrolment), Some(ggCredentials)),
      Some(Email("test@email.com"))),
      Some(Name(Some("Troy"), Some("Barnes")))),
      Some(credentialRole)))

  private val suspensionDetailsResponse: Future[SuspensionDetails] = Future.successful(SuspensionDetails(suspensionStatus = false, None))

  private val ukUpdateAmlsJourneyWithAmlsExists = UpdateAmlsJourney(status = AmlsStatus.ValidAmlsDetailsUK)
  private val ukUpdateAmlsJourneyWithoutAmls = UpdateAmlsJourney(status = AmlsStatus.NoAmlsDetailsUK)
  private val amlsDetails = AmlsDetails(supervisoryBody = "HMRC")

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
    protected val mockView: view_details = mock[view_details]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey = DataKey[UpdateAmlsJourney]("amlsJourney")

    object TestController extends ViewDetailsController(mockActions, mockUpdateAmlsJourneyRepository, mockView, cc)(mockAppConfig, ec)
  }

  "showPage" should {
    "display the page for the first time when AMLS details exist" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(ukUpdateAmlsJourneyWithAmlsExists))

      mockAgentAssuranceConnector.getAMLSDetails(*[String])(*[ExecutionContext], *[HeaderCarrier]) returns Future.successful(amlsDetails)

      mockView.apply(*[AmlsStatus], *[Option[AmlsDetails]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }

    "display the page for the first time when AMLS details do not exist" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse

      mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
        Future.successful(Some(ukUpdateAmlsJourneyWithoutAmls))

      mockView.apply(*[AmlsStatus], *[Option[AmlsDetails]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe OK
    }
  }
}
