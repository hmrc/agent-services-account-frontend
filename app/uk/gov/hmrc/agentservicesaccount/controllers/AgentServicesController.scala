/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.{AuthActions, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentServicesController @Inject()(
                                         authActions: AuthActions,
                                         asaConnector: AgentServicesAccountConnector,
                                         val withMaybePasscode: PasscodeVerification,
                                         @Named("customDimension") customDimension: String)
                                       (implicit val externalUrls: ExternalUrls,
                                        configuration: Configuration, ec: ExecutionContext, messagesApi: MessagesApi)
  extends AgentServicesBaseController {

  import authActions._

  val root: Action[AnyContent] = Action.async { implicit request =>
    withMaybePasscode { isWhitelisted =>
      withAuthorisedAsAgent { agentInfo =>
        Logger.info(s"isAdmin: ${agentInfo.isAdmin}")
        Future.successful(Ok(agent_services_account(formatArn(agentInfo.arn), isWhitelisted, customDimension, agentInfo.isAdmin)))
      }
    }
  }

  val manageAccount: Action[AnyContent] = Action.async { implicit request =>
    withMaybePasscode { _ =>
      withAuthorisedAsAgent { agentInfo =>
        if (agentInfo.isAdmin) {
          Future.successful(Ok(manage_account()))
        } else {
          Future.successful(Forbidden)
        }
      }
    }
  }

  private def formatArn(arn: Arn): String = {
    val arnStr = arn.value
    s"${arnStr.take(4)} ${arnStr.slice(4, 7)} ${arnStr.drop(7)}"
  }

}
