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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ToFuture
import uk.gov.hmrc.agentservicesaccount.forms.NewAmlsSupervisoryBodyForm
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.new_supervisory_body
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AmlsNewSupervisoryBodyController @Inject() (
  actions: Actions,
  amlsLoader: AMLSLoader,
  val sessionCacheService: SessionCacheService,
  newSupervisoryBody: new_supervisory_body,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with AmlsJourneySupport
with I18nSupport {

  def showPage(cya: Boolean): Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { amlsJourney =>
        val amlsBodies = amlsLoader.load("/amls.csv")
        val form = NewAmlsSupervisoryBodyForm.form(amlsBodies)(amlsJourney.isUkAgent).fill(amlsJourney.newAmlsBody.getOrElse(""))
        Ok(newSupervisoryBody(
          form,
          amlsBodies,
          amlsJourney.isUkAgent,
          cya
        )).toFuture
      }
    }
  }

  def onSubmit(cya: Boolean): Action[AnyContent] = Action.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { journey =>
        val amlsBodies = amlsLoader.load("/amls.csv")
        NewAmlsSupervisoryBodyForm.form(amlsBodies)(journey.isUkAgent)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              BadRequest(newSupervisoryBody(
                formWithErrors,
                amlsBodies,
                journey.isUkAgent,
                cya
              )).toFuture,
            data => {
              val updatedJourney = journey.copy(
                newAmlsBody =
                  if (journey.isUkAgent)
                    Some(amlsBodies(data))
                  else
                    Some(data),
                isAmlsBodyStillTheSame = maybeChangePreviousAnswer(data, journey)
              )
              saveAmlsJourney(updatedJourney).map(_ =>
                Redirect(nextPage(
                  cya,
                  updatedJourney,
                  journey
                ))
              )
            }
          )
      }
    }
  }

  private def maybeChangePreviousAnswer(
    answer: String,
    journey: UpdateAmlsJourney
  ): Option[Boolean] =
    for {
      x <- journey.isAmlsBodyStillTheSame
      y = journey.newAmlsBody.contains(answer)
    } yield x && y

  private def nextPage(
    cya: Boolean,
    updatedJourney: UpdateAmlsJourney,
    existingJourney: UpdateAmlsJourney
  ): String = {
    if (cya) {
      if (updatedJourney.isHmrc && !existingJourney.newAmlsBody.exists(_.contains("HMRC")))
        routes.EnterRegistrationNumberController.showPage(cya).url
      else
        routes.CheckYourAnswersController.showPage.url
    }
    else if (updatedJourney.isUkAgent & updatedJourney.isAmlsBodyStillTheSame.contains(true))
      routes.ConfirmRegistrationNumberController.showPage.url
    else
      routes.EnterRegistrationNumberController.showPage().url
  }

}
