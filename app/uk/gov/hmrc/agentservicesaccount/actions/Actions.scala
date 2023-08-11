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

package uk.gov.hmrc.agentservicesaccount.actions

import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, ActionFilter, AnyContent, DefaultActionBuilder, Request, Result}
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Actions @Inject()(  agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                 authActions: AuthActions,
                 actionBuilder:DefaultActionBuilder
              ) (implicit ec: ExecutionContext ) {

  def filterSuspendedAgent: ActionFilter[AuthRequestWithAgentInfo] = new ActionFilter[AuthRequestWithAgentInfo] {
    def executionContext: ExecutionContext = ec
    def filter[A](request: AuthRequestWithAgentInfo[A]): Future[Option[Result]] = {


      implicit val req: Request[A] = request.request

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(req, req.session)

      agentClientAuthorisationConnector.getSuspensionDetails().map { suspensionDetails =>
        if (!suspensionDetails.suspensionStatus)      None
        else
         Some( Redirect(routes.SuspendedJourneyController.showSuspendedWarning()))
      }


    }



  }
  def authActionCheckSuspend: ActionBuilder[AuthRequestWithAgentInfo, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen filterSuspendedAgent

  def authAction: ActionBuilder[AuthRequestWithAgentInfo, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner
}
