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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.FeedbackWhichServiceForm
import uk.gov.hmrc.agentservicesaccount.forms.SignOutForm
import uk.gov.hmrc.agentservicesaccount.views.html.pages.survey
import uk.gov.hmrc.agentservicesaccount.views.html.pages.survey_which_service
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future

class SurveyController @Inject() (implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  surveyView: survey,
  whichServiceView: survey_which_service
)
extends FrontendController(cc)
with I18nSupport {

  def showSurvey: Action[AnyContent] = Action.async { implicit request =>
    Future successful Ok(surveyView(SignOutForm.form))
  }

  def submitSurvey: Action[AnyContent] = Action.async { implicit request =>
    val errorFunction = { formWithErrors: Form[String] =>
      Future successful BadRequest(surveyView(formWithErrors))
    }

    val successFunction = { key: String =>
      key match {
        case "ACCESSINGSERVICE" if appConfig.feedbackSurveyServiceSelect => Future successful Redirect(routes.SurveyController.showWhichService())
        case k => Future successful Redirect(appConfig.signOutUrlWithSurvey(key))

      }
    }

    SignOutForm.form
      .bindFromRequest()
      .fold(
        errorFunction,
        successFunction
      )
  }

  def showWhichService: Action[AnyContent] = Action.async { implicit request =>
    if (appConfig.feedbackSurveyServiceSelect)
      Future successful Ok(whichServiceView(FeedbackWhichServiceForm.form))
    else
      Future.failed(new UnsupportedOperationException("Display of this page is disabled by configuration."))
  }

  def submitWhichService: Action[AnyContent] = Action.async { implicit request =>
    val errorFunction = { formWithErrors: Form[String] =>
      Future successful BadRequest(whichServiceView(formWithErrors))
    }

    // APB-5437
    val feedbackKeyMapping = Map(
      "VAT" -> "VATCA",
      "IT" -> "ITVC",
      "TRUST" -> "trusts",
      "IR" -> "AGENTINDIV",
      "CGT" -> "AGENTHOME",
      "OTHER" -> "AGENTHOME"
    )

    val successFunction = { key: String =>
      Future successful Redirect(appConfig.signOutUrlWithSurvey(feedbackKeyMapping.apply(key)))
    }

    if (appConfig.feedbackSurveyServiceSelect) {
      FeedbackWhichServiceForm.form
        .bindFromRequest()
        .fold(
          errorFunction,
          successFunction
        )
    }
    else {
      Future.failed(new UnsupportedOperationException("Display of this page is disabled by configuration."))
    }
  }

}
