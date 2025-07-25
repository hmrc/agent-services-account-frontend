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
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.supervision_details
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext

class AMLSDetailsController @Inject() (
  agentAssuranceConnector: AgentAssuranceConnector,
  actions: Actions,
  supervision_details: supervision_details
)(implicit
  val appConfig: AppConfig,
  ec: ExecutionContext,
  val cc: MessagesControllerComponents
)
extends FrontendController(cc)
with I18nSupport {

  val showSupervisionDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      agentAssuranceConnector.getAMLSDetails(request.agentInfo.arn.value).map(amlsDetails =>
        Ok(supervision_details(amlsDetails))
      )
    }
  }

}
