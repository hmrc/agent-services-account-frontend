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
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.ContactDetailsSuspendForm
import uk.gov.hmrc.agentservicesaccount.models.SuspendContactDetails
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.{contact_details, suspension_warning}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SuspendedJourneyController @Inject() (
                                             actions: Actions,
                                             suspensionWarningView: suspension_warning,
                                             contactDetailsView: contact_details,
                                             cacheService: SessionCacheService
                                           )(
                                             implicit val appConfig: AppConfig,
                                             val cc: MessagesControllerComponents,
                                             ec: ExecutionContext,
                                             messagesApi: MessagesApi
                                           ) extends AgentServicesBaseController with Logging {

  def showSuspendedWarning: Action[AnyContent] = actions.authActionOnlyForSuspended { implicit request =>
    Ok(suspensionWarningView(request.agentInfo.isAdmin))
  }

  def showContactDetails: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>

    cacheService.getSessionItems().flatMap(answers => {
      val contactForm = ContactDetailsSuspendForm.form.fill(
        SuspendContactDetails(
          answers(1).getOrElse(""),
          answers(2).getOrElse(""),
          answers(3).getOrElse(""),
          answers(4)
        )
      )
      contactDetailsView(contactForm).toFuture.map(Ok(_))
    })
  }

  def submitContactDetails: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    ContactDetailsSuspendForm.form.bindFromRequest().fold(
      formWithErrors => {
        contactDetailsView(formWithErrors).toFuture.map(BadRequest(_))
      },
      formData => {
        for {
          _ <- cacheService.put(NAME, formData.name)
          _ <- cacheService.put(EMAIL, formData.email)
          _ <- cacheService.put(PHONE, formData.phone)
          _ <- cacheService.put(UTR, formData.utr.getOrElse(""))
          _ <- cacheService.put(ARN, request.agentInfo.arn.value)
        } yield Redirect("")
      }
    )
  }
}