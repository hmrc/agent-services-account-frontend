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
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentservicesaccount.forms.{BetaInviteContactDetailsForm, BetaInviteForm, YesNoForm}
import uk.gov.hmrc.agentservicesaccount.models.{AgentSize, BetaInviteContactDetails, BetaInviteDetailsForEmail}
import uk.gov.hmrc.agentservicesaccount.services.{EmailService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.beta_invite._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class BetaInviteController @Inject()(actions: Actions,
                                     agentPermissionsConnector: AgentPermissionsConnector,
                                     emailService: EmailService,
                                     cacheService: SessionCacheService,
                                     participate: participate,
                                     number_of_clients: number_of_clients,
                                     your_details: your_details,
                                     check_answers: check_answers,
                                     confirmation: confirmation
                                    )(implicit appConfig: AppConfig,
                                      cc: MessagesControllerComponents,
                                      ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with Logging {


  private val controller: ReverseBetaInviteController = routes.BetaInviteController

  val hideInvite: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    agentPermissionsConnector.declinePrivateBetaInvite().map(_ =>
      Redirect(routes.AgentServicesController.showAgentServicesAccount())
    )
  }

  val showInvite: Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>

    Ok(participate(
      YesNoForm.form("beta.invite.yes-no.required.error")
    ))
  }

  def submitInvite(): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>

    YesNoForm
      .form("beta.invite.yes-no.required.error")
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Ok(participate(formWithErrors)).toFuture
        },
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

  val showInviteDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>

    cacheService.get(AGENT_SIZE).map(maybeAnswer => {
      val sizeForm = BetaInviteForm.form.fill(maybeAnswer.getOrElse(""))
      Ok(number_of_clients(sizeForm))
    })

  }

  def submitInviteDetails(): Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>

    BetaInviteForm
      .form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Ok(number_of_clients(formWithErrors))
        },
        formData => {
          cacheService.put(AGENT_SIZE, formData)
          Redirect(controller.showInviteContactDetails)
        }
      )

  }


  val showInviteContactDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>

    cacheService.getSessionItems().map(answers => {
      val contactForm = BetaInviteContactDetailsForm.form.fill(
        BetaInviteContactDetails(
          answers(1).getOrElse(""),
          answers(2).getOrElse(""),
          answers(3)
        )
      )
      Ok(your_details(contactForm))
    })
  }


  def submitInviteContactDetails(): Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>

    BetaInviteContactDetailsForm
      .form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Ok(your_details(formWithErrors))
        },
        formData => {
          cacheService.put(NAME, formData.name)
          cacheService.put(EMAIL, formData.email)
          cacheService.put(PHONE, formData.phone.getOrElse(""))
          logger.info(s"data to be saved: $formData")
          Redirect(controller.showInviteCheckYourAnswers)
        }
      )

  }

  val showInviteCheckYourAnswers: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    cacheService.getSessionItems().flatMap(answers => {
      val detailsForEmail: BetaInviteDetailsForEmail = BetaInviteDetailsForEmail(
        AgentSize(answers.head.getOrElse("")),
        answers(1).getOrElse(""),
        answers(2).getOrElse(""),
        answers(3),
      )
      Ok(check_answers(detailsForEmail))
    }.toFuture)
  }

  def submitDetailsToEmail(): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>

    cacheService.getSessionItems().flatMap(answers => {
      val detailsForEmail: BetaInviteDetailsForEmail = BetaInviteDetailsForEmail(
        AgentSize(answers.head.getOrElse("")),
        answers(1).getOrElse(""),
        answers(2).getOrElse(""),
        answers(3),
      )
      emailService.sendInviteAcceptedEmail(request.agentInfo.arn, detailsForEmail).map(_ =>
        Redirect(controller.showInviteConfirmation)
      )
    })

  }

  val showInviteConfirmation: Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>
    Ok(confirmation())
  }


}
