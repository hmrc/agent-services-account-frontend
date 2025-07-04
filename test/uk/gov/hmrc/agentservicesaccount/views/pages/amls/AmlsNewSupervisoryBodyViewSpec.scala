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
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.NewAmlsSupervisoryBodyForm
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.new_supervisory_body

class AmlsNewSupervisoryBodyViewSpec
extends BaseISpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")

  val view: new_supervisory_body = app.injector.instanceOf[new_supervisory_body]
  val messages: Messages = MessagesImpl(lang, messagesApi)
  val amlsBodies = Map("ACCA" -> "Association of Certified Chartered Accountant")
  val selectedBody = "Association of Certified Chartered Accountant"

  def form(isUk: Boolean): Form[String] = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk)
  def prePopulateForm(isUk: Boolean): Form[String] = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk).fill(selectedBody)
  def formWithErrors(isUk: Boolean): Form[String] = form(isUk).withError(key = "body", message = "amls.new-supervisory-body.error")

  "new_supervisory_body" when {

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

    def testPageStaticContent(doc: Document)(isUk: Boolean): Unit = {

      "have the correct h1 heading and legend" in {
        doc.select("h1").first.text() mustBe "What’s the name of your supervisory body?"
        doc.select("legend").text() mustBe "What’s the name of your supervisory body?"
      }

      if (isUk) {
        "have the correct hint" in {
          doc.select(".govuk-hint").first().text() mustBe "Start typing and select the anti-money laundering supervisor from the list"
        }
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }
    }

    "first viewing page for UK agent" should {
      val isUk = true

      val doc: Document = Jsoup.parse(view.apply(
        form(isUk),
        amlsBodies,
        isUk,
        cya = false
      )(
        fakeRequest(),
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)(isUk)

      "display the correct page title" in {
        doc.title() mustBe "What’s the name of your supervisory body? - Agent services account - GOV.UK"
      }
    }

    "viewing page with form data for UK agent" should {
      val isUk = true

      val doc: Document = Jsoup.parse(view.apply(
        prePopulateForm(isUk),
        amlsBodies,
        isUk,
        cya = false
      )(
        fakeRequest(),
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)(isUk)

      "display the correct page title" in {
        doc.title() mustBe "What’s the name of your supervisory body? - Agent services account - GOV.UK"
      }

      "display pre-populated selected supervisory body" in {
        doc.select("option[selected]").text mustBe selectedBody
      }
    }

    "first viewing page for overseas agent" should {
      val isUk = false

      val doc: Document = Jsoup.parse(view.apply(
        form(isUk),
        amlsBodies,
        isUk,
        cya = false
      )(
        fakeRequest(),
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)(isUk)

      "display the correct page title" in {
        doc.title() mustBe "What’s the name of your supervisory body? - Agent services account - GOV.UK"
      }
    }

    "form is submitted with errors should" should {
      val isUk = true

      val doc: Document = Jsoup.parse(view.apply(
        formWithErrors(isUk),
        amlsBodies,
        isUk,
        cya = false
      )(
        fakeRequest(),
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)(isUk)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: What’s the name of your supervisory body? - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Tell us the name of your supervisory body"
        errorLink.attr("href") mustBe "#body"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Tell us the name of your supervisory body"
      }
    }
  }

}
