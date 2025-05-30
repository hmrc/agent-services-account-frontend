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

package uk.gov.hmrc.agentservicesaccount.views.pages

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.admin_access_for_access_groups

class AdminAccessForAccessGroupsViewSpec
extends BaseISpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  val view: admin_access_for_access_groups = app.injector.instanceOf[admin_access_for_access_groups]
  val messages: Messages = MessagesImpl(lang, messagesApi)

  "admin_access_for_access_groups" should {
    val doc: Document = Jsoup.parse(view.apply()(
      FakeRequest(),
      messages,
      appConfig
    ).body)

    "have the correct service name link" in {
      doc.select(".govuk-header__service-name").first.text() mustBe "Agent services account"
      doc.select(".govuk-header__service-name").first.attr("href") mustBe "/agent-services-account"
    }

    "have the correct sign out link" in {
      doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
      doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
    }

    "display the correct page title" in {
      doc.title() mustBe "You have administrative access - Agent services account - GOV.UK"
    }

    "display the correct h1" in {
      doc.select("h1").text() mustBe "You have administrative access"
    }

    "display the correct body content" in {
      doc.select("p").get(0).text() mustBe "As an administrator, you can give yourself access to the client."
      doc.select("p").get(1).text() mustBe "You can do this by:"
      doc.select(".govuk-list--item").get(0).text() mustBe "adding yourself to an access group the client is already in"
      doc.select(".govuk-list--item").get(1).text() mustBe "adding the client to an access group you are in"
      doc.select(".govuk-list--item").get(2).text() mustBe "creating a new access group containing you and the client"
      doc.select("p").get(2).text() mustBe "If you are not sure how to do this, ask another administrator in your organisation."
    }

    "display the correct button link" in {
      val buttonLink = doc.select(".govuk-button")
      buttonLink.text() mustBe "Go to manage account"
      buttonLink.attr("href") mustBe "/agent-services-account/manage-account"
    }

  }

}
