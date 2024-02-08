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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.views.html.pages.admin_access_for_access_groups
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class NoAccessGroupsAssignmentController @Inject()(actions: Actions,
                                                   adminInfoView: admin_access_for_access_groups
                                    )(implicit appConfig: AppConfig, cc: MessagesControllerComponents) extends FrontendController(cc) with I18nSupport {

  def redirectForNoAssignment: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    if(request.agentInfo.isAdmin) {
      Redirect(routes.NoAccessGroupsAssignmentController.showAdminAccessInformation()).toFuture
    } else {
      Redirect(routes.AgentServicesController.administrators).toFuture
    }
  }

  //For access groups
  def showAdminAccessInformation(): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    if(request.agentInfo.isAdmin) {
      Ok(adminInfoView()).toFuture
    } else {
      Forbidden.toFuture
    }
  }


}
