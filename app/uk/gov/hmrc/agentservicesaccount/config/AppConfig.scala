/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.config

import com.google.inject.{Inject, Singleton}
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.{Configuration, Environment, Logging, Mode}
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.helper.urlEncode
import scala.concurrent.duration.Duration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig, env:Environment) extends Logging {

  val appName = "agent-services-account-frontend"

  def isTest: Boolean = env.mode == Mode.Test

  private def getConfString(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config $key not found"))

  private def getString(key: String) = servicesConfig.getString(key)

  private def getBoolean(key: String) = servicesConfig.getBoolean(key)

  private def getInt(key: String) = servicesConfig.getInt(key)

  private def baseUrl(key: String) = servicesConfig.baseUrl(key)

  val customDimension: String = getString("customDimension")

  val authBaseUrl: String = baseUrl("auth")

  val ssoBaseUrl: String = baseUrl("sso")

  val acaBaseUrl: String = baseUrl("agent-client-authorisation")

  val emailBaseUrl: String = baseUrl("email")

  val asaFrontendExternalUrl: String = getConfString("agent-services-account-frontend.external-url")

  val companyAuthFrontendExternalUrl: String = getConfString("company-auth-frontend.external-url")
  val signOutPath: String = getConfString("company-auth-frontend.sign-out.path")
  val signInPath: String = getConfString("company-auth-frontend.sign-in.path")
  val signOutContinueUrl: String = getConfString("company-auth-frontend.sign-out.continue-url")
  lazy val signOut: String = s"$companyAuthFrontendExternalUrl$signOutPath"

  lazy val continueFromGGSignIn = s"$companyAuthFrontendExternalUrl$signInPath?continue=${urlEncode(s"$asaFrontendExternalUrl/agent-services-account")}"

  val agentMappingFrontendExternalUrl: String = getConfString("agent-mapping-frontend.external-url")

  val agentMappingUrl: String = s"$agentMappingFrontendExternalUrl/agent-mapping/start"

  val agentSubscriptionFrontendExternalUrl: String = getConfString("agent-subscription-frontend.external-url")

  val agentSubscriptionFrontendUrl: String = s"$agentSubscriptionFrontendExternalUrl/agent-subscription/start"

  val agentInvitationsFrontendExternalUrl: String = getConfString("agent-invitations-frontend.external-url")

  val agentInvitationsFrontendUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/agents"

  val agentInvitationsFrontendClientTypeUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/agents/client-type"

  val incomeTaxSubscriptionAgentFrontendExternalUrl: String = getConfString("income-tax-subscription-frontend.external-url")

  val incomeTaxSubscriptionAgentFrontendUrl: String = s"$incomeTaxSubscriptionAgentFrontendExternalUrl/report-quarterly/income-and-expenses/view/agents"

  val agentInvitationsTrackUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/track"

  val agentInvitationsCancelAuthUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/agents/cancel-authorisation/client-type"

  val taxHistoryFrontendExternalUrl: String = getConfString("tax-history-frontend.external-url")

  val taxHistoryFrontendUrl: String = s"$taxHistoryFrontendExternalUrl/tax-history/select-client"

  val userManagementExternalUrl: String = getString("user-management.external-url")
  val manageUsersStartPath: String = getString("user-management.manage-users")
  val addUserStartPath: String = getString("user-management.add-user")
  lazy val manageUsersUrl: String = s"$userManagementExternalUrl$manageUsersStartPath"
  lazy val addUserUrl: String = s"$userManagementExternalUrl$addUserStartPath"

  val vatExternalUrl: String = getConfString("vat-agent-client-lookup-frontend.external-url")
  val vatStartPath: String = getConfString("vat-agent-client-lookup-frontend.start.path")
  val vatUrl = s"$vatExternalUrl$vatStartPath"

  val vrsExternalUrl: String = getString("vat-registration-service.external-url")
  val vrsPath: String = getString("vat-registration-service.path")
  val vrsUrl = s"$vrsExternalUrl$vrsPath"

  val timeoutDialogTimeout: Int = getInt("timeoutDialog.timeout")
  val timeoutDialogCountdown: Int = getInt("timeoutDialog.countdown")

  val runMode: Option[String] = config.getOptional[String]("run.mode")
  val isDevEnv: Boolean = if (env.mode.equals(Mode.Test)) false else runMode.forall(Mode.Dev.toString.equals)

  val betaFeedbackUrl: String = getString("betaFeedbackUrl")

  val hmrcOnlineGuidanceLink: String = getString("hmrcOnlineGuidanceLink")
  val hmrcOnlineSignInLink: String = getString("hmrcOnlineSignInLink")

  def signOutUrlWithSurvey(surveyKey: String): String = s"$companyAuthFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl + surveyKey)}"

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.AgentServicesLanguageController.switchToLanguage(lang)

  val govUkGuidanceChangeDetails: String = getString("govUkGuidanceChangeDetails")

  val afiBaseUrl: String = baseUrl("agent-fi-relationship")

  lazy val sessionCacheExpiryDuration: Duration = servicesConfig.getDuration("mongodb.cache.expiry")

  // feature flags
  val feedbackSurveyServiceSelect: Boolean = getBoolean("features.enable-feedback-survey-service-select")
  val agentSuspensionEnabled: Boolean = getBoolean("features.enable-agent-suspension")
  val enablePpt: Boolean = getBoolean("features.enable-ppt")
  val granPermsEnabled: Boolean = getBoolean("features.enable-gran-perms")

  //Gran Perms
  val agentPermissionsBaseUrl: String = baseUrl("agent-permissions")
  val agentUserClientDetailsBaseUrl: String = baseUrl("agent-user-client-details")
  val agentPermissionsFrontendExternalUrl: String = getConfString("agent-permissions-frontend.external-url")
  val agentPermissionsFrontendOptInPath: String = getConfString("agent-permissions-frontend.optin-start-path")
  val agentPermissionsFrontendOptOutPath: String = getConfString("agent-permissions-frontend.optout-start-path")
  val agentPermissionsFrontendManageAccessGroupsPath: String = getConfString("agent-permissions-frontend.manage-access-groups-path")
  val agentPermissionsFrontendManageClientsPath: String = getConfString("agent-permissions-frontend.manage-clients-path")
  val agentPermissionsFrontendManageTeamMembersPath: String = getConfString("agent-permissions-frontend.manage-team-members-path")
  val agentPermissionsFrontendGroupsCreatePath: String = getConfString("agent-permissions-frontend.create-access-group-path")
  val agentPermissionsFrontendUnassignedClientsPath: String = getConfString("agent-permissions-frontend.unassigned-clients-path")
  val granPermsMaxClientCount: Int = getInt("gran-perms-max-client-count")

  val agentPermissionsOptInUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendOptInPath"
  val agentPermissionsOptOutUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendOptOutPath"
  val agentPermissionsCreateAccessGroupUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendGroupsCreatePath"
  val agentPermissionsManageAccessGroupsUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendManageAccessGroupsPath"
  val agentPermissionsUnassignedClientsUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendUnassignedClientsPath"
  val agentPermissionsManageClientUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendManageClientsPath"
  val agentPermissionsManageTeamMembersUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendManageTeamMembersPath"

  // Assistant users are read only
  val agentPermissionsFrontendAssistantViewGroupClientsUrl = s"$agentPermissionsFrontendExternalUrl/agent-permissions/your-account/group-clients"
  val agentPermissionsFrontendAssistantViewUnassignedClientsUrl = s"$agentPermissionsFrontendExternalUrl/agent-permissions/your-account/other-clients"
}
