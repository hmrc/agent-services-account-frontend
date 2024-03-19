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
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.models.{AmlsCheckYourAnswers, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.check_your_answers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CheckYourAnswersController @Inject()(actions: Actions,
                                           val updateAmlsJourneyRepository: UpdateAmlsJourneyRepository,
                                           checkYourAnswers: check_your_answers,
                                           cc: MessagesControllerComponents
                                          )(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { amlsJourney =>
        checkJourneyData(amlsJourney) { checkYourAnswersData =>
          Ok(checkYourAnswers(checkYourAnswersData)).toFuture
        }
      }
    }
  }

  private[amls] def buildViewModel(userInTheUk: Boolean, journey: UpdateAmlsJourney): Seq[SummaryListData] = {
    for {
      body <- journey.newAmlsBody
      regNumber <- journey.newRegistrationNumber
      renewalDate <- journey.newExpirationDate
    } yield {
      val items = Seq(
        SummaryListData(
          key = "replace this with the supervisory body message key",
          value = body,
          link = Some(amls.routes.AmlsNewSupervisoryBodyController.showPage)
        ),
        SummaryListData(
          key = "replace this with the registration number message key",
          value = regNumber,
          link = Some(amls.routes.ConfirmRegistrationNumberController.showPage)
        ),

        SummaryListData(
          key = "replace this with the renewal date message key",
          value = renewalDate.toString,
          link = Some(amls.routes.EnterRenewalDateController.showPage)
        )

      )
      items
    }
  }.getOrElse(Seq.empty)

  private def checkJourneyData(updateAmlsJourney: UpdateAmlsJourney)
                              (action: AmlsCheckYourAnswers => Future[Result]): Future[Result] = {

    (updateAmlsJourney.newAmlsBody, updateAmlsJourney.newRegistrationNumber, updateAmlsJourney.newExpirationDate) match {
      case (Some(body), Some(regNum), Some(renewalDate))
        if updateAmlsJourney.isUkAgent => action(AmlsCheckYourAnswers(body, regNum, Some(renewalDate)))
      case (Some(body), Some(regNum), mRenewalDate) =>
        if (!updateAmlsJourney.isUkAgent) action(AmlsCheckYourAnswers(body, regNum, mRenewalDate))
        else Redirect(amls.routes.EnterRenewalDateController.showPage).toFuture
      case (Some(_), None, _) => Redirect(amls.routes.EnterRegistrationNumberController.showPage).toFuture
      case (None, _, _) => Redirect(amls.routes.AmlsNewSupervisoryBodyController.showPage).toFuture
      case _ =>
        Redirect(routes.AgentServicesController.manageAccount).toFuture

    }
  }
}

