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
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.is_amls_hmrc
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class AmlsIsHmrcController @Inject()(actions: Actions,
                                     isAmlsHmrc: is_amls_hmrc,
                                     cc: MessagesControllerComponents
                                    )(implicit appConfig: AppConfig) extends FrontendController(cc) with I18nSupport {

  def showAmlsIsHmrc: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      Ok(isAmlsHmrc(YesNoForm.form("amls.is-hmrc.error"))).toFuture
    }
  }

  def submitAmlsIsHmrc: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      YesNoForm.form("amls.is-hmrc.error")
        .bindFromRequest()
        .fold(
          formWithErrors => {
          Ok(isAmlsHmrc(formWithErrors)).toFuture
          },
          (isHmrcRegistered: Boolean) => {
            if (isHmrcRegistered) {
              Redirect("not-implemented-hmrc-page").toFuture
            } else {
              // TODO update with route in APB-7547
              Redirect("manage-account/update-money-laundering-supervision").toFuture
            }
          })
    }
  }

}
