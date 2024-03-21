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

package uk.gov.hmrc.agentservicesaccount.views.pages.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.update_confirmation_received

class AmlsConfirmationControllerViewSpec extends BaseISpec{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  val view: update_confirmation_received = app.injector.instanceOf[update_confirmation_received]
  val messages: Messages = MessagesImpl(lang, messagesApi)

  val form: Form[Boolean] = YesNoForm.form("amls.is-hmrc.error") // ideally should be static so if code changes test breaks
  val formWithErrors: Form[Boolean] = YesNoForm.form("amls.is-hmrc.error").withError(key = "accept", message = "amls.is-hmrc.error")

  "update_confirmation_received view" when {

    val doc: Document = Jsoup.parse(view.apply()(FakeRequest(), messages, appConfig).body)
      "page content" in {
        doc.select(".hmrc-header__service-name").first.text() mustBe "Agent services account"
        doc.select(".hmrc-header__service-name").first.attr("href") mustBe "/agent-services-account"

        doc.select(".govuk-panel__title").first().text() mustBe "You've changed your supervision details" //title
        doc.select(".govuk-heading-m").first().text() mustBe "What happens next" //h2
        doc.select(".govuk-body").first().text() mustBe "We'll update your anti-money laundering supervision details on your agent services accounts." //p


        doc.select(".govuk-link").get(2).text() mustBe "Return to manage account"
        doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
        doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
      }
  }
}
