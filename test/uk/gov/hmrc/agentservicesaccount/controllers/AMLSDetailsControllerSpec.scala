/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers

import org.scalatestplus.play.PlaySpec
import play.api.test.DefaultAwaitTimeout
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import play.api.Environment
import play.api.i18n.MessagesApi
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, DefaultActionBuilderImpl}
import play.api.test.Helpers.stubMessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls_details.suspension_details
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext

class AMLSDetailsControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  trait Setup {
    protected val mockAppConfig = mock[AppConfig]
    protected val mockAuthConnector = mock[AuthConnector]
    protected val mockEnvironment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockAgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val mockActions =
      new Actions(mockAgentClientAuthorisationConnector, authActions, )

    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockView = mock[suspension_details]
    protected val cc = stubMessagesControllerComponents()
    protected val mockMessagesApi = mock[MessagesApi]

    object TestController extends AMLSDetailsController(mockAgentAssuranceConnector, mockActions, mockView)(mockAppConfig, ec, cc, mockMessagesApi)
  }

  "AMLSDetailsController.showSupervisionDetails" should {
    "display the page if AMLS suspension details were successfully retrieved from agent-assurance" in new Setup {

      val result = TestController.showSupervisionDetails
      result mustBe Ok
    }
  }

}
