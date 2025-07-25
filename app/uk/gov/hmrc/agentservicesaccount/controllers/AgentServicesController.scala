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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.optin.OptedInReady
import uk.gov.hmrc.agents.accessgroups.optin.OptinStatus
import uk.gov.hmrc.agentservicesaccount.actions.CallOps._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentPermissionsConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentUserClientDetailsConnector
import uk.gov.hmrc.agentservicesaccount.controllers.amls.{routes => amlsRoutes}
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatus
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses._
import uk.gov.hmrc.agentservicesaccount.views.html.pages._
import uk.gov.hmrc.agentservicesaccount.views.html.pages.assistant.administrators
import uk.gov.hmrc.agentservicesaccount.views.html.pages.assistant.your_account
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentServicesController @Inject() (
  authActions: AuthActions,
  actions: Actions,
  agentPermissionsConnector: AgentPermissionsConnector,
  agentUserClientDetailsConnector: AgentUserClientDetailsConnector,
  agentAssuranceConnector: AgentAssuranceConnector,
  manage_account: manage_account,
  administrators_html: administrators,
  your_account: your_account,
  asaDashboard: asa_dashboard,
  account_details: account_details,
  helpView: help
)(implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  import authActions._

  val root: Action[AnyContent] = actions.authActionCheckSuspend {
    Redirect(routes.AgentServicesController.showAgentServicesAccount())
  }

  val showAgentServicesAccount: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    val agentInfo = request.agentInfo
    /* TODO remove call to withShowFeatureInvite if okay with 28 day duration on UR banner
     *      showFeatureInvite is unused at the mo
     * */
    withShowFeatureInvite(agentInfo.arn) { showFeatureInvite: Boolean =>
      Future successful Ok(
        asaDashboard(
          formatArn(agentInfo.arn),
          showFeatureInvite && agentInfo.isAdmin,
          agentInfo.isAdmin
        )
      ).addingToSession(toReturnFromMapping())
    }
  }

  private def toReturnFromMapping()(implicit request: Request[AnyContent]) = {
    val sessionKeyUsedInMappingService = "OriginForMapping"
    sessionKeyUsedInMappingService -> localFriendlyUrl(env)(request.path, request.host)
  }

  val manageAccount: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    val agentInfo = request.agentInfo
    if (agentInfo.isAdmin) {

      for {
        amlsStatus <- agentAssuranceConnector.getAmlsStatus(agentInfo.arn)
        amlsLink = getAmlsStatusLink(amlsStatus)
        accessGroups <-
          if (appConfig.granPermsEnabled)
            agentPermissionsConnector.isArnAllowed flatMap {
              case true =>
                for {
                  maybeOptinStatus <- agentPermissionsConnector.getOptinStatus(agentInfo.arn)
                  mGroups <- agentPermissionsConnector.getGroupsSummaries(agentInfo.arn)
                  hasAnyGroups = mGroups.exists(_.groups.nonEmpty)
                } yield {
                  maybeOptinStatus.foreach(syncEacdIfOptedIn(agentInfo.arn, _))
                  (maybeOptinStatus, hasAnyGroups)
                }
              case false => Future successful (None, false)
            }
          else
            Future successful (None, false)

      } yield Ok(manage_account(
        Some(amlsLink.msg),
        Some(amlsLink.href),
        accessGroups._1,
        accessGroups._2
      ))
    }
    else {
      if (appConfig.granPermsEnabled) {
        Future.successful(Redirect(routes.AgentServicesController.yourAccount))
      }
      else
        Future.successful(Forbidden)
    }
  }

  private case class AmlsLink(
    msg: String,
    href: String
  )
  private def getAmlsStatusLink(amlsStatus: AmlsStatus): AmlsLink = {
    amlsStatus match {
      case NoAmlsDetailsNonUK | ValidAmlsNonUK | ValidAmlsDetailsUK | PendingAmlsDetails =>
        AmlsLink("manage.account.amls.view", amlsRoutes.ViewDetailsController.showPage.url)
      case NoAmlsDetailsUK | PendingAmlsDetailsRejected => AmlsLink("manage.account.amls.add", amlsRoutes.ViewDetailsController.showPage.url)
      case ExpiredAmlsDetailsUK => AmlsLink("manage.account.amls.update", amlsRoutes.ViewDetailsController.showPage.url)
    }
  }

  private def syncEacdIfOptedIn(
    arn: Arn,
    optinStatus: OptinStatus
  )(implicit rh: RequestHeader) = {
    if (optinStatus == OptedInReady)
      agentPermissionsConnector.syncEacd(arn, fullSync = true)
    else
      Future.successful(())
  }

  val yourAccount: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    if (!request.agentInfo.isAdmin) {
      if (appConfig.granPermsEnabled) {
        request.agentInfo.credentials.fold(Ok(your_account()).toFuture) { credentials =>
          agentPermissionsConnector
            .isOptedIn(request.agentInfo.arn)
            .flatMap(isOptedIn =>
              if (isOptedIn)
                agentPermissionsConnector.getGroupsForTeamMember(request.agentInfo.arn, credentials.providerId)
                  .map(maybeSummaries =>
                    Ok(your_account(
                      info = Some(request.agentInfo),
                      groups = maybeSummaries
                    ))
                  )
              else
                Ok(your_account(
                  info = Some(request.agentInfo),
                  groups = None,
                  optedIn = false
                )).toFuture
            )
        }
      }
      else {
        Ok(your_account(None)).toFuture
      }
    }
    else {
      Forbidden.toFuture
    }
  }

  val administrators: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    val agentInfo = request.agentInfo
    agentUserClientDetailsConnector.getTeamMembers(agentInfo.arn).map { maybeMembers =>
      Ok(
        administrators_html(
          agentInfo.isAdmin,
          maybeMembers.getOrElse(Seq.empty)
            .filterNot(_.credentialRole.getOrElse("").equals("Assistant"))
        )
      )
    }
  }

  val accountDetails: Action[AnyContent] = actions.authActionWithSuspensionCheckWithAgentRecord.async { implicit request =>
    Ok(account_details(request.agentDetailsDesResponse.agencyDetails, request.authRequestWithAgentInfo.agentInfo.isAdmin)).toFuture
  }

  val showHelp: Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>
    Ok(helpView(request.agentInfo.isAdmin))

  }

  private def formatArn(arn: Arn): String = {
    val arnStr = arn.value
    s"${arnStr.take(4)} ${arnStr.slice(4, 7)} ${arnStr.drop(7)}"
  }

  private def withShowFeatureInvite(arn: Arn)(f: Boolean => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    agentPermissionsConnector.isShownPrivateBetaInvite.flatMap(f)
  }

}
