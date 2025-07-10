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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.views.html.signed_out
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future

class SignOutController @Inject() (implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  signedOutView: signed_out
)
extends FrontendController(cc)
with I18nSupport {

  private def signOutWithContinue(continue: String) = {
    val signOutAndRedirectUrl: String = uri"${appConfig.signOut}?${Map("continue" -> continue)}".toString
    Redirect(signOutAndRedirectUrl)
  }

  def signOut: Action[AnyContent] = Action.async {
    val continue = uri"${appConfig.asaFrontendExternalUrl + routes.SurveyController.showSurvey().url}"
    Future successful signOutWithContinue(continue.toString)
  }

  def signedOut: Action[AnyContent] = Action.async {
    Future successful signOutWithContinue(appConfig.continueFromGGSignIn)
  }

  def onlineSignIn: Action[AnyContent] = Action.async {
    Future successful signOutWithContinue(appConfig.hmrcOnlineSignInLink)
  }

  def timeOut: Action[AnyContent] = Action.async {
    val continue = uri"${appConfig.asaFrontendExternalUrl + routes.SignOutController.timedOut().url}"
    Future.successful(signOutWithContinue(continue.toString))
  }

  def timedOut: Action[AnyContent] = Action.async { implicit request =>
    Future successful Forbidden(signedOutView(appConfig.continueFromGGSignIn))
  }

}
