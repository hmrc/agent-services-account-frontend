/*
 * Copyright 2019 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.views.html.error_template_Scope0.error_template_Scope1.error_template
import uk.gov.hmrc.agentservicesaccount.views.html.main_template_Scope0.main_template_Scope1.main_template
import uk.gov.hmrc.agentservicesaccount.views.html.pages.agent_services_account_Scope0.agent_services_account_Scope1.agent_services_account
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.agentservicesaccount.views.html.govuk_wrapper_Scope0.govuk_wrapper_Scope1.govuk_wrapper

class ViewsSpec extends UnitSpec with GuiceOneAppPerTest {
  trait PlainAppConfig {
    implicit lazy val app: Application = new GuiceApplicationBuilder().configure("metrics.jvm" -> false, "metrics.logback" -> false).build()
    lazy val config = app.injector.instanceOf[Configuration]
    lazy val _externalUrls = app.injector.instanceOf[ExternalUrls]
  }

  "error_template view" should {

    "render title, heading and message" in new PlainAppConfig {
      val view = new error_template()
      val html = view.render(
        "My custom page title", "My custom heading", "My custom message",
        FakeRequest(), Messages.Implicits.applicationMessages, config, _externalUrls)

      contentAsString(html) should {
        include("My custom page title") and
          include("My custom heading") and
          include("My custom message")
      }

      val hmtl2 = view.f("My custom page title", "My custom heading", "My custom message")(
        FakeRequest(), Messages.Implicits.applicationMessages, config, _externalUrls
      )
      hmtl2 should be(html)
    }
  }

  "govuk_wrapper view" should {
    "render content" in new PlainAppConfig {
      val view = new govuk_wrapper()

      val html = view.render(
        title = "My custom page title", mainClass = Some("main class"), mainDataAttributes = Some(Html("main data attributes")),
        bodyClasses = Some("body-classes"), navLinks = Some(Html("nav_links")), sidebar = Html("sideBar"),
        contentHeader = Some(Html("content-header")), mainContent = Html("mainContent"), serviceInfoContent = Html("serviceInfoContent"),
        scriptElem = Some(Html("scriptElem")), analyticsAdditionalJs = Option(Html("additional-js")), isAdmin = false,
        hasCustomContent = false, hasTimeout = true, request = FakeRequest(), messages = Messages.Implicits.applicationMessages,
        configuration = config, externalUrls = _externalUrls
      )

      contentAsString(html) should {
        include("My custom page title")
        include("main class") and
          include("main data attributes") and
          include("body-classes") and
          include("nav_links")
        include("sideBar") and
          include("content-header") and
          include("mainContent") and
          include("serviceInfoContent") and
          include("scriptElem")
      }


      val doc = Jsoup.parse(contentAsString(html))

      doc.getElementById("timeoutDialog").isBlock shouldBe true
    }
  }

  "main_template view" should {

    "render title, header, sidebar and main content" in new PlainAppConfig {
      val view = new main_template()

      val html = view.render(
        title = "My custom page title",
        navLinks = Some(Html("navLinks")),
        sidebarLinks = Some(Html("sidebarLinks")),
        contentHeader = Some(Html("contentHeader")),
        bodyClasses = Some("bodyClasses"),
        mainClass = Some("mainClass"),
        scriptElem = Some(Html("My custom script")),
        analyticsAdditionalJs = None,
        isAdmin = false,
        hasCustomContent = false,
        hasTimeout = true,
        mainContent = Html("mainContent"),
        request = FakeRequest(),
        messages = Messages.Implicits.applicationMessages,
        configuration = config,
        externalUrls = _externalUrls
      )

      val content = contentAsString(html)

      content should {
        include("My custom page title")
          include("sidebarLinks") and
          include("navLinks") and
          include("contentHeader") and
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
        Some(Html("My custom script")),
        None,
        false,
        false,
        true
      )(Html("mainContent"))(FakeRequest(), Messages.Implicits.applicationMessages, config, _externalUrls)
      hmtl2 should be(html)
    }
  }

  "agent_services_account view" should {

    "render the content including the 'Manage account' navbar menu item if the user has the Admin Credential Role" in new PlainAppConfig {
      val view = new agent_services_account()
      val html = view.render(arn = "ARN0001", isWhitelisted = true, customDimension =  "", isAdmin = true, Messages.Implicits.applicationMessages, FakeRequest(), _externalUrls, config)
      contentAsString(html) should {
        include("Account home") and
          include("Manage account") and
          include("Sign out") and
          include("href='http://localhost:9250/contact/beta-feedback?service=AOSS'") and
          include("Agent services account") and
          include("Account number: ARN0001")
          include("You cannot view your client lists in your agent services account. You can use your account to view and manage an individual clients VAT details.") and
          include("Making Tax Digital for VAT") and
          include("Sign clients up for Making Tax Digital for VAT") and
          include("You copied across existing client authorisations to your agent services account. This means you can now sign these clients up to Making Tax Digital.") and
          include("Sign clients up for Making Tax Digital for VAT (opens in a new window or tab)") and
          include("Manage your client&#x27;s VAT details") and
          include("Use this service to update your client’s VAT registration status, business name (if they are a limited company), principal place of business and VAT stagger.") and
          include("View a client&#x27;s PAYE income record") and
          include("Access a client&#x27;s PAYE income record to help you complete their Self Assessment tax return.") and
            include("http://localhost:9996/tax-history/select-client") and
          include("Client authorisations") and
          include("Ask a client to authorise you") and
          include("You only need to do this if you have not copied across an existing authorisation from the client.") and
          include("http://localhost:9448/invitations/agents/") and
          include("Manage authorisations") and
          include("Track your recent authorisation requests") and
          include("Copy across more VAT and Self Assessment client authorisations") and
          include("http://localhost:9438/agent-mapping/start") and
          include("Cancel a client&#x27;s authorisation") and
          include("http://localhost:9448/invitations/agents/cancel-authorisation/client-type")
      }
    }

    "not render the 'Manage account' navbar menu item if the user does not have the Admin Credential Role" in new PlainAppConfig {
      val view = new agent_services_account()
      val html = view.render(arn = "ARN0001", isWhitelisted = true, customDimension = "", isAdmin = false,  Messages.Implicits.applicationMessages, FakeRequest(), _externalUrls, config)
      contentAsString(html) should not {
        include("Manage account") and
        include("http://localhost:9401/agent-services-account/manage-account")
      }
    }

    "render invitations link but not income viewer link when not whitelisted" in new PlainAppConfig {
      val view = new agent_services_account()
      val html = view.render(arn = "ARN0001", isWhitelisted = false, customDimension = "", isAdmin = true, Messages.Implicits.applicationMessages, FakeRequest(), _externalUrls, config)
      contentAsString(html) should not include ("http://localhost:9996/tax-history/select-client")
      contentAsString(html) should {
        include("Track your recent authorisation requests") and
          include("http://localhost:9448/invitations/track")

      }
    }

    "render does not show manage your users link because Agent is Assistant" in new PlainAppConfig {
      val view = new agent_services_account()
      val html = view.render(arn = "ARN0001", isWhitelisted = true, customDimension =  "", isAdmin = true, Messages.Implicits.applicationMessages, FakeRequest(), _externalUrls, config)
      contentAsString(html) should not {
        include("Manage your users") or
          include("Control who can access your agent services account") or
          include("href=\"http://localhost:9851/user-delegation/manage-users\"")
      }
    }
  }

}
