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

import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls_details._
import uk.gov.hmrc.agentservicesaccount.actions.Actions


import javax.inject._
import scala.concurrent.ExecutionContext

class AMLSDetailsController @Inject()(agentAssuranceConnector: AgentAssuranceConnector,
                                      actions:Actions,
                                      suspension_details: suspension_details)(implicit val appConfig: AppConfig,
                                                                              ec: ExecutionContext,
                                                                              val cc: MessagesControllerComponents,
                                                                              messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {

  val showSupervisionDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    agentAssuranceConnector.getAMLSDetails(request.agentInfo.arn.value).map(amlsDetails =>
      Ok(suspension_details(amlsDetails))
    )
  }

}
