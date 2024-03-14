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

import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc.{ActionBuilder, ActionFilter, AnyContent, DefaultActionBuilder, Request, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Actions @Inject()(  agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                          agentAssuranceConnector: AgentAssuranceConnector,
                 authActions: AuthActions,
                 actionBuilder:DefaultActionBuilder
              ) (implicit ec: ExecutionContext ) {

  private def filterSuspendedAgent(onlyForSuspended: Boolean): ActionFilter[AuthRequestWithAgentInfo] = new ActionFilter[AuthRequestWithAgentInfo] {
    def executionContext: ExecutionContext = ec
    def filter[A](request: AuthRequestWithAgentInfo[A]): Future[Option[Result]] = {
      implicit val req: Request[A] = request.request
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(req, req.session)
      agentClientAuthorisationConnector.getSuspensionDetails().map { suspensionDetails => {
        (onlyForSuspended, suspensionDetails.suspensionStatus) match {
          case (true, true) | (false, false) => None
          case (true, false) => Some(Redirect(routes.AgentServicesController.showAgentServicesAccount()))
          case (false, true) => Some(Redirect(routes.SuspendedJourneyController.showSuspendedWarning()))
        }
        }
      }
    }
  }
  def authActionCheckSuspend: ActionBuilder[AuthRequestWithAgentInfo, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen filterSuspendedAgent(false)

  def authActionOnlyForSuspended: ActionBuilder[AuthRequestWithAgentInfo, AnyContent] =
    actionBuilder andThen authActions.authActionRefiner andThen filterSuspendedAgent(true)

  def ifFeatureEnabled(feature: Boolean)(action: => Future[Result]): Future[Result] = {
    if (feature) action else Future.successful(Forbidden)
  }

  def withCurrentAmlsDetails(arn: Arn)(action: AmlsDetails => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    agentAssuranceConnector.getAMLSDetails(arn.value)
      .flatMap(amlsDetails => action(amlsDetails) )
  }
}
