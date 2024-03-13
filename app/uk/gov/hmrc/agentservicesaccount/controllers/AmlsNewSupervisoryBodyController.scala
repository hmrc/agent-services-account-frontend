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
import uk.gov.hmrc.agentservicesaccount.forms.NewAmlsSupervisoryBodyForm
import uk.gov.hmrc.agentservicesaccount.repository.AmlsJourneySessionRepository
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.new_supervisory_body
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AmlsNewSupervisoryBodyController @Inject() (actions: Actions,
                                                  amlsLoader: AMLSLoader,
                                                  val amlsJourneySessionRepository: AmlsJourneySessionRepository,
                                                  newSupervisoryBody: new_supervisory_body,
                                                   cc: MessagesControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {


  def showNewSupervisoryBody: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request  =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withAmlsJourneySession { journey =>
        val amlsBodies = amlsLoader.load("/amls-no-hmrc.csv")
        val form = NewAmlsSupervisoryBodyForm.form(amlsBodies)(journey.isUkAgent).fill(journey.newAmlsBody.getOrElse(""))
        // TODO // define the backlink (either '/confirm-supervisory-body' or '/check-your-answers' - awaiting routing logic APB-7796
        Future successful Ok(newSupervisoryBody(form, amlsBodies, journey.isUkAgent))
      }
    }
  }

  def submitNewSupervisoryBody: Action[AnyContent] = Action.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withAmlsJourneySession { journey =>
        val amlsBodies = amlsLoader.load("/amls-no-hmrc.csv")
        NewAmlsSupervisoryBodyForm.form(amlsBodies)(journey.isUkAgent)
          .bindFromRequest()
          .fold(
            formWithErrors => Future successful Ok(newSupervisoryBody(formWithErrors, amlsBodies, journey.isUkAgent)),
            data => {
              saveAmlsJourneySession(journey.copy(newAmlsBody = Some(data))).map(_ =>
              Redirect("/not-implemented")) //TODO - next page (/supervisory-number) APB-7794
            }
          )
      }
    }
  }

}
