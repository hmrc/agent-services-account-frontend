/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.agentmtdidentifiers.model.OptedInReady
import uk.gov.hmrc.agentservicesaccount.auth.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentservicesaccount.views.html.EACD._
import uk.gov.hmrc.agentservicesaccount.views.html.pages._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class ManageAgentServicesAccountLanding @Inject()(

                                                   authActions: AuthActions,

                                                   agentPermissionsConnector: AgentPermissionsConnector,

                                                   suspensionWarningView: suspension_warning,

                                                   asa_bridging_screen: asa_bridging_screen,

                                                   helpView: help)(implicit val appConfig: AppConfig,
                                                                   val cc: MessagesControllerComponents,
                                                                   ec: ExecutionContext,
                                                                   messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {


  val showAccessGroupSummaryForASA: Action[AnyContent] = Action.async { implicit request =>

    authActions.withAuthorisedAsAgent { agentInfo => // agentInfo is a val for withAuthorisedAsAgent
      if (agentInfo.isAdmin) { // <- whats happening here

        if (appConfig.granPermsEnabled) { // checks GranPremsEnable
          for {
            maybeOptinStatus <- agentPermissionsConnector.getOptinStatus(agentInfo.arn) //maybeOptinStatus is a instance of getOptinStatus from agentPermissionConnector
          } yield {
            maybeOptinStatus match { // pattern match on mayOptinStatus
              case Some(OptedInReady) => (Ok(asa_bridging_screen(true)))

              case _ => (Ok(asa_bridging_screen(false)))
            }

          }

        } else {
          Future.successful(Ok(asa_bridging_screen(false)))


        }

      } else {

        Future.successful(Unauthorized)
      }
    }
  }


}
