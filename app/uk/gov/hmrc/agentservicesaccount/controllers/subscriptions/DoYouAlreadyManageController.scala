/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.DoYouAlreadyManageForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.DoYouAlreadyManageFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.do_you_already_manage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class DoYouAlreadyManageController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  do_you_already_manage: do_you_already_manage,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport {

  def showPage(agentRegime: AgentRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val journey = request.subscriptionJourney
    val asaDetailsAgencyName = journey.asaDetails.agencyName.getOrElse("")

    val form =
      journey.doYouAlreadyManage match {
        case Some(value) => DoYouAlreadyManageForm.form(agentRegime, asaDetailsAgencyName).fill(DoYouAlreadyManageFormValues(value))
        case None => DoYouAlreadyManageForm.form(agentRegime, asaDetailsAgencyName)
      }

    Future.successful(Ok(do_you_already_manage(
      form,
      agentRegime,
      asaDetailsAgencyName
    )))
  }

  def onSubmit(agentRegime: AgentRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val journey = request.subscriptionJourney
    val asaDetailsAgencyName = journey.asaDetails.agencyName.getOrElse("")

    DoYouAlreadyManageForm.form(agentRegime, asaDetailsAgencyName).bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(do_you_already_manage(
          formWithErrors,
          agentRegime,
          asaDetailsAgencyName
        ))),
      answer => {
        val updatedJourney = journey.copy(
          doYouAlreadyManage = Some(answer.doYouAlreadyManage)
        )

        val nextPage =
          if (answer.doYouAlreadyManage)
            routes.YouMayNotNeedToApplyController.showPage(agentRegime)
          else {
            agentRegime match {
              case PAYE => routes.PayeUpdateContactNameController.showPage
              case _ => routes.UpdateBusinessNameController.showPage(agentRegime)
            }
          }

        sessionCacheService
          .put(subscriptionJourneyKey(agentRegime), updatedJourney)
          .map(_ => Redirect(nextPage))
      }
    )
  }

}
