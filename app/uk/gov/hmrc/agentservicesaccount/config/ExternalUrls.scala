/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import views.html.helper.urlEncode

/**
  * Externally accessible URLs, i.e. URLs for use by web browsers, not URLs for use by microservices.
  */
@Singleton
class ExternalUrls @Inject() (override val configuration: Configuration) extends RequiredConfigString {
  private lazy val companyAuthFrontendExternalUrl = getConfigString("microservice.services.company-auth-frontend.external-url")
  private lazy val signOutPath = getConfigString("microservice.services.company-auth-frontend.sign-out.path")
  private lazy val signOutContinueUrl = getConfigString("microservice.services.company-auth-frontend.sign-out.continue-url")
  lazy val signOutUrl: String = s"$companyAuthFrontendExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl)}"

  private lazy val mappingExternalUrl = getConfigString("microservice.services.agent-mapping-frontend.external-url")
  private lazy val mappingStartPath= getConfigString("microservice.services.agent-mapping-frontend.start.path")
  lazy val agentMappingUrl: String = s"$mappingExternalUrl$mappingStartPath"

  private lazy val subscriptionExternalUrl = getConfigString("microservice.services.agent-subscription-frontend.external-url")
  private lazy val subscriptionStartPath = getConfigString("microservice.services.agent-subscription-frontend.start.path")
  lazy val agentSubscriptionUrl: String = s"$subscriptionExternalUrl$subscriptionStartPath"
}
