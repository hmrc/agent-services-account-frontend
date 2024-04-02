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
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.models.{AmlsStatus, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.check_your_answers
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import java.time.LocalDate
import java.util.Locale
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersControllerSpec extends PlaySpec with IdiomaticMockito with ArgumentMatchersSugar {
  implicit val ec: ExecutionContext = ExecutionContext.global
  private val fakeRequest = FakeRequest().withCookies(Cookie("PLAY_LANG", "en_GB")).withTransientLang(Locale.UK)

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
  private val invalidAuthResponse: Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[Assistant.type]] =
    Future.successful(new~(new~(new~(new~(
      Enrolments(agentEnrolment), Some(ggCredentials)),
      Some(Email("test@email.com"))),
      Some(Name(Some("Troy"), Some("Barnes")))),
      Some(Assistant)))
  private val invalidCredentialAuthResponse: Future[Enrolments ~ None.type ~ Some[Email] ~ Some[Name] ~ Some[User.type]] =
    Future.successful(new~(new~(new~(new~(
      Enrolments(agentEnrolment), None),
      Some(Email("test@email.com"))),
      Some(Name(Some("Troy"), Some("Barnes")))),
      Some(credentialRole)))

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatus.ValidAmlsDetailsUK,
    newAmlsBody = Some("ABC"),
    newRegistrationNumber = Some("1234567890"),
    newExpirationDate = Some(LocalDate.now())
  )

  private val suspensionDetailsResponse: Future[SuspensionDetails] = Future.successful(SuspensionDetails(suspensionStatus = false, None))


  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockAmlsLoader: AMLSLoader = mock[AMLSLoader]
    protected val mockAgentClientAuthorisationConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val actionBuilder = new DefaultActionBuilderImpl(stubBodyParser())
    protected val mockActions =
      new Actions(mockAgentClientAuthorisationConnector, mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockUpdateAmlsJourneyRepository: UpdateAmlsJourneyRepository = mock[UpdateAmlsJourneyRepository]
    protected val mockView: check_your_answers = mock[check_your_answers]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey: DataKey[UpdateAmlsJourney] = DataKey[UpdateAmlsJourney]("amlsJourney")

    mockAmlsLoader.load(*[String]) returns Map("ABC" -> "Alphabet")

    object TestController extends CheckYourAnswersController(
      mockAmlsLoader,mockActions, mockAgentAssuranceConnector ,mockUpdateAmlsJourneyRepository, mockView, cc)(mockAppConfig, ec)
  }

  "ShowPage" should {
    "render view" when {
      "agent has successfully entered all the data for CYA page" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse
        mockAppConfig.enableNonHmrcSupervisoryBody returns true
        mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse
        mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
          Future.successful(Some(ukAmlsJourney))

        mockView.apply(*[Seq[SummaryListData]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.showPage()(fakeRequest)
        status(result) mustBe OK

      }
    }
    "Redirect to manage account page" when {
      "There's no update amls journey in session" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse
        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse
        mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
          Future.successful(None)

        mockView.apply(*[Seq[SummaryListData]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

        val result: Future[Result] = TestController.showPage(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(uk.gov.hmrc.agentservicesaccount.controllers.routes.AgentServicesController.manageAccount.url)
      }
    }
    "Returns forbidden" when {
      "feature flag is turned off" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns authResponse
        mockAppConfig.enableNonHmrcSupervisoryBody returns false
        mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse

        val result: Future[Result] = TestController.showPage(fakeRequest)
        status(result) mustBe FORBIDDEN

      }
      "the user has the incorrect credential roles" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns invalidAuthResponse
        mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse

        val result: Future[Result] = TestController.showPage(fakeRequest)
        status(result) mustBe FORBIDDEN
      }
      "the user has the incorrect provider type" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]) returns invalidCredentialAuthResponse
        mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse

        val result: Future[Result] = TestController.showPage(fakeRequest)
        status(result) mustBe FORBIDDEN
      }

    }
  }

  "buildSummaryListItems" when {

    val supervisoryBodyMessageKey = "amls.check-your-answers.supervisory-body"
    val registrationNumberMessageKey = "amls.check-your-answers.registration-number"

    val registrationNumber = "1234567890"
    val supervisoryBody = "ABC"
    val supervisoryBodyDescription = "Alphabet"

    "expected journey data is missing" should {
      val journey = UpdateAmlsJourney(
        status = AmlsStatus.ValidAmlsNonUK,
        newAmlsBody = None,
        newRegistrationNumber = Some(registrationNumber),
        newExpirationDate = None
      )

      "throw an exception" in new Setup {
        val expectedException: Exception = intercept[Exception] {
          TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        }
        expectedException.getMessage mustBe "Expected AMLS journey data missing"
      }
    }
    "journey data is for an overseas agent" should {
      val journey = UpdateAmlsJourney(
        status = AmlsStatus.ValidAmlsNonUK,
        newAmlsBody = Some(supervisoryBody),
        newRegistrationNumber = Some(registrationNumber),
        newExpirationDate = None
      )

      "build a summary list with two items" in new Setup {
        TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK).length mustBe 2
      }

      "render the correct message key for supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        data.head.key mustBe supervisoryBodyMessageKey
      }
      "render the correct URL to change the supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        data.head.link mustBe Some(routes.AmlsNewSupervisoryBodyController.showPage(true))
      }
      "render the correct supervisory body entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        data.head.value mustBe supervisoryBody
      }

      "render the correct message key for registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        data(1).key mustBe registrationNumberMessageKey
      }
      "render the correct URL to change the registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        data(1).link mustBe Some(routes.EnterRegistrationNumberController.showPage(true))
      }
      "render the correct renewal date entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = false, journey, Locale.UK)
        data(1).value mustBe registrationNumber
      }
    }

    "onSubmit" should {
      "Submit answers" when {
        "agent has successfully entered all the data for CYA page" in new Setup {

          mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
            *[HeaderCarrier],
            *[ExecutionContext]) returns authResponse
          mockAppConfig.enableNonHmrcSupervisoryBody returns true
          mockAgentClientAuthorisationConnector.getSuspensionDetails()(*[HeaderCarrier], *[ExecutionContext]) returns suspensionDetailsResponse
          mockAgentAssuranceConnector.postAmlsDetails(arn)(*[ExecutionContext], *[HeaderCarrier]) returns
            Future.successful(AmlsStatus.ValidAmlsDetailsUK)
          mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[Request[_]]) returns
            Future.successful(Some(ukAmlsJourney))


          val result: Future[Result] = TestController.onSubmit()(fakeRequest)
          status(result) mustBe SEE_OTHER

        }
      }
    }

    "journey data is for an UK agent" should {
      val renewalDateMessageKey = "amls.check-your-answers.renewal-date"
      val renewalDate = LocalDate.of(2001, 1, 1)
      val journey = UpdateAmlsJourney(
        status = AmlsStatus.ValidAmlsDetailsUK,
        newAmlsBody = Some(supervisoryBody),
        newRegistrationNumber = Some(registrationNumber),
        newExpirationDate = Some(renewalDate)
      )

      "build a summary list with three items" in new Setup {
        TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK).length mustBe 3
      }

      "render the correct message key for supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data.head.key mustBe supervisoryBodyMessageKey
      }
      "render the correct URL to change the supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data.head.link mustBe Some(routes.AmlsNewSupervisoryBodyController.showPage(true))
      }
      "render the supervisory body description entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data.head.value mustBe supervisoryBodyDescription
      }

      "render the correct message key for registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data(1).key mustBe registrationNumberMessageKey
      }
      "render the correct URL to change the registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data(1).link mustBe Some(routes.EnterRegistrationNumberController.showPage(true))
      }
      "render the registration number entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data(1).value mustBe registrationNumber
      }

      "render the correct message key for renewal date" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data(2).key mustBe renewalDateMessageKey
      }
      "render the correct URL to change the renewal date" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data(2).link mustBe Some(routes.EnterRenewalDateController.showPage)
      }
      "render the renewal date entered by the user in long [English] format" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, Locale.UK)
        data(2).value mustBe "1 January 2001"
      }
      "render the renewal date entered by the user in long [Welsh] format" in new Setup {
        private val data = TestController.buildSummaryListItems(isUkAgent = true, journey, new Locale("cy"))
        data(2).value mustBe "1 Ionawr 2001"
      }
    }
  }
}
