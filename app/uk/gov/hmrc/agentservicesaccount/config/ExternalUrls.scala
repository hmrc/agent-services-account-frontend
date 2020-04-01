/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import views.html.helper.urlEncode

/**
  * Externally accessible URLs, i.e. URLs for use by web browsers, not URLs for use by microservices.
  */
@Singleton
class ExternalUrls @Inject() (override val configuration: Configuration) extends RequiredConfig {
  private lazy val agentServicesAccountBaseUrl = getConfigString("microservice.services.agent-services-account-frontend.external-url")
  private lazy val companyAuthFrontendExternalUrl = getConfigString("microservice.services.company-auth-frontend.external-url")
  private lazy val signOutPath = getConfigString("microservice.services.company-auth-frontend.sign-out.path")
  private lazy val signInPath = getConfigString("microservice.services.company-auth-frontend.sign-in.path")
  lazy val continueFromGGSignIn = s"$companyAuthFrontendExternalUrl$signInPath?continue=${urlEncode(s"$agentServicesAccountBaseUrl/agent-services-account")}"
  private lazy val signOutContinueUrl = getConfigString("microservice.services.company-auth-frontend.sign-out.continue-url")
  lazy val signOutUrlWithSurvey: String = s"$companyAuthFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl)}"
  lazy val signOut: String = s"$companyAuthFrontendExternalUrl$signOutPath"

  private lazy val mappingExternalUrl = getConfigString("microservice.services.agent-mapping-frontend.external-url")
  private lazy val mappingStartPath= getConfigString("microservice.services.agent-mapping-frontend.start.path")
  lazy val agentMappingUrl: String = s"$mappingExternalUrl$mappingStartPath"

  private lazy val subscriptionExternalUrl = getConfigString("microservice.services.agent-subscription-frontend.external-url")
  private lazy val subscriptionStartPath = getConfigString("microservice.services.agent-subscription-frontend.start.path")
  lazy val agentSubscriptionUrl: String = s"$subscriptionExternalUrl$subscriptionStartPath"

  private lazy val invitationsExternalUrl = getConfigString("microservice.services.agent-invitations-frontend.external-url")
  private lazy val invitationsStartPath = getConfigString("microservice.services.agent-invitations-frontend.start.path")
  private lazy val invitationsTrackPath = getConfigString("microservice.services.agent-invitations-frontend.track.path")
  private lazy val cancelAuthPath = getConfigString("microservice.services.agent-invitations-frontend.cancel-auth.path")
  lazy val agentInvitationsUrl: String = s"$invitationsExternalUrl$invitationsStartPath"
  lazy val agentInvitationsTrackUrl: String = s"$invitationsExternalUrl$invitationsTrackPath"
  lazy val agentCancelAuthUrl: String = s"$invitationsExternalUrl$cancelAuthPath"

  private lazy val agentAfiExternalUrl = getConfigString("microservice.services.tax-history-frontend.external-url")
  private lazy val agentAfiStartPath = getConfigString("microservice.services.tax-history-frontend.start.path")
  lazy val agentAfiUrl: String = s"$agentAfiExternalUrl$agentAfiStartPath"

  private lazy val userManangementExternalUrl = getConfigString("user-management.external-url")
  private lazy val manageUsersStartPath = getConfigString("user-management.manage-users")
  lazy val manageUsersUrl: String = s"$userManangementExternalUrl$manageUsersStartPath"

  private lazy val addUserStartPath = getConfigString("user-management.add-user")
  lazy val addUserUrl: String = s"$userManangementExternalUrl$addUserStartPath"

  private lazy val vatThroughSoftwareExternalUrl = getConfigString("microservice.services.vat-agent-client-lookup-frontend.external-url")
  private lazy val vatThroughSoftwareStartPath = getConfigString("microservice.services.vat-agent-client-lookup-frontend.start.path")
  lazy val vatThroughSoftwareUrl: String = s"$vatThroughSoftwareExternalUrl$vatThroughSoftwareStartPath"

  lazy val timeout: Int = getConfigInteger("timeoutDialog.timeout-seconds")
  lazy val countdown: Int = getConfigInteger("timeoutDialog.timeout-countdown-seconds")

  lazy val languageToggle: Boolean = getConfigBoolean("features.enable-welsh-toggle")

  private lazy val cgtPropertyDisposalsExternalUrl = getConfigString("microservice.services.cgt-property-disposals-frontend.external-url")
  private lazy val cgtPropertyDisposalsStartPath = getConfigString("microservice.services.cgt-property-disposals-frontend.start.path")
  lazy val cgtPropertyDisposalsUrl = s"$cgtPropertyDisposalsExternalUrl$cgtPropertyDisposalsStartPath"

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage = (lang: String) => routes.AgentServicesLanguageController.switchToLanguage(lang)
}
