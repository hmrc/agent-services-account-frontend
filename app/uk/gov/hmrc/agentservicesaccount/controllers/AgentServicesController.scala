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
import uk.gov.hmrc.agents.accessgroups.optin.{OptedInReady, OptinStatus}
import uk.gov.hmrc.agentservicesaccount.actions.CallOps._
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector, AgentPermissionsConnector, AgentUserClientDetailsConnector}
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatus
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses._
import uk.gov.hmrc.agentservicesaccount.views.html.pages._
import uk.gov.hmrc.agentservicesaccount.views.html.pages.assistant.{administrators, your_account}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.agentservicesaccount.controllers.amls.{routes => amlsRoutes}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentServicesController @Inject()(authActions: AuthActions,
                                        actions: Actions,
                                        agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                                        agentPermissionsConnector: AgentPermissionsConnector,
                                        agentUserClientDetailsConnector: AgentUserClientDetailsConnector,
                                        agentAssuranceConnector: AgentAssuranceConnector,
                                        manage_account: manage_account,
                                        administrators_html: administrators,
                                        your_account: your_account,
                                        asaDashboard: asa_dashboard,
                                        account_details: account_details,
                                        helpView: help
                                       )(implicit appConfig: AppConfig,
                                         cc: MessagesControllerComponents,
                                         ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with Logging {

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
          agentInfo.isAdmin)).addingToSession(toReturnFromMapping())
    }
  }


  private def toReturnFromMapping()(implicit request: Request[AnyContent]) = {
    val sessionKeyUsedInMappingService = "OriginForMapping"
    sessionKeyUsedInMappingService -> localFriendlyUrl(env)(request.path, request.host)
  }


  val manageAccount: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    val agentInfo = request.agentInfo
    if (agentInfo.isAdmin) {
      if (appConfig.granPermsEnabled) {
        agentPermissionsConnector.isArnAllowed flatMap {
          case true =>
            for {
              maybeOptinStatus <- agentPermissionsConnector.getOptinStatus(agentInfo.arn)
              mGroups <- agentPermissionsConnector.getGroupsSummaries(agentInfo.arn)
              hasAnyGroups = mGroups.exists(_.groups.nonEmpty)
              amlsStatus <- agentAssuranceConnector.getAmlsStatus(agentInfo.arn)
              (amlsKey, amlsLinkHref) = getAmlsStatusLink(amlsStatus)
            } yield {
              maybeOptinStatus.foreach(syncEacdIfOptedIn(agentInfo.arn, _))
              Ok(manage_account(Some(amlsKey), Some(amlsLinkHref), maybeOptinStatus, hasAnyGroups))
            }
          case false =>
            Future successful Ok(manage_account())
        }
      } else {
        Future.successful(Ok(manage_account()))
      }
    } else {
      if (appConfig.granPermsEnabled) {
        Future.successful(Redirect(routes.AgentServicesController.yourAccount))
      } else {
        Future.successful(Forbidden)
      }

    }
  }

  private def getAmlsStatusLink(amlsStatus: AmlsStatus): (String, String) = {
    amlsStatus match {
      case NoAmlsDetailsNonUK =>
        ("manage.account.amls.view",amlsRoutes.ViewDetailsController.showPage.url)
      case ValidAmlsNonUK =>
        ("manage.account.amls.view",amlsRoutes.ViewDetailsController.showPage.url)
      case NoAmlsDetailsUK =>
        ("manage.account.amls.add",amlsRoutes.ViewDetailsController.showPage.url)
      case ValidAmlsDetailsUK =>
        ("manage.account.amls.view",amlsRoutes.ViewDetailsController.showPage.url)
      case ExpiredAmlsDetailsUK | PendingAmlsDetails | PendingAmlsDetailsRejected=>
        ("manage.account.amls.update",amlsRoutes.ViewDetailsController.showPage.url)
    }
  }

  private def syncEacdIfOptedIn(arn: Arn, optinStatus: OptinStatus)(implicit hc: HeaderCarrier) = {
    if (optinStatus == OptedInReady) agentPermissionsConnector.syncEacd(arn, fullSync = true)
    else Future.successful(())
  }

  val yourAccount: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    if (!request.agentInfo.isAdmin) {
      if (appConfig.granPermsEnabled) {
        request.agentInfo.credentials.fold(Ok(your_account(None)).toFuture) { credentials =>
          agentPermissionsConnector
            .isOptedIn(request.agentInfo.arn)
            .flatMap(isOptedIn =>
              if (isOptedIn)
                agentPermissionsConnector.getGroupsForTeamMember(request.agentInfo.arn, credentials.providerId)
                  .map(maybeSummaries => Ok(your_account(Some(request.agentInfo), maybeSummaries)))
              else
                Ok(your_account(Some(request.agentInfo), None, optedIn = false)).toFuture
            )
        }
      } else {
        Ok(your_account(None)).toFuture
      }
    } else {
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
              .filterNot(_.credentialRole.getOrElse("").equals("Assistant")))
        )
      }
  }


  val accountDetails: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    agentClientAuthorisationConnector.getAgencyDetails().map(agencyDetails =>
      Ok(account_details(agencyDetails, request.agentInfo.isAdmin))
    )

  }

  val showHelp: Action[AnyContent] = actions.authActionCheckSuspend { implicit request =>

    Ok(helpView(request.agentInfo.isAdmin))

  }

  private def formatArn(arn: Arn): String = {
    val arnStr = arn.value
    s"${arnStr.take(4)} ${arnStr.slice(4, 7)} ${arnStr.drop(7)}"
  }

  private def withShowFeatureInvite(arn: Arn)(f: Boolean => Future[Result])(implicit request: Request[_]): Future[Result] = {
    agentPermissionsConnector.isShownPrivateBetaInvite.flatMap(f)
  }

}
