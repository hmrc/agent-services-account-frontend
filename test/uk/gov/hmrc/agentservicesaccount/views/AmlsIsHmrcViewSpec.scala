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

package uk.gov.hmrc.agentservicesaccount.views

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.is_amls_hmrc
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec

class AmlsIsHmrcViewSpec extends BaseISpec {
  trait Setup {
    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val lang: Lang = Lang("en")
    val view: is_amls_hmrc = app.injector.instanceOf[is_amls_hmrc]
    val messages: Messages = MessagesImpl(lang, messagesApi)

    val form: Form[Boolean] = YesNoForm.form("amls.is-hmrc.error") // ideally should be static so if code changes test breaks
    val formWithErrors: Form[Boolean] = YesNoForm.form("amls.is-hmrc.error").withError(key ="accept",message = "amls.is-hmrc.error")

  }

  "is_amls_hmrc view" should {
    "display form page correctly" in new Setup {
      val doc: Document = Jsoup.parse(view.apply(form)(FakeRequest(), messages, appConfig).body)

      doc.title() mustBe "Is HMRC your money laundering supervisory body? - Agent services account - GOV.UK"
      expectedH1(doc, "Is HMRC your money laundering supervisory body?")

      doc.select("legend").text() mustBe "Is HMRC your money laundering supervisory body?" // same as h1
      val labels: Elements = doc.select(".govuk-radios label")
      labels.first().text() mustBe "Yes"
      labels.last().text() mustBe "No"
    }

    "display with errors" in new Setup {
      val doc: Document = Jsoup.parse(view.apply(formWithErrors)(FakeRequest(), messages, appConfig).body)

      doc.title() mustBe "Error: Is HMRC your money laundering supervisory body? - Agent services account - GOV.UK"
      expectedH1(doc, "Is HMRC your money laundering supervisory body?")
      doc.select("legend").text() mustBe "Is HMRC your money laundering supervisory body?"

      val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
      errorLink.text() mustBe "Select yes if HMRC is your money laundering supervisory body"
      errorLink.attr("href") mustBe "#accept"

    }

  }


}
