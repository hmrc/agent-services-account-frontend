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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.DRAFT_NEW_CONTACT_DETAILS
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.view_contact_details
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ViewContactDetailsController @Inject() (
  actions: Actions,
  sessionCache: SessionCacheService,
  agentAssuranceConnector: AgentAssuranceConnector,
  pcodRepository: PendingChangeRequestRepository,
  view_contact_details: view_contact_details
)(implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport {

  private def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails)
      action
    else
      Future.successful(NotFound)
  }

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabled {
      for {
        _ <- sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)
        mPendingChange <- pcodRepository.find(request.agentInfo.arn)
        agencyDetails <- agentAssuranceConnector.getAgentRecord.map(_.agencyDetails.getOrElse {
          throw new RuntimeException(s"Could not retrieve current agency details for ${request.agentInfo.arn} from the backend")
        })
      } yield Ok(view_contact_details(
        agencyDetails,
        mPendingChange,
        request.agentInfo.isAdmin
      ))
    }
  }

}
