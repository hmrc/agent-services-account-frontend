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

package uk.gov.hmrc.agentservicesaccount.auth

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import uk.gov.hmrc.agentservicesaccount.config.RequiredConfigString
import views.html.helper.urlEncode

@Singleton
class SignOutUrl @Inject() (override val configuration: Configuration) extends RequiredConfigString {
  private def signOutBaseExternalUrl = getConfigString("microservice.services.company-auth-frontend.external-url")
  private def signOutPath = getConfigString("microservice.services.company-auth-frontend.sign-out.path")
  private def signOutContinueUrl = getConfigString("microservice.services.company-auth-frontend.sign-out.continue-url")

  def signOutUrl: String = s"$signOutBaseExternalUrl$signOutPath?continue=${urlEncode(signOutContinueUrl)}"
}
