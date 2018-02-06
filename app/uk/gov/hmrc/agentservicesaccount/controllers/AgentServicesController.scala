/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject._

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentservicesaccount.AppConfig
import uk.gov.hmrc.agentservicesaccount.auth.{AuthActions, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages._
import uk.gov.hmrc.auth.core.NoActiveSession
import uk.gov.hmrc.play.frontend.controller.FrontendController

@Singleton
class AgentServicesController @Inject()(
                                         val messagesApi: MessagesApi,
                                         authActions: AuthActions,
                                         continueUrlActions: ContinueUrlActions,
                                         asaConnector: AgentServicesAccountConnector,
                                         val withMaybePasscode: PasscodeVerification
                                       )
                                       (implicit val externalUrls: ExternalUrls, appConfig: AppConfig) extends FrontendController with I18nSupport {


  val root: Action[AnyContent] = Action.async { implicit request =>
    withMaybePasscode { isWhitelisted =>
      authActions.authorisedWithAgent { agent =>
        continueUrlActions.withMaybeContinueUrl { continueUrlOpt =>
          asaConnector.getAgencyName(agent.arn).map { maybeAgencyName =>
            Ok(agent_services_account(agent.arn, maybeAgencyName, continueUrlOpt, isWhitelisted, routes.SignOutController.signOut().url))
          }
        }
      } map { maybeResult =>
        maybeResult.getOrElse(authActions.redirectToAgentSubscriptionGgSignIn)
      } recover {
        case _: NoActiveSession =>
          authActions.redirectToAgentSubscriptionGgSignIn
      }
    }
  }

}
