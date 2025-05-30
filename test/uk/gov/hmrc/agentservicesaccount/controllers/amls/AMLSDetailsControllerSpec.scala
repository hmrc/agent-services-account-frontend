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
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, RequestHeader, Result}
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.supervision_details
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthConnector, Nino => _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AMLSDetailsControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()



  private val amlsDetails = AmlsDetails("HMRC")
  private val amlsDetailsResponse = Future.successful(amlsDetails)

  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockActions =
      new Actions(mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockView: supervision_details = mock[supervision_details]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController extends AMLSDetailsController(mockAgentAssuranceConnector, mockActions, mockView)(mockAppConfig, ec, cc)
  }

  "AMLSDetailsController.showSupervisionDetails" should {
    "return Ok and show the 'What's the name of your supervisory body?' page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockAgentAssuranceConnector.getAMLSDetails(arn.value)(*[RequestHeader]) returns amlsDetailsResponse

      mockView.apply(amlsDetails)(*[Messages], *[Request[Any]], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showSupervisionDetails(fakeRequest)

      status(result) mustBe OK
    }
  }

}
