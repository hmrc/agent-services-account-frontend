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

import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class ViewDetailsController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  agentAssuranceConnector: AgentAssuranceConnector,
  viewDetails: view_details,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with AmlsJourneySupport
with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      agentAssuranceConnector.getAMLSDetailsResponse(request.agentInfo.arn.value).flatMap { amlsDetailsResponse =>
        saveAmlsJourney(UpdateAmlsJourney(status = amlsDetailsResponse.status)).map { _ =>
          amlsDetailsResponse.details match {
            case details @ Some(_) => Ok(viewDetails(amlsDetailsResponse.status, details))
            case None => Ok(viewDetails(amlsDetailsResponse.status, None))
          }
        }
      }
    }
  }

}
