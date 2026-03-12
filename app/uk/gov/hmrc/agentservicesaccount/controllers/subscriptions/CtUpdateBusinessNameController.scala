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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.controllers.{routes => homeRoutes}
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CtUpdateBusinessNameController @Inject()(
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  //  TODO: 10902 Only here to allow CtUpdateBusinessNameController to compile
  draftDetailsService: DraftDetailsService,
  ct_update_business_name: ct_update_business_name,
  cc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
extends FrontendController(cc)
with CtJourneySupport
with I18nSupport
with Logging {

  val showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    Future.successful(Ok(ct_update_business_name(UpdateDetailsForms.businessNameForm)))
  }

  val onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    UpdateDetailsForms.businessNameForm
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(ct_update_business_name(formWithErrors))),
        newBusinessName => {
//            draftDetailsService.updateDraftDetails(ctDetails =>
//              ctDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyName = Some(newBusinessName)))
//            )
          draftDetailsService.dummyCtMethod()
        .flatMap {
            _ =>
//              isJourneyComplete().flatMap(journeyComplete => Future.successful(getNextPage(journeyComplete, "businessName")))
          //  TODO: 10902 Temp value here
          case _ => Future.successful(Redirect(homeRoutes.AgentServicesController.root()))
          }
        }
      )
  }

}
