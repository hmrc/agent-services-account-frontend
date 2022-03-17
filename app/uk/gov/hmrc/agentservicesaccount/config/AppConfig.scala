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

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig, env:Environment) extends Logging{

  val appName = "agent-services-account-frontend"

  private def getConfString(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config $key not found"))

  private def getString(key: String) = servicesConfig.getString(key)
  private def getBoolean(key: String) = servicesConfig.getBoolean(key)
  private def getInt(key: String) = servicesConfig.getInt(key)

  private def baseUrl(key: String) = servicesConfig.baseUrl(key)

  val authBaseUrl = baseUrl("auth")

  val ssoBaseUrl = baseUrl("sso")

  val acaBaseUrl = baseUrl("agent-client-authorisation")

  val customDimension = getString("customDimension")

  val asaFrontendExternalUrl = getConfString("agent-services-account-frontend.external-url")

  val companyAuthFrontendExternalUrl = getConfString("company-auth-frontend.external-url")
  val signOutPath = getConfString("company-auth-frontend.sign-out.path")
  val signInPath = getConfString("company-auth-frontend.sign-in.path")
  val signOutContinueUrl = getConfString("company-auth-frontend.sign-out.continue-url")
  lazy val signOut: String = s"$companyAuthFrontendExternalUrl$signOutPath"

  lazy val continueFromGGSignIn = s"$companyAuthFrontendExternalUrl$signInPath?continue=${urlEncode(s"$asaFrontendExternalUrl/agent-services-account")}"

  val agentMappingFrontendExternalUrl = getConfString("agent-mapping-frontend.external-url")

  val agentMappingUrl: String = s"$agentMappingFrontendExternalUrl/agent-mapping/start"

  val agentSubscriptionFrontendExternalUrl = getConfString("agent-subscription-frontend.external-url")

  val agentSubscriptionFrontendUrl: String = s"$agentSubscriptionFrontendExternalUrl/agent-subscription/start"

  val agentInvitationsFrontendExternalUrl = getConfString("agent-invitations-frontend.external-url")

  val agentInvitationsFrontendUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/agents"

  val agentInvitationsFrontendClientTypeUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/agents/client-type"

  val incomeTaxSubscriptionAgentFrontendExternalUrl = getConfString("income-tax-subscription-frontend.external-url")

  val incomeTaxSubscriptionAgentFrontendUrl: String = s"$incomeTaxSubscriptionAgentFrontendExternalUrl/report-quarterly/income-and-expenses/view/agents"

  val agentInvitationsTrackUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/track"

  val agentInvitationsCancelAuthUrl: String = s"$agentInvitationsFrontendExternalUrl/invitations/agents/cancel-authorisation/client-type"

  val taxHistoryFrontendExternalUrl = getConfString("tax-history-frontend.external-url")

  val taxHistoryFrontendUrl: String = s"$taxHistoryFrontendExternalUrl/tax-history/select-client"

  val userManagementExternalUrl =  getString("user-management.external-url")
  val manageUsersStartPath =  getString("user-management.manage-users")
  val addUserStartPath =  getString("user-management.add-user")
  lazy val manageUsersUrl: String = s"$userManagementExternalUrl$manageUsersStartPath"
  lazy val addUserUrl: String = s"$userManagementExternalUrl$addUserStartPath"

  val vatExternalUrl  = getConfString("vat-agent-client-lookup-frontend.external-url")
  val vatStartPath  = getConfString("vat-agent-client-lookup-frontend.start.path")
  val vatUrl = s"$vatExternalUrl$vatStartPath"

  val agentSuspensionEnabled = getBoolean("features.enable-agent-suspension")
  val irvAllowlistEnabled = getBoolean("features.enable-irv-allowlist")
  val ITSAAccordionEnabled = getBoolean("features.enable-ITSA-Accordion")
  val HelpAndGuidanceEnabled = getBoolean("features.enable-help-and-guidance")

  val timeoutDialogTimeout = getInt("timeoutDialog.timeout")
  val timeoutDialogCountdown = getInt("timeoutDialog.countdown")

  val runMode = config.getOptional[String]("run.mode")
  val isDevEnv = if (env.mode.equals(Mode.Test)) false else runMode.forall(Mode.Dev.toString.equals)

  val betaFeedbackUrl = getString("betaFeedbackUrl")

  val hmrcOnlineGuidanceLink = getString("hmrcOnlineGuidanceLink")
  val hmrcOnlineSignInLink = getString("hmrcOnlineSignInLink")

  val feedbackSurveyServiceSelect = getBoolean("features.enable-feedback-survey-service-select")

  val enablePpt = getBoolean("features.enable-ppt")

  def signOutUrlWithSurvey(surveyKey: String): String = s"$companyAuthFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl + surveyKey)}"

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call = (lang: String) => routes.AgentServicesLanguageController.switchToLanguage(lang)

  val govUkGuidanceChangeDetails = getString("govUkGuidanceChangeDetails")
}
