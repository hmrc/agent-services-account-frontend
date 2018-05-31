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

import play.api.{Configuration, Logger}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentservicesaccount.auth.{AuthActions, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages._
import uk.gov.hmrc.auth.core.NoActiveSession
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class AgentServicesController @Inject()(
                                         val messagesApi: MessagesApi,
                                         authActions: AuthActions,
                                         asaConnector: AgentServicesAccountConnector,
                                         val withMaybePasscode: PasscodeVerification,
                                         @Named("customDimension") customDimension: String
                                       )
                                       (implicit val externalUrls: ExternalUrls, configuration: Configuration) extends FrontendController with I18nSupport {


  val root: Action[AnyContent] = Action.async { implicit request =>
    withMaybePasscode { isWhitelisted =>
      authActions.authorisedWithAgent { agentInfo =>
        asaConnector.getAgencyName(agentInfo.arn).map { maybeAgencyName =>
          Logger.info(s"isAdmin: ${agentInfo.isAdmin}")
          Ok(agent_services_account(agentInfo.arn, agentInfo.isAdmin, maybeAgencyName, isWhitelisted, customDimension))
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
