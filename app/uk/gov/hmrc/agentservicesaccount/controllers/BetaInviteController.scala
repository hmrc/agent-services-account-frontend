/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.auth.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentservicesaccount.forms.{BetaInviteContactDetailsForm, BetaInviteForm, YesNoForm}
import uk.gov.hmrc.agentservicesaccount.models.{AgentSize, BetaInviteContactDetails, BetaInviteDetailsForEmail}
import uk.gov.hmrc.agentservicesaccount.services.{EmailService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.beta_invite._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class BetaInviteController @Inject()
(
  authActions: AuthActions,
  agentPermissionsConnector: AgentPermissionsConnector,
  emailService: EmailService,
  cacheService: SessionCacheService,
  participate: participate,
  number_of_clients: number_of_clients,
  your_details: your_details,
  check_answers: check_answers,
  confirmation: confirmation
)(implicit val appConfig: AppConfig,
                  val cc: MessagesControllerComponents,
                  ec: ExecutionContext,
                  messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {

  import authActions._

  private val controller: ReverseBetaInviteController = routes.BetaInviteController

  val hideInvite: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      agentPermissionsConnector.declinePrivateBetaInvite().map(_ =>
        Redirect(routes.AgentServicesController.showAgentServicesAccount())
      )
    }
  }

  val showInvite: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Ok(participate(
        YesNoForm.form("beta.invite.yes-no.required.error")
      )).toFuture
    }
  }

  def submitInvite(): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      YesNoForm
        .form("beta.invite.yes-no.required.error")
        .bindFromRequest()
        .fold(
          formWithErrors => { Ok(participate(formWithErrors)).toFuture},
          (acceptInvite: Boolean) => {
            if (acceptInvite) {
              Redirect(controller.showInviteDetails).toFuture
            } else {
              agentPermissionsConnector.declinePrivateBetaInvite().map(_ =>
                Redirect(routes.AgentServicesController.showAgentServicesAccount())
              )
            }
          }
        )
    }
  }

  val showInviteDetails: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      cacheService.get(AGENT_SIZE).flatMap(maybeAnswer => {
        val sizeForm = BetaInviteForm.form.fill(maybeAnswer.getOrElse(""))
        Ok(number_of_clients(sizeForm)).toFuture
      })
    }
  }

  def submitInviteDetails(): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      BetaInviteForm
        .form
        .bindFromRequest()
        .fold(
          formWithErrors => { Ok(number_of_clients(formWithErrors)).toFuture},
          formData => {
            cacheService.put(AGENT_SIZE, formData)
            Redirect(controller.showInviteContactDetails).toFuture}
        )
    }
  }


  val showInviteContactDetails: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      cacheService.getBetaInviteSessionItems().flatMap(answers => {
        val contactForm = BetaInviteContactDetailsForm.form.fill(
          BetaInviteContactDetails(
            answers(1).getOrElse(""),
            answers(2).getOrElse(""),
            answers(3)
          )
        )
        Ok(your_details(contactForm)).toFuture
      })
    }
  }

  def submitInviteContactDetails(): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      BetaInviteContactDetailsForm
        .form
        .bindFromRequest()
        .fold(
          formWithErrors => { Ok(your_details(formWithErrors)).toFuture},
          formData => {
            cacheService.put(NAME, formData.name)
            cacheService.put(EMAIL, formData.email)
            cacheService.put(PHONE, formData.phone.getOrElse(""))
            logger.info(s"data to be saved: $formData")
            Redirect(controller.showInviteCheckYourAnswers).toFuture}
        )
    }
  }

  val showInviteCheckYourAnswers: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      cacheService.getBetaInviteSessionItems().flatMap(answers => {
        val detailsForEmail: BetaInviteDetailsForEmail = BetaInviteDetailsForEmail(
          AgentSize(answers.head.getOrElse("")),
          answers(1).getOrElse(""),
          answers(2).getOrElse(""),
          answers(3),
        )
        Ok(check_answers(detailsForEmail))
      }.toFuture)
    }
  }

  def submitDetailsToEmail(): Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      cacheService.getBetaInviteSessionItems().flatMap(answers => {
        val detailsForEmail: BetaInviteDetailsForEmail = BetaInviteDetailsForEmail(
          AgentSize(answers.head.getOrElse("")),
          answers(1).getOrElse(""),
          answers(2).getOrElse(""),
          answers(3),
        )
        emailService.sendInviteAcceptedEmail(agentInfo.arn, detailsForEmail).map(_ =>
          Redirect(controller.showInviteConfirmation)
        )
      })
    }
  }

  val showInviteConfirmation: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Ok(confirmation()).toFuture
    }
  }



}
