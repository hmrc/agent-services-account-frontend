/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.SignOutForm
import uk.gov.hmrc.agentservicesaccount.views.html.pages.survey
import uk.gov.hmrc.agentservicesaccount.views.html.signed_out
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

class SignOutController @Inject()(
  implicit val appConfig: AppConfig,
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  surveyView: survey,
  signedOutView: signed_out)
    extends FrontendController(cc) with I18nSupport {

  def showSurvey: Action[AnyContent] = Action.async { implicit request =>
    Future successful Ok(surveyView(SignOutForm.form))
  }

  def submitSurvey: Action[AnyContent] = Action.async { implicit request =>
    val errorFunction = { formWithErrors: Form[String] =>
      Future successful BadRequest(surveyView(formWithErrors))
    }

    val successFunction = { key: String =>
      Future successful Redirect(appConfig.signOutUrlWithSurvey(key))
    }

    SignOutForm.form
      .bindFromRequest()
      .fold(
        errorFunction,
        successFunction
      )
  }

  def signOut: Action[AnyContent] = Action.async {
    Future successful Redirect(routes.SignOutController.showSurvey()).withNewSession
  }

  def signedOut = Action.async {
    Future successful Redirect(appConfig.continueFromGGSignIn).withNewSession
  }

  def onlineSignIn: Action[AnyContent] = Action.async {
    Future successful Redirect(appConfig.hmrcOnlineSignInLink).withNewSession
  }

  def timedOut = Action.async { implicit request =>
    Future successful Forbidden(signedOutView(appConfig.continueFromGGSignIn)).withNewSession
  }

  def keepAlive: Action[AnyContent] = Action.async {
    Future successful Ok("OK")
  }
}
