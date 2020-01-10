/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Configuration
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.views.html.signed_out
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class SignOutController @Inject()(implicit val externalUrls: ExternalUrls, configuration: Configuration, val messagesApi: MessagesApi)
  extends FrontendController with I18nSupport {

  def signOut: Action[AnyContent] = Action.async { implicit request =>
      Future successful Redirect(externalUrls.signOutUrlWithSurvey).removingFromSession("otacTokenParam")
  }

  def signedOut = Action.async { implicit request =>
    Future successful Redirect(externalUrls.continueFromGGSignIn).withNewSession
  }

  def timedOut = Action.async { implicit request =>
    Future successful Forbidden(signed_out(externalUrls.continueFromGGSignIn)).withNewSession
  }

  def keepAlive: Action[AnyContent] = Action.async { implicit request =>
    Future successful Ok("OK")
  }
}
