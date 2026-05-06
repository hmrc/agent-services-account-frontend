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
import uk.gov.hmrc.agentservicesaccount.controllers.{routes => homeRoutes}
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.YouMayNotNeedToApplyForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.YouMayNotNeedToApplyFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.you_may_not_need_to_apply
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class YouMayNotNeedToApplyController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  you_may_not_need_to_apply: you_may_not_need_to_apply,
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
      journey.youMayNotNeedToApply match {
        case Some(value) => YouMayNotNeedToApplyForm.form(legacyRegime).fill(YouMayNotNeedToApplyFormValues(value))
        case None => YouMayNotNeedToApplyForm.form(legacyRegime)
      }

    Future.successful(Ok(you_may_not_need_to_apply(
      form,
      legacyRegime
    )))
  }

  def onSubmit(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    YouMayNotNeedToApplyForm.form(legacyRegime).bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(you_may_not_need_to_apply(
          formWithErrors,
          legacyRegime
        ))),
      answer => {
        val updatedJourney = journey.copy(
          youMayNotNeedToApply = Some(answer.doYouStillWantToApply)
        )

        val nextPage =
          if (answer.doYouStillWantToApply)
            legacyRegime match {
              case PAYE => routes.PayeUpdateContactNameController.showPage
              case _ => routes.UpdateBusinessNameController.showPage(legacyRegime)
            }
          else {
            homeRoutes.AgentServicesController.root()
          }

        sessionCacheService
          .put(subscriptionJourneyKey(legacyRegime), updatedJourney)
          .map(_ => Redirect(nextPage))
      }
    )
  }

}
