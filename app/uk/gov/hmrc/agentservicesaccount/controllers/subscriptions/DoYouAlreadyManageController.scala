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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
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

  def showPage(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val form =
      journey.doYouAlreadyManage match {
        case Some(value) => DoYouAlreadyManageForm.form(legacyRegime).fill(DoYouAlreadyManageFormValues(value))
        case None => DoYouAlreadyManageForm.form(legacyRegime)
      }

    Future.successful(Ok(do_you_already_manage(
      form,
      legacyRegime,
      journey.asaDetails.agencyName.getOrElse("")
    )))
  }

  def onSubmit(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    DoYouAlreadyManageForm.form(legacyRegime).bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(do_you_already_manage(
          formWithErrors,
          legacyRegime,
          journey.asaDetails.agencyName.getOrElse("")
        ))),
      answer => {
        val updatedJourney = journey.copy(
          doYouAlreadyManage = Some(answer.doYouAlreadyManage)
        )

        val nextPage =
          if (answer.doYouAlreadyManage)
            routes.UpdateBusinessNameController.showPage(legacyRegime) // TODO: APB-11185 change to new page
          else {
            legacyRegime match {
              case PAYE => routes.PayeUpdateContactNameController.showPage
              case _ => routes.UpdateBusinessNameController.showPage(legacyRegime)
            }
          }

        sessionCacheService
          .put(subscriptionJourneyKey(legacyRegime), updatedJourney)
          .map(_ => Redirect(nextPage))
      }
    )
  }

}
