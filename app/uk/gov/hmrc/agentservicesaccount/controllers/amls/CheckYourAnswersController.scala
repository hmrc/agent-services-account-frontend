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
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.check_your_answers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class CheckYourAnswersController @Inject()(amlsLoader: AMLSLoader,
                                           actions: Actions,
                                           val updateAmlsJourneyRepository: UpdateAmlsJourneyRepository,
                                           checkYourAnswers: check_your_answers,
                                           cc: MessagesControllerComponents
                                          )(implicit appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {

  private lazy val supervisoryBodies: Map[String, String] = amlsLoader.load("/amls.csv")

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { journeyData =>
        val model = buildSummaryListItems(journeyData.isUkAgent, journeyData, request.messages.lang.locale)
        Ok(checkYourAnswers(model)).toFuture
      }
    }
  }

  private[amls] def buildSummaryListItems(isUkAgent: Boolean, journey: UpdateAmlsJourney, lang: Locale): Seq[SummaryListData] = {
    for {
      body <- journey.newAmlsBody.map {
        case body if isUkAgent => supervisoryBodies(body)
        case body => body
      }
      regNumber <- journey.newRegistrationNumber
    } yield {
      val items = Seq(
        SummaryListData(
          key = "amls.check-your-answers.supervisory-body",
          value = body,
          link = Some(amls.routes.AmlsNewSupervisoryBodyController.showPage(true))
        ),
        SummaryListData(
          key = "amls.check-your-answers.registration-number",
          value = regNumber,
          link = Some(amls.routes.EnterRegistrationNumberController.showPage(true))
        )
      )

      (isUkAgent, journey.newExpirationDate) match {
        case (false, _) => items
        case (true, Some(date)) =>
          val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", lang)
          items ++ Seq(SummaryListData(
            key = "amls.check-your-answers.renewal-date",
            value = date.format(formatter),
            link = Some(amls.routes.EnterRenewalDateController.showPage)
          ))
      }
    }
  }.getOrElse(throw new Exception("Expected AMLS journey data missing"))

}
