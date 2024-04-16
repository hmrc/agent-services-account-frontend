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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util

import play.api.mvc.Results.{NotFound, Redirect}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentservicesaccount.actions.AuthRequestWithAgentInfo
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.{DRAFT_NEW_CONTACT_DETAILS, desiDetails, routes}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import scala.concurrent.{ExecutionContext, Future}

trait DesiDetailsJourneySupport {

  def sessionCache: SessionCacheService

  def withUpdateDesiDetailsJourney(body: DesignatoryDetails => Future[Result])(
    implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS).flatMap {
      case Some(desiDetails: DesignatoryDetails) => body(desiDetails)
      case None =>
        Future successful Redirect(routes.AgentServicesController.manageAccount)
    }
  }

  def ifChangeContactDetailsFeatureEnabled(action: => Future[Result])(implicit appConfig: AppConfig): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  def ifChangeContactFeatureEnabledAndNoPendingChanges(action: => Future[Result])
                                                      (implicit request: AuthRequestWithAgentInfo[_],
                                                       appConfig: AppConfig,
                                                       pcodRepository: PendingChangeRequestRepository,
                                                       ec: ExecutionContext): Future[Result] =
    ifChangeContactDetailsFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
      }
    }

}
