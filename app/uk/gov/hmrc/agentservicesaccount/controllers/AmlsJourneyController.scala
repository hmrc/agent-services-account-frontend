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

package uk.gov.hmrc.agentservicesaccount.controllers

import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.services.{EmailService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.ALMS.{update_confirmation_recevied}

import javax.inject.Inject

class AmlsJourneyController @Inject() (
                                               actions: Actions,
                                               emailService: EmailService,
                                               agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                                               updateConfirmationRecevied: update_confirmation_recevied,
                                               cacheService: SessionCacheService
                                             )(
                                               implicit val appConfig: AppConfig,
                                               val cc: MessagesControllerComponents,
                                               messagesApi: MessagesApi
                                             ) extends AgentServicesBaseController with Logging {

    def showUpdatedAmlsConfirmationPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
      Ok(updateConfirmationRecevied()).toFuture
    }

  }

