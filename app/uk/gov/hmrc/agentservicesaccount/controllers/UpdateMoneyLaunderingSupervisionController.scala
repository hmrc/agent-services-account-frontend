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
import uk.gov.hmrc.agentservicesaccount.forms.UpdateMoneyLaunderingSupervisionForm
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UpdateMoneyLaunderingSupervisionController @Inject()()(implicit appConfig: AppConfig,
                                                             amlsLoader: AMLSLoader,
                                                             actions: Actions,
                                                             cacheService: SessionCacheService,
                                                             //UpdateMoneySupervisonFormDetails: UpdateMoneyLaunderingSupervisionDetails,
                                                             UpdateMoneySupervisonView: update_money_laundering_supervision_details,
                                                             cc: MessagesControllerComponents,
                                                             ec: ExecutionContext) extends FrontendController(cc) with I18nSupport{

  private val amlsBodies: Map[String, String] = amlsLoader.load("/amls.csv")

  def showUpdateMoneyLaunderingSupervision: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
      val updateAmlsForm = UpdateMoneyLaunderingSupervisionForm.form
      //  UpdateMoneyLaunderingSupervisionDetails

      Ok(UpdateMoneySupervisonView(updateAmlsForm, amlsBodies)).toFuture
    }

  def submitUpdateMoneyLaunderingSupervision: Action[AnyContent] = actions.authActionOnlyForSuspended.async { implicit request =>
    UpdateMoneyLaunderingSupervisionForm.form.bindFromRequest().fold(
      formWithErrors => {
        UpdateMoneySupervisonView(formWithErrors, amlsBodies).toFuture.map(BadRequest(_))
      },
      formData => {
        for {
          _ <- cacheService.put(BODY,formData.body )
          _ <- cacheService.put(REG_NUMBER, formData.number)
          _ <- cacheService.put(END_DATE, formData.endDate)
        } yield Redirect(routes.AmlsConfirmationController.showUpdatedAmlsConfirmationPage)
      }
    )
  }
}
