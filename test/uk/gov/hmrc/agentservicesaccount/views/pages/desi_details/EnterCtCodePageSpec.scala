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

package uk.gov.hmrc.agentservicesaccount.views.pages.desi_details

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.ctCodeForm
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

class EnterCtCodePageSpec
extends BaseISpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  val view: enter_ct_code = app.injector.instanceOf[enter_ct_code]
  implicit val messages: Messages = MessagesImpl(lang, messagesApi)

  val form: Form[String] = ctCodeForm
  val formWithNameErrors: Form[String] = form.withError(key = "ctCode", message = Messages("update-contact-details.ct-code.error.empty"))

  "enter_ct_code" when {

    def testServiceStaticContent(doc: Document): Unit = {

      "have the correct service name link" in {
        doc.select(".govuk-header__service-name").first.text() mustBe "Agent services account"
        doc.select(".govuk-header__service-name").first.attr("href") mustBe "/agent-services-account"
      }
      "have the correct sign out link" in {
        doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
        doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
      }
      "have the correct back link" in {
        doc.select(".govuk-back-link").first.text() mustBe "Back"
        doc.select(".govuk-back-link").first.attr("href") mustBe "#"
      }
    }

    def testPageStaticContent(doc: Document): Unit = {

      "have the correct h1 heading and introduction" in {
        doc.select("h1").first.text() mustBe "What's the agent code you use for Corporation Tax?"
      }

      "have the correct hint" in {
        doc.select(".govuk-hint").first.text() mustBe "This is a 6-character code made up of numbers and letters. For example, A1234B or 5678CD."
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }

      "have the correct continue without  button" in {
        doc.select(".govuk-button").last().text() mustBe "Continue without code"
      }

    }

    "first viewing page" should {

      val doc: Document = Jsoup.parse(view.apply(form)(
        messages,
        fakeRequest(),
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe "What's the agent code you use for Corporation Tax? - Agent services account - GOV.UK"
      }
    }

    "form is submitted with name errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithNameErrors)(
        messages,
        fakeRequest(),
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: What's the agent code you use for Corporation Tax? - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Enter the agent code you use for Corporation Tax."
        errorLink.attr("href") mustBe "#ctCode"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Enter the agent code you use for Corporation Tax."
      }
    }

  }

}
