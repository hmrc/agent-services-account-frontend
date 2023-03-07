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
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, OptedInReady, OptinStatus}
import uk.gov.hmrc.agentservicesaccount.auth.CallOps._
import uk.gov.hmrc.agentservicesaccount.auth.{AgentInfo, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentClientAuthorisationConnector, AgentPermissionsConnector, AgentUserClientDetailsConnector}
import uk.gov.hmrc.agentservicesaccount.models.ManageAccessPermissionsConfig
import uk.gov.hmrc.agentservicesaccount.views.html.pages._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentServicesController @Inject()
(
  authActions: AuthActions,
  agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
  agentPermissionsConnector: AgentPermissionsConnector,
  agentUserClientDetailsConnector: AgentUserClientDetailsConnector,
  suspensionWarningView: suspension_warning,
  manage_account: manage_account,
  administrators_html: administrators,
  your_account: your_account,
  asaDashboard: asa_dashboard,
  accountDetailsView: account_details,
  helpView: help)(implicit val appConfig: AppConfig,
                  val cc: MessagesControllerComponents,
                  ec: ExecutionContext,
                  messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {

  import authActions._

  val customDimension: String = appConfig.customDimension

  val root: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
        agentClientAuthorisationConnector.getSuspensionDetails().map { suspensionDetails =>
          if (!suspensionDetails.suspensionStatus) Redirect(routes.AgentServicesController.showAgentServicesAccount())
          else
            Redirect(routes.AgentServicesController.showSuspendedWarning())
              .addingToSession(
                "suspendedServices" -> suspensionDetails.toString,
                "isSuspendedForVat" -> suspensionDetails.suspendedRegimes.contains("VATC").toString)
        }
    }
  }

  val showAgentServicesAccount: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      withShowFeatureInvite(agentInfo.arn) { showFeatureInvite : Boolean =>
          request.session.get("isSuspendedForVat") match {
            case Some(isSuspendedForVat) =>
              Future successful Ok(
                asaDashboard(
                  formatArn(agentInfo.arn),
                  showFeatureInvite,
                  customDimension,
                  agentInfo.isAdmin,
                  isSuspendedForVat.toBoolean)).addingToSession(toReturnFromMapping)

            case None =>
              agentClientAuthorisationConnector.getSuspensionDetails().map { suspensionDetails =>
                Ok(
                  asaDashboard(
                    formatArn(agentInfo.arn),
                    showFeatureInvite,
                    customDimension,
                    agentInfo.isAdmin,
                    suspensionDetails.suspendedRegimes.contains("VATC"))).addingToSession(toReturnFromMapping)
              }
          }
      }

    }
  }

  private def toReturnFromMapping()(implicit request: Request[AnyContent]) = {
    val sessionKeyUsedInMappingService = "OriginForMapping"
    sessionKeyUsedInMappingService -> localFriendlyUrl(env)(request.path, request.host)
  }

  val showSuspendedWarning: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      Future successful Ok(suspensionWarningView(agentInfo.isAdmin))
    }
  }

  val manageAccount: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      if (agentInfo.isAdmin) {
        if (appConfig.granPermsEnabled) {
          agentPermissionsConnector.isArnAllowed flatMap {
            case true =>
              for {
                maybeOptinStatus <- agentPermissionsConnector.getOptinStatus(agentInfo.arn)
                mGroups <- agentPermissionsConnector.getGroupsSummaries(agentInfo.arn)
                hasAnyGroups = mGroups.exists(_.groups.nonEmpty)
              } yield {
                maybeOptinStatus.foreach(syncEacdIfOptedIn(agentInfo.arn, _))
                Ok(manage_account(maybeOptinStatus.map(ManageAccessPermissionsConfig(_, hasAnyGroups))))
              }
            case false =>
              Future successful Ok(manage_account(None))
          }
        } else {
          Future.successful(Ok(manage_account(None)))
        }
      } else {
        if (appConfig.granPermsEnabled) {
          Future.successful(Redirect(routes.AgentServicesController.yourAccount))
        } else {
          Future.successful(Forbidden)
        }
      }
    }
  }

  private def syncEacdIfOptedIn(arn: Arn, optinStatus: OptinStatus)(implicit hc: HeaderCarrier) = {
    if (optinStatus == OptedInReady) agentPermissionsConnector.syncEacd(arn)
    else Future.successful(())
  }

  val yourAccount: Action[AnyContent] = Action.async { implicit request =>
    withFullUserDetails { (agentInfo: AgentInfo) =>
      if (!agentInfo.isAdmin) {
        if (appConfig.granPermsEnabled) {
          agentInfo.credentials.fold(Ok(your_account(None)).toFuture) { credentials =>
            agentPermissionsConnector.isOptedIn(agentInfo.arn).flatMap(isOptedIn =>
              if (isOptedIn)
                agentPermissionsConnector.getGroupsForTeamMember(agentInfo.arn, credentials.providerId)
                  .map(maybeSummaries => Ok(your_account(Some(agentInfo), maybeSummaries)))
              else
                Ok(your_account(Some(agentInfo), None, optedIn = false)).toFuture
            )
          }
        } else {
          Ok(your_account(None)).toFuture
        }
      } else {
        Forbidden.toFuture
      }
    }
  }

  val administrators: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      agentUserClientDetailsConnector.getTeamMembers(agentInfo.arn).map{ maybeMembers =>
        Ok(
          administrators_html(
            maybeMembers.getOrElse(Seq.empty)
              .filterNot(_.credentialRole.getOrElse("").equals("Assistant")))
        )
      }
    }
  }

  val accountDetails: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
        agentClientAuthorisationConnector.getAgencyDetails().map(agencyDetails => Ok(accountDetailsView(agencyDetails, agentInfo.isAdmin)))
    }
  }

  val showHelp: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { agentInfo =>
      Future successful Ok(helpView(agentInfo.isAdmin))
    }
  }

  private def formatArn(arn: Arn): String = {
    val arnStr = arn.value
    s"${arnStr.take(4)} ${arnStr.slice(4, 7)} ${arnStr.drop(7)}"
  }

  private def withShowFeatureInvite(arn: Arn)(f: Boolean => Future[Result])(implicit request: Request[_]): Future[Result] = {
    agentPermissionsConnector.isShownPrivateBetaInvite.flatMap(f)
  }

}
