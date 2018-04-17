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

package uk.gov.hmrc.agentservicesaccount.views

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.views.html.error_template_Scope0.error_template_Scope1.error_template
import uk.gov.hmrc.agentservicesaccount.views.html.main_template_Scope0.main_template_Scope1.main_template
import uk.gov.hmrc.agentservicesaccount.views.html.pages.agent_services_account_Scope0.agent_services_account_Scope1.agent_services_account
import uk.gov.hmrc.play.test.UnitSpec

class ViewsSpec extends UnitSpec with GuiceOneAppPerTest {

  "error_template view" should {

    "render title, heading and message" in new App {
      implicit lazy val app: Application = new GuiceApplicationBuilder().build()
      val configuration = app.injector.instanceOf[Configuration]

      val view = new error_template()
      val html = view.render(
        "My custom page title", "My custom heading", "My custom message",
        FakeRequest(), Messages.Implicits.applicationMessages, configuration)

      contentAsString(html) should {
        include("My custom page title") and
          include("My custom heading") and
          include("My custom message")
      }

      val hmtl2 = view.f("My custom page title", "My custom heading", "My custom message")(
        FakeRequest(), Messages.Implicits.applicationMessages, configuration
      )
      hmtl2 should be(html)
    }
  }

  "main_template view" should {

    "render title, header, sidebar and main content" in new App {
      implicit lazy val app: Application = new GuiceApplicationBuilder().build()
      val configuration = app.injector.instanceOf[Configuration]

      val view = new main_template()
      val html = view.render(
        title = "My custom page title",
        navLinks = Some(Html("navLinks")),
        sidebarLinks = Some(Html("sidebarLinks")),
        contentHeader = Some(Html("contentHeader")),
        bodyClasses = Some("bodyClasses"),
        mainClass = Some("mainClass"),
        scriptElem = Some(Html("scriptElem")),
        mainContent = Html("mainContent"),
        request = FakeRequest(),
        messages = Messages.Implicits.applicationMessages,
        configuration = configuration,
        analyticsAdditionalJs = None
      )

      contentAsString(html) should {
        include("My custom page title") and
          include("sidebarLinks") and
          include("navLinks") and
          include("contentHeader") and
          include("scriptElem") and
          include("mainContent") and
          include("bodyClasses") and
          include("mainClass")
      }

      val hmtl2 = view.f(
        "My custom page title",
        Some(Html("navLinks")),
        Some(Html("sidebarLinks")),
        Some(Html("contentHeader")),
        Some("bodyClasses"),
        Some("mainClass"),
        Some(Html("scriptElem")),
        None
      )(Html("mainContent"))(FakeRequest(), Messages.Implicits.applicationMessages, configuration)
      hmtl2 should be(html)
    }

  }

  "agent_services_account view" should {

    "render additional services section and manage client section with mapping, afi, invitations and manage users links when respective feature switches are on" in new App {
      implicit lazy val app: Application = new GuiceApplicationBuilder().build()
      val configuration = app.injector.instanceOf[Configuration]
      val externalUrls = app.injector.instanceOf[ExternalUrls]

      val isAgent = true
      val view = new agent_services_account()
      val html = view.render(Arn("ARN0001"), isAgent, Some("AgencyName"), None, true, externalUrls.signOutUrl, "", Messages.Implicits.applicationMessages, FakeRequest(), externalUrls, configuration)
      contentAsString(html) should {
        include("Services you might need") and
          include("Allow this account to access existing client relationships") and
          include("If your agency uses more than one Government Gateway you will need to copy your existing client relationships from each of your Government Gateway IDs into this account.") and
          include("href=\"http://localhost:9438/agent-mapping/start\"") and
          include("href=\"http://localhost:9996/tax-history/select-client\"") and
          include("Manage your clients") and
          include("Ask a client to authorise you") and
          include("href=\"http://localhost:9448/invitations/agents/\"") and
          include("Manage your users") and
          include("Control who can access your agent services account") and
          include("href=\"http://localhost:9851/user-delegation/manage-users\"")
      }
    }

    "not render additional services section, nor manage clients section, nor mapping link, nor invitations and manage users link, when respective feature switches are off" in new App {
      implicit lazy val app: Application = appBuilder.build()

      protected def appBuilder: GuiceApplicationBuilder =
        new GuiceApplicationBuilder()
          .configure(
            "features.showAdditionalServicesSection" -> false,
            "features.showAgentMappingLink" -> false,
            "features.showAgentInvitationsLink" -> false,
            "features.showAgentAfiLink" -> false,
            "features.showManageUsersLink" -> false
          )

      private val configuration = app.injector.instanceOf[Configuration]

      val externalUrls = app.injector.instanceOf[ExternalUrls]

      val isAgent = true
      val view = new agent_services_account()
      val html = view.render(Arn("ARN0001"), isAgent, Some("AgencyName"), None, true, externalUrls.signOutUrl, "", Messages.Implicits.applicationMessages, FakeRequest(), externalUrls, configuration)
      contentAsString(html) should not {
        include("Services you might need") or
          include("Allow this account to access existing client relationships") or
          include("If your agency uses more than one Government Gateway you will need to copy your existing client relationships from each of your Government Gateway IDs into this account.") or
          include("Authorise a client to report their Income Tax using software, if they&#x27;ve signed up to do so.") or
          include("Manage your clients") or
          include("Ask a client to authorise you") or
          include("href=\"http://localhost:9448/invitations/agents/\"") or
          include("Manage your users") or
          include("Control who can access your agent services account") or
          include("href=\"http://localhost:9851/user-delegation/manage-users\"")
      }
    }

    "render invitations link but not income viewer link when not whitelisted" in new App {
      implicit lazy val app: Application = new GuiceApplicationBuilder().build()
      val configuration = app.injector.instanceOf[Configuration]
      val externalUrls = app.injector.instanceOf[ExternalUrls]

      val isAgent = true
      val view = new agent_services_account()
      val html = view.render(Arn("ARN0001"), isAgent, Some("AgencyName"), None, isWhitelisted = false, externalUrls.signOutUrl, "", Messages.Implicits.applicationMessages, FakeRequest(), externalUrls, configuration)
      contentAsString(html) should not include ("href=\"http://localhost:9996/tax-history/select-client\"")
      contentAsString(html) should {
        include("Services you might need") and
          include("If your agency uses more than one Government Gateway you will need to copy your existing client relationships from each of your Government Gateway IDs into this account.") and
          include("href=\"http://localhost:9438/agent-mapping/start\"") and
          include("href=\"http://localhost:9448/invitations/agents/\"")
      }
    }

    "render does not show manage your users link because Agent is Assistant" in new App {
      implicit lazy val app: Application = new GuiceApplicationBuilder().build()
      val configuration = app.injector.instanceOf[Configuration]
      val externalUrls = app.injector.instanceOf[ExternalUrls]

      val isAgent = false
      val view = new agent_services_account()
      val html = view.render(Arn("ARN0001"), isAgent, Some("AgencyName"), None, true, externalUrls.signOutUrl, "", Messages.Implicits.applicationMessages, FakeRequest(), externalUrls, configuration)
      contentAsString(html) should not {
        include("Manage your users") or
          include("Control who can access your agent services account") or
          include("href=\"http://localhost:9851/user-delegation/manage-users\"")
      }
    }
  }

}
