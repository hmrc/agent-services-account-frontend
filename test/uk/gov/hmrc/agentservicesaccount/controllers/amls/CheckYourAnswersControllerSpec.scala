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
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.services.AuditService
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.check_your_answers
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.cache.DataKey

import java.time.LocalDate
import java.util.Locale
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CheckYourAnswersControllerSpec
extends PlaySpec
with IdiomaticMockito
with ArgumentMatchersSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val fakeRequest = FakeRequest().withCookies(Cookie("PLAY_LANG", "en_GB")).withTransientLang(Locale.UK)

  private val arn: Arn = Arn("arn")
  private val amlsRequest: AmlsRequest =
    new AmlsRequest(
      ukRecord = true,
      supervisoryBody = "ABC",
      membershipNumber = "1234567890",
      membershipExpiresOn = Some(LocalDate.now())
    )
  private val credentialRole: User.type = User
  private val agentEnrolment: Set[Enrolment] = Set(
    Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None
    )
  )
  private val ggCredentials: Credentials = Credentials("ggId", "GovernmentGateway")
  private val authResponse: Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[User.type]] = Future.successful(new ~(
    new ~(
      new ~(
        new ~(
          Enrolments(agentEnrolment),
          Some(ggCredentials)
        ),
        Some(Email("test@email.com"))
      ),
      Some(Name(Some("Troy"), Some("Barnes")))
    ),
    Some(credentialRole)
  ))
  private val invalidAuthResponse: Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[Assistant.type]] = Future.successful(new ~(
    new ~(
      new ~(
        new ~(
          Enrolments(agentEnrolment),
          Some(ggCredentials)
        ),
        Some(Email("test@email.com"))
      ),
      Some(Name(Some("Troy"), Some("Barnes")))
    ),
    Some(Assistant)
  ))
  private val invalidCredentialAuthResponse: Future[Enrolments ~ None.type ~ Some[Email] ~ Some[Name] ~ Some[User.type]] = Future.successful(new ~(
    new ~(
      new ~(
        new ~(
          Enrolments(agentEnrolment),
          None
        ),
        Some(Email("test@email.com"))
      ),
      Some(Name(Some("Troy"), Some("Barnes")))
    ),
    Some(credentialRole)
  ))

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsDetailsUK,
    newAmlsBody = Some("ABC"),
    newRegistrationNumber = Some("1234567890"),
    newExpirationDate = Some(LocalDate.now())
  )

  val agentDetails = AgencyDetails(
    Some("My Agency"),
    Some("abc@abc.com"),
    Some("07345678901"),
    Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      "GB"
    ))
  )

  val amlsDetails = AmlsDetails(
    "supervisoryBody",
    Some("membershipNumber"),
    None,
    None,
    None,
    Some(LocalDate.now())
  )

  val suspensionDetails = SuspensionDetails(suspensionStatus = false, None)

  val agentRecord = AgentDetailsDesResponse(
    uniqueTaxReference = Some(Utr("0123456789")),
    agencyDetails = Some(agentDetails),
    suspensionDetails = Some(suspensionDetails)
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

    protected val mockAmlsLoader: AMLSLoader = mock[AMLSLoader]
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val actionBuilder = new DefaultActionBuilderImpl(stubBodyParser())
    protected val mockActions =
      new Actions(
        mockAgentAssuranceConnector,
        authActions,
        actionBuilder
      )

    protected val mockUpdateAmlsJourneyRepository: UpdateAmlsJourneyRepository = mock[UpdateAmlsJourneyRepository]
    protected val mockView: check_your_answers = mock[check_your_answers]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val dataKey: DataKey[UpdateAmlsJourney] = DataKey[UpdateAmlsJourney]("amlsJourney")
    protected val mockAuditService = mock[AuditService]
    protected val mockAaConnector = mock[AgentAssuranceConnector]

    object TestController
    extends CheckYourAnswersController(
      mockActions,
      mockAgentAssuranceConnector,
      mockUpdateAmlsJourneyRepository,
      mockView,
      cc,
      mockAuditService
    )(mockAppConfig, ec)

  }

  "ShowPage" should {
    "render view" when {
      "agent has successfully entered all the data for CYA page" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns authResponse
        mockAppConfig.enableNonHmrcSupervisoryBody returns true
        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)
        mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns
          Future.successful(Some(ukAmlsJourney))

        mockView.apply(*[Seq[SummaryListData]])(
          *[Messages],
          *[RequestHeader],
          *[AppConfig]
        ) returns Html("")

        val result: Future[Result] = TestController.showPage()(fakeRequest)
        status(result) mustBe OK

      }
    }
    "Redirect to manage account page" when {
      "There's no update amls journey in session" in new Setup {

        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns authResponse
        mockAppConfig.enableNonHmrcSupervisoryBody returns true

        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)
        mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns
          Future.successful(None)

        mockView.apply(*[Seq[SummaryListData]])(
          *[Messages],
          *[RequestHeader],
          *[AppConfig]
        ) returns Html("")

        val result: Future[Result] = TestController.showPage(fakeRequest)

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(uk.gov.hmrc.agentservicesaccount.controllers.routes.AgentServicesController.manageAccount.url)
      }
    }
    "Returns forbidden" when {
      "feature flag is turned off" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns authResponse
        mockAppConfig.enableNonHmrcSupervisoryBody returns false
        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

        val result: Future[Result] = TestController.showPage(fakeRequest)
        status(result) mustBe FORBIDDEN

      }
      "the user has the incorrect credential roles" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns invalidAuthResponse
        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

        val result: Future[Result] = TestController.showPage(fakeRequest)
        status(result) mustBe FORBIDDEN
      }
      "the user has the incorrect provider type" in new Setup {
        mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
          *[HeaderCarrier],
          *[ExecutionContext]
        ) returns invalidCredentialAuthResponse
        mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

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
        status = AmlsStatuses.ValidAmlsNonUK,
        newAmlsBody = None,
        newRegistrationNumber = Some(registrationNumber),
        newExpirationDate = None
      )

      "throw an exception" in new Setup {
        val expectedException: Exception = intercept[Exception] {
          TestController.buildSummaryListItems(
            isUkAgent = false,
            journey,
            Locale.UK
          )
        }
        expectedException.getMessage mustBe "Expected AMLS journey data missing"
      }
    }

    "journey data is for an overseas agent" should {
      val journey = UpdateAmlsJourney(
        status = AmlsStatuses.ValidAmlsNonUK,
        newAmlsBody = Some(supervisoryBody),
        newRegistrationNumber = Some(registrationNumber),
        newExpirationDate = None
      )

      "build a summary list with two items" in new Setup {
        TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        ).length mustBe 2
      }

      "render the correct message key for supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        )
        data.head.key mustBe supervisoryBodyMessageKey
      }
      "render the correct URL to change the supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        )
        data.head.link mustBe Some(routes.AmlsNewSupervisoryBodyController.showPage(true))
      }
      "render the correct supervisory body entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        )
        data.head.value mustBe supervisoryBody
      }

      "render the correct message key for registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        )
        data(1).key mustBe registrationNumberMessageKey
      }
      "render the correct URL to change the registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        )
        data(1).link mustBe Some(routes.EnterRegistrationNumberController.showPage(true))
      }
      "render the correct renewal date entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = false,
          journey,
          Locale.UK
        )
        data(1).value mustBe registrationNumber
      }
    }

    "onSubmit" should {
      "Submit answers" when {
        "agent has successfully entered all the data for CYA page" in new Setup {

          mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
            *[HeaderCarrier],
            *[ExecutionContext]
          ) returns authResponse
          mockAppConfig.enableNonHmrcSupervisoryBody returns true
          mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)
          mockUpdateAmlsJourneyRepository
            .getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns Future.successful(Some(ukAmlsJourney))
          mockAgentAssuranceConnector.postAmlsDetails(arn, amlsRequest)(*[RequestHeader]) returns Future.successful(())

          mockAgentAssuranceConnector.getAMLSDetails(arn.value)(*[RequestHeader]) returns Future.successful(amlsDetails)
          mockAaConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

          val result: Future[Result] = TestController.onSubmit()(fakeRequest)
          status(result) mustBe SEE_OTHER

        }

        "agent has successfully entered all the data for CYA page with Audit data gathering exceptions " in new Setup {

          mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
            *[HeaderCarrier],
            *[ExecutionContext]
          ) returns authResponse
          mockAppConfig.enableNonHmrcSupervisoryBody returns true
          mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)
          mockUpdateAmlsJourneyRepository
            .getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns Future.successful(Some(ukAmlsJourney))
          mockAgentAssuranceConnector.postAmlsDetails(arn, amlsRequest)(*[RequestHeader]) returns Future.successful(())

          mockAgentAssuranceConnector.getAMLSDetails(arn.value)(*[RequestHeader]).throws(UpstreamErrorResponse("Something went wrong", 500))
          // mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]).throws(UpstreamErrorResponse("Something went wrong",500))

          val result: Future[Result] = TestController.onSubmit()(fakeRequest)
          status(result) mustBe SEE_OTHER

        }

        "agent has unsuccessfully entered data for CYA page" in new Setup {

          mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
            *[HeaderCarrier],
            *[ExecutionContext]
          ) returns authResponse
          mockAppConfig.enableNonHmrcSupervisoryBody returns true
          mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)
          mockUpdateAmlsJourneyRepository.getFromSession(*[DataKey[UpdateAmlsJourney]])(*[Reads[UpdateAmlsJourney]], *[RequestHeader]) returns
            Future.successful(Some(UpdateAmlsJourney(
              AmlsStatuses.ValidAmlsDetailsUK,
              None,
              None,
              None,
              None
            )))

          mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

          val result: Future[Result] = TestController.onSubmit()(fakeRequest)
          status(result) mustBe BAD_REQUEST

        }
      }
    }

    "journey data is for an UK agent" should {
      val renewalDateMessageKey = "amls.check-your-answers.renewal-date"
      val renewalDate = LocalDate.of(2001, 1, 1)
      val journey = UpdateAmlsJourney(
        status = AmlsStatuses.ValidAmlsDetailsUK,
        newAmlsBody = Some(supervisoryBodyDescription),
        newRegistrationNumber = Some(registrationNumber),
        newExpirationDate = Some(renewalDate)
      )

      "build a summary list with three items" in new Setup {
        TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        ).length mustBe 3
      }

      "render the correct message key for supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data.head.key mustBe supervisoryBodyMessageKey
      }
      "render the correct URL to change the supervisory body" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data.head.link mustBe Some(routes.AmlsNewSupervisoryBodyController.showPage(true))
      }
      "render the supervisory body description entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data.head.value mustBe supervisoryBodyDescription
      }

      "render the correct message key for registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data(1).key mustBe registrationNumberMessageKey
      }
      "render the correct URL to change the registration number" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data(1).link mustBe Some(routes.EnterRegistrationNumberController.showPage(true))
      }
      "render the registration number entered by the user" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data(1).value mustBe registrationNumber
      }

      "render the correct message key for renewal date" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data(2).key mustBe renewalDateMessageKey
      }
      "render the correct URL to change the renewal date" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data(2).link mustBe Some(routes.EnterRenewalDateController.showPage)
      }
      "render the renewal date entered by the user in long [English] format" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          Locale.UK
        )
        data(2).value mustBe "1 January 2001"
      }
      "render the renewal date entered by the user in long [Welsh] format" in new Setup {
        private val data = TestController.buildSummaryListItems(
          isUkAgent = true,
          journey,
          new Locale("cy")
        )
        data(2).value mustBe "1 Ionawr 2001"
      }
    }
  }

}
