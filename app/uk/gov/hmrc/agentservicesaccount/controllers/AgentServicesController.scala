/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.api.Logging
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.{AuthActions, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AfiRelationshipConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.views.html.pages._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentServicesController @Inject()(
  authActions: AuthActions,
  agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
  val withMaybePasscode: PasscodeVerification,
  afiRelationshipConnector: AfiRelationshipConnector,
  suspensionWarningView: suspension_warning,
  manageAccountView: manage_account,
  asaDashboard: asa_dashboard)(
  implicit val appConfig: AppConfig,
  val cc: MessagesControllerComponents,
  ec: ExecutionContext,
  messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {

  import authActions._

  val customDimension = appConfig.customDimension
  val agentSuspensionEnabled = appConfig.agentSuspensionEnabled

  val root: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      if (agentSuspensionEnabled) {
        agentClientAuthorisationConnector.getSuspensionDetails().map { suspensionDetails =>
          if (!suspensionDetails.suspensionStatus) Redirect(routes.AgentServicesController.showAgentServicesAccount())
          else
            Redirect(routes.AgentServicesController.showSuspendedWarning())
              .addingToSession(
                "suspendedServices" -> suspensionDetails.toString,
                "isSuspendedForVat" -> suspensionDetails.suspendedRegimes.contains("VATC").toString)
        }
      } else Future successful Redirect(routes.AgentServicesController.showAgentServicesAccount())
    }
  }

  val showAgentServicesAccount: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      withIrvAllowed(agentInfo.arn) { isvAllowed =>
        logger.info(s"isAdmin: ${agentInfo.isAdmin}")
        if (agentSuspensionEnabled) {
          request.session.get("isSuspendedForVat") match {
            case Some(isSuspendedForVat) =>
              Future successful Ok(
                asaDashboard(
                  formatArn(agentInfo.arn),
                  isvAllowed,
                  customDimension,
                  agentInfo.isAdmin,
                  isSuspendedForVat.toBoolean))

            case None =>
              agentClientAuthorisationConnector.getSuspensionDetails().map { suspensionDetails =>
                Ok(
                  asaDashboard(
                    formatArn(agentInfo.arn),
                    isvAllowed,
                    customDimension,
                    agentInfo.isAdmin,
                    suspensionDetails.suspendedRegimes.contains("VATC")))
              }
          }
        } else
          Future successful Ok(
            asaDashboard(
              formatArn(agentInfo.arn),
              isvAllowed,
              customDimension,
              agentInfo.isAdmin,
              isSuspendedForVat = false))
      }
    }
  }

  val showSuspendedWarning: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      Future successful Ok(suspensionWarningView(agentInfo.isAdmin))
    }
  }

  val manageAccount: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      withIrvAllowed(agentInfo.arn) { _ =>
        if (agentInfo.isAdmin) {
          Future.successful(Ok(manageAccountView()))
        } else {
          Future.successful(Forbidden)
        }
      }
    }
  }

  private def withIrvAllowed(arn: Arn)(f: Boolean => Future[Result])(implicit request: Request[_]): Future[Result] = {
    if (appConfig.irvAllowlistEnabled) {
      afiRelationshipConnector.checkIrvAllowed(arn).flatMap(f)
    } else {
      withMaybePasscode(f)
    }
  }

  private def formatArn(arn: Arn): String = {
    val arnStr = arn.value
    s"${arnStr.take(4)} ${arnStr.slice(4, 7)} ${arnStr.drop(7)}"
  }

}
