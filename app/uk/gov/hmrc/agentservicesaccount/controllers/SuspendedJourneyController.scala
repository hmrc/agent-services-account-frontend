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
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.forms.ContactDetailsSuspendForm
import uk.gov.hmrc.agentservicesaccount.forms.SuspendDescriptionForm
import uk.gov.hmrc.agentservicesaccount.models.AccountRecoverySummary
import uk.gov.hmrc.agentservicesaccount.models.SuspendContactDetails
import uk.gov.hmrc.agentservicesaccount.services.EmailService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SuspendedJourneyController @Inject() (
  actions: Actions,
  emailService: EmailService,
  agentAssuranceConnector: AgentAssuranceConnector,
  suspensionWarningView: suspension_warning,
  contactDetailsView: contact_details,
  recoveryDescriptionView: recovery_description,
  checkRecoveryAnswersView: check_recovery_answers,
  confirmationReceived: confirmation_received,
  cacheService: SessionCacheService
)(implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  def showSuspendedWarning: Action[AnyContent] = actions.authActionOnlyForSuspended { implicit request =>
    Ok(suspensionWarningView())
  }

  def showContactDetails: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    cacheService.getBetaInviteSessionItems().flatMap(answers => {
      val contactForm = ContactDetailsSuspendForm.form.fill(
        SuspendContactDetails(
          answers(1).getOrElse(""),
          answers(2).getOrElse(""),
          answers(3).getOrElse("")
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
          _ <- cacheService.put(nameKey, formData.name)
          _ <- cacheService.put(emailKey, formData.email)
          _ <- cacheService.put(phoneKey, formData.phone)
          _ <- cacheService.put(arnKey, request.agentInfo.arn.value)
        } yield Redirect(routes.SuspendedJourneyController.showSuspendedDescription())
      }
    )
  }

  def showSuspendedDescription: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    cacheService.get(descriptionKey).flatMap {
      case Some(description) => Ok(recoveryDescriptionView(SuspendDescriptionForm.form.fill(description))).toFuture
      case _ => Ok(recoveryDescriptionView(SuspendDescriptionForm.form)).toFuture
    }
  }

  def submitSuspendedDescription: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    SuspendDescriptionForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          recoveryDescriptionView(formWithErrors).toFuture
            .map(BadRequest(_)),
        formData => {
          for {
            _ <- cacheService.put(descriptionKey, formData)
          } yield Redirect(routes.SuspendedJourneyController.showSuspendedSummary())
        }
      )
  }

  private def getSummaryDetails(implicit request: RequestHeader) = {
    for {
      name <- cacheService.get(nameKey)
      email <- cacheService.get(emailKey)
      phone <- cacheService.get(phoneKey)
      description <- cacheService.get(descriptionKey)
      arn <- cacheService.get(arnKey)
    } yield (name, email, phone, description, arn) match {
      case (Some(n), Some(e), Some(p), Some(d), Some(a)) => Option(AccountRecoverySummary(n, e, p, d, a))
      case _ => None
    }
  }

  def showSuspendedSummary: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    getSummaryDetails.flatMap {
      case Some(summaryDetails) => Ok(checkRecoveryAnswersView(summaryDetails)).toFuture
      case None => Redirect(routes.SuspendedJourneyController.showContactDetails()).toFuture
    }
  }

  def submitSuspendedSummary: Action[AnyContent] = actions.authActionForSuspendedAgentWithAgentRecord.async { implicit request =>
    getSummaryDetails.flatMap {
      case Some(summaryDetails) =>
        emailService.sendSuspendedSummaryEmail(summaryDetails, request.agentDetailsDesResponse.agencyDetails).flatMap(_ =>
          Redirect(routes.SuspendedJourneyController.showSuspendedConfirmation()).toFuture
        )
      case None => Redirect(routes.SuspendedJourneyController.showContactDetails()).toFuture
    }
  }

  def showSuspendedConfirmation: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    Ok(confirmationReceived()).toFuture
  }

}
