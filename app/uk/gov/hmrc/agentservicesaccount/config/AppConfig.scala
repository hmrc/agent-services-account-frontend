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

package uk.gov.hmrc.agentservicesaccount.config

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.i18n.Lang
import play.api.mvc.Call
import play.api.Configuration
import play.api.Environment
import play.api.Logging
import play.api.Mode
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedInNotReady
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedOutEligible
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptinStatus
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.helper.urlEncode

import scala.concurrent.duration.Duration

@Singleton
class AppConfig @Inject() (
  config: Configuration,
  servicesConfig: ServicesConfig,
  env: Environment
)
extends Logging {

  val appName = "agent-services-account-frontend"

  def isTest: Boolean = env.mode == Mode.Test

  private def getConfString(key: String) = servicesConfig.getConfString(key, throw new RuntimeException(s"config $key not found"))

  private def getString(key: String) = servicesConfig.getString(key)

  private def getBoolean(key: String) = servicesConfig.getBoolean(key)

  private def getInt(key: String) = servicesConfig.getInt(key)

  private def baseUrl(key: String) = servicesConfig.baseUrl(key)

  val agentServicesAccountBaseUrl: String = baseUrl("agent-services-account")

  val agentAssuranceBaseUrl: String = baseUrl("agent-assurance")

  val continueUrl: String = getString("login.continue")

  val emailBaseUrl: String = baseUrl("email")

  val addressLookupBaseUrl: String = baseUrl("address-lookup-frontend")

  val emailVerificationBaseUrl: String = baseUrl("email-verification")
  val emailVerificationFrontendBaseUrl: String = getString("microservice.services.email-verification-frontend.external-url")

  val deskproServiceName: String = getString("contact-frontend.serviceId")
  val accessibilityStatementUrl: String = getString("accessibility-statement.service-path")

  val suspendedContactDetailsSendToAddress: String = getString("suspendedContactDetails.sendToAddress")
  val suspendedContactDetailsSendEmail: Boolean = getBoolean("suspendedContactDetails.sendEmail")

  val asaFrontendExternalUrl: String = getConfString("agent-services-account-frontend.external-url")

  private val basGatewayFrontendExternalUrl: String = getConfString("bas-gateway-frontend.external-url")
  private val signOutPath: String = getConfString("bas-gateway-frontend.sign-out.path")
  private val signInPath: String = getConfString("bas-gateway-frontend.sign-in.path")
  private val signOutContinueUrl: String = getConfString("bas-gateway-frontend.sign-out.continue-url")
  lazy val signOut: String = s"$basGatewayFrontendExternalUrl$signOutPath"
  lazy val signInUrl: String = s"$basGatewayFrontendExternalUrl$signInPath"

  def signOutUrlWithSurvey(surveyKey: String): String = s"$basGatewayFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl + surveyKey)}"

  val agentMappingFrontendExternalUrl: String = getConfString("agent-mapping-frontend.external-url")

  val agentMappingUrl: String = s"$agentMappingFrontendExternalUrl/agent-mapping/start"

  val agentSubscriptionFrontendExternalUrl: String = getConfString("agent-subscription-frontend.external-url")

  val agentSubscriptionFrontendUrl: String = s"$agentSubscriptionFrontendExternalUrl/agent-subscription/start"

  val agentClientRelationshipsFrontendExternalUrl: String = getConfString("agent-client-relationships-frontend.external-url")
  private val agentClientRelationshipsFrontendTrackPath: String = getConfString("agent-client-relationships-frontend.track.path")
  private val agentClientRelationshipsFrontendInvitationsPath: String = getConfString("agent-client-relationships-frontend.invitations.path")
  private val agentClientRelationshipsFrontendDeauthPath: String = getConfString("agent-client-relationships-frontend.deauth.path")
  val agentClientRelationshipsFrontendTrackUrl: String = s"$agentClientRelationshipsFrontendExternalUrl$agentClientRelationshipsFrontendTrackPath"
  val agentClientRelationshipsFrontendInvitationsUrl: String = s"$agentClientRelationshipsFrontendExternalUrl$agentClientRelationshipsFrontendInvitationsPath"
  val agentClientRelationshipsFrontendDeauthUrl: String = s"$agentClientRelationshipsFrontendExternalUrl$agentClientRelationshipsFrontendDeauthPath"

  val incomeTaxSubscriptionAgentFrontendExternalUrl: String = getConfString("income-tax-subscription-frontend.external-url")

  val incomeTaxSubscriptionAgentFrontendUrl: String = s"$incomeTaxSubscriptionAgentFrontendExternalUrl/report-quarterly/income-and-expenses/view/agents"

  val incomeTaxSubscriptionSignupClientUrl: String = s"$incomeTaxSubscriptionAgentFrontendExternalUrl/report-quarterly/income-and-expenses/sign-up/client/"

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
  val isDevEnv: Boolean =
    if (env.mode.equals(Mode.Test))
      false
    else
      runMode.forall(Mode.Dev.toString.equals)

  val userResearchLink: String = getString("userResearchLink")

  val hmrcOnlineGuidanceLink: String = getString("hmrcOnlineGuidanceLink")
  val hmrcOnlineSignInLink: String = getString("hmrcOnlineSignInLink")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.AgentServicesLanguageController.switchToLanguage(lang)

  val govUkGuidanceChangeDetails: String = getString("govUkGuidanceChangeDetails")
  val govUkItsaAsAnAgent: String = getString("govUkItsaAsAnAgent")
  val govUkItsaSignUpClient: String = getString("govUkItsaSignUpClient")

  lazy val sessionCacheExpiryDuration: Duration = servicesConfig.getDuration("mongodb.cache.expiry")
  val pendingChangeTTL: Long = getInt("mongodb.desi-details.lockout-period").toLong

  // feature flags
  val feedbackSurveyServiceSelect: Boolean = getBoolean("features.enable-feedback-survey-service-select")
  val enableChangeContactDetails: Boolean = getBoolean("features.enable-change-contact-details")
  val granPermsEnabled: Boolean = getBoolean("features.enable-gran-perms")
  val enableCbc: Boolean = getBoolean("features.enable-cbc")
  val enableBackendPCRDatabase: Boolean = getBoolean("features.enable-backend-pcr-database")

  // Gran Perms
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
  def getOptinInOrOutUrl(status: OptinStatus): String = {
    status match {
      case OptedOutEligible => agentPermissionsOptInUrl
      case OptedInNotReady => agentPermissionsOptOutUrl
      case _ => throw new IllegalArgumentException("Invalid OptinStatus")
    }
  }
  val agentPermissionsCreateAccessGroupUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendGroupsCreatePath"
  val agentPermissionsManageAccessGroupsUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendManageAccessGroupsPath"
  val agentPermissionsUnassignedClientsUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendUnassignedClientsPath"
  val agentPermissionsManageClientUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendManageClientsPath"
  val agentPermissionsManageTeamMembersUrl = s"$agentPermissionsFrontendExternalUrl$agentPermissionsFrontendManageTeamMembersPath"

  // Assistant users are read only
  val agentPermissionsFrontendAssistantViewGroupClientsUrl = s"$agentPermissionsFrontendExternalUrl/agent-permissions/your-account/group-clients"
  val agentPermissionsFrontendAssistantViewUnassignedClientsUrl = s"$agentPermissionsFrontendExternalUrl/agent-permissions/your-account/other-clients"

  // DMS queue name
  val dmsSubmissionClassificationType: String = servicesConfig.getString("microservice.services.dms-submission.contact-details-submission.classificationType")

  private val pillar2SubmissionFrontendExternalUrl: String = getString("pillar2-submission-frontend.external-url")

  val pillar2StartUrl = s"$pillar2SubmissionFrontendExternalUrl/report-pillar2-top-up-taxes/asa/input-pillar-2-id"
  val pillar2GuidanceUrl = "https://www.gov.uk/guidance/report-pillar-2-top-up-taxes"

}
