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

import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito.when
import play.api.Configuration
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.support.ResettingMockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import views.html.helper.urlEncode

class ExternalUrlsSpec extends UnitSpec with ResettingMockitoSugar {

  // deliberately different to the values in application.conf to test that the code reads the configuration rather than hard coding values
  val companyAuthFrontendExternalBaseUrl = "http://gg-sign-in-host:1234"
  val ggSignInPath = "/blah/sign-in"
  val ggSignOutPath = "/blah/sign-out"
  val ggSignOutContinueUrl = "http://www.example.com/foo"
  val externalUrl = "https://localhost:9401"
  val completeGgSignInUrl = s"$companyAuthFrontendExternalBaseUrl$ggSignInPath?continue=${urlEncode(externalUrl + routes.AgentServicesController.root())}"
  def completeGgSignInUrlWithExternalContinue(url: String) = s"$companyAuthFrontendExternalBaseUrl$ggSignInPath?continue=${urlEncode(externalUrl + routes.AgentServicesController.root() + "?continue="+urlEncode(url))}"
  val completeGgSignOutUrl = s"$companyAuthFrontendExternalBaseUrl$ggSignOutPath?continue=${urlEncode(ggSignOutContinueUrl)}"

  val configuration = resettingMock[Configuration]
  val externalUrls = new ExternalUrls(configuration)

  "signInUrl" should {
    "return the sign in URL including continue parameter" in {
      mockConfig()
      externalUrls.signInUrl() shouldBe completeGgSignInUrl
    }

    "return the sign in URL including continue parameter with external continue param" in {
      mockConfig()
      externalUrls.signInUrl(Some("/baz-foo/bar?abc=xyz")) shouldBe completeGgSignInUrlWithExternalContinue("/baz-foo/bar?abc=xyz")
    }
  }

  "signOutUrl" should {
    "return the sign out URL including continue parameter" in {
      mockConfig()
      externalUrls.signOutUrl shouldBe completeGgSignOutUrl
    }
  }

  private def mockConfig(): Unit = {
    mockConfigString("microservice.services.company-auth-frontend.external-url", companyAuthFrontendExternalBaseUrl)
    mockConfigString("microservice.services.company-auth-frontend.sign-in.path", ggSignInPath)
    mockConfigString("microservice.services.company-auth-frontend.sign-out.path", ggSignOutPath)
    mockConfigString("microservice.services.company-auth-frontend.sign-out.continue-url", ggSignOutContinueUrl)
    mockConfigString("microservice.services.agent-services-account-frontend.external-url", externalUrl)
  }

  private def mockConfigString(path: String, configValue: String) = {
    when(configuration.getString(eqs(path), any[Option[Set[String]]]))
      .thenReturn(Some(configValue))
  }
}
