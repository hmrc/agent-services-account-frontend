/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.support.ResettingMockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import views.html.helper.urlEncode

class ExternalUrlsSpec extends UnitSpec with ResettingMockitoSugar {

  // deliberately different to the values in application.conf to test that the code reads the configuration rather than hard coding values
  val companyAuthFrontendExternalBaseUrl = "http://gg-sign-in-host:1234"
  val subscriptionPath = "/blah/sign-in"
  val ggSignOutPath = "/blah/sign-out"
  val ggSignOutContinueUrl = "http://www.example.com"
  val externalUrl = "https://localhost:9401"

  val completeAgentSubscriptionGgSignInUrl = s"$externalUrl$subscriptionPath"

  val completeGgSignOutUrl = s"$companyAuthFrontendExternalBaseUrl$ggSignOutPath?continue=${urlEncode(ggSignOutContinueUrl)}"

  val mappingExternalUrl = "http://www.example.com/foo"
  val mappingStartPath = "/foo"
  val agentMappingUrl: String = s"$mappingExternalUrl$mappingStartPath"

  val agentInvitationsExternalUrl = "http://www.example.com/invitations"
  val agentInvitationsStartPath = "/foo"
  val agentInvitationsUrl: String = s"$agentInvitationsExternalUrl$agentInvitationsStartPath"

  val agentInvitationsTrackPath = "/track/"
  val agentInvitationsTrackUrl: String = s"$agentInvitationsExternalUrl$agentInvitationsTrackPath"

  val agentAfiExternalUrl = "http://www.example.com/afi"
  val agentAfiStartPath = "/foo"
  val agentAfiUrl: String = s"$agentAfiExternalUrl$agentAfiStartPath"

  val configuration: Configuration = resettingMock[Configuration]
  val externalUrls = new ExternalUrls(configuration)

  "signInUrl" should {
    "return the sign in URL" in {
      mockConfig()
      externalUrls.agentSubscriptionUrl shouldBe completeAgentSubscriptionGgSignInUrl
    }
  }

  "signOutUrl" should {
    "return the sign out URL including continue parameter" in {
      mockConfig()
      externalUrls.signOutUrl shouldBe completeGgSignOutUrl
    }
  }

  "agentMappingUrl" should {
    "return the agent mapping frontend URL" in {
      mockConfig()
      externalUrls.agentMappingUrl shouldBe agentMappingUrl
    }
  }

  "agentInvitationsUrl" should {
    "return the agent invitation frontend URL" in {
      mockConfig()
      externalUrls.agentInvitationsUrl shouldBe agentInvitationsUrl
    }
  }

  "agentInvitationsTrackUrl" should {
    "return the agent invitation frontend track URL" in {
      mockConfig()
      externalUrls.agentInvitationsTrackUrl shouldBe agentInvitationsTrackUrl
    }
  }

  "agentAfiUrl" should {
    "return the agent for individuals frontend URL" in {
      mockConfig()
      externalUrls.agentAfiUrl shouldBe agentAfiUrl
    }
  }

  private def mockConfig(): Unit = {
    mockConfigString("microservice.services.company-auth-frontend.external-url", companyAuthFrontendExternalBaseUrl)
    mockConfigString("microservice.services.company-auth-frontend.sign-in.path", subscriptionPath)
    mockConfigString("microservice.services.company-auth-frontend.sign-out.path", ggSignOutPath)
    mockConfigString("microservice.services.company-auth-frontend.sign-out.continue-url", ggSignOutContinueUrl)
    mockConfigString("microservice.services.agent-mapping-frontend.external-url", mappingExternalUrl)
    mockConfigString("microservice.services.agent-mapping-frontend.start.path", mappingStartPath)
    mockConfigString("microservice.services.agent-invitations-frontend.external-url", agentInvitationsExternalUrl)
    mockConfigString("microservice.services.agent-invitations-frontend.start.path", agentInvitationsStartPath)
    mockConfigString("microservice.services.agent-invitations-frontend.track.path", agentInvitationsTrackPath)
    mockConfigString("microservice.services.tax-history-frontend.external-url", agentAfiExternalUrl)
    mockConfigString("microservice.services.tax-history-frontend.start.path", agentAfiStartPath)
    mockConfigString("microservice.services.agent-subscription-frontend.external-url", externalUrl)
    mockConfigString("microservice.services.agent-subscription-frontend.start.path", subscriptionPath)
  }

  private def mockConfigString(path: String, configValue: String) = {
    when(configuration.getString(eqs(path), any[Option[Set[String]]]))
      .thenReturn(Some(configValue))
  }
}
