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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.BetaInviteContactDetailsForm
import uk.gov.hmrc.agentservicesaccount.models.BetaInviteContactDetails
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.{contact_details, suspension_warning}

import javax.inject.Inject
//import scala.concurrent.ExecutionContext

class SuspendedJourneyController @Inject()
(
  actions:Actions,
  suspensionWarningView: suspension_warning,
  contactDetails:contact_details,
  cacheService: SessionCacheService,

)(implicit val appConfig: AppConfig,
  val cc: MessagesControllerComponents,
//  ec: ExecutionContext,
  messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {

  val showSuspendedWarning: Action[AnyContent] = actions.authAction { implicit request =>
    Ok(suspensionWarningView(request.agentInfo.isAdmin))
  }

  val showContactDetails: Action[AnyContent] = actions.authAction { implicit request =>
    //todo change
    //ask about validation write tests for it etc..
    val contactForm = BetaInviteContactDetailsForm.form.fill(
      BetaInviteContactDetails(
        (""),
        (""),
        None
      )
    )
    Ok(contactDetails(contactForm))
  }

}