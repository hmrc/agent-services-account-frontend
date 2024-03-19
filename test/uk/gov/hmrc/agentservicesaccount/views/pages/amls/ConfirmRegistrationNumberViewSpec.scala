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
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.confirm_registration_number

class ConfirmRegistrationNumberViewSpec extends BaseISpec {


  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  val view: confirm_registration_number  = app.injector.instanceOf[confirm_registration_number]
  implicit val messages: Messages = MessagesImpl(lang, messagesApi)

  def form: Form[Boolean] = YesNoForm.form("")
  def formWithErrors: Form[Boolean] = form.withError(key ="accept", message = Messages("amls.confirm-registration-number.error", "7"))


  "confirm_registration_number" when {

    def testServiceStaticContent(doc: Document): Unit = {

      "have the correct service name link" in {
        doc.select(".hmrc-header__service-name").first.text() mustBe "Agent services account"
        doc.select(".hmrc-header__service-name").first.attr("href") mustBe "/agent-services-account"
      }
      "have the correct sign out link" in {
        doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
        doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
      }
    }

    def testPageStaticContent(doc: Document): Unit = {

      "have the correct h1 heading and legend" in {
        doc.select("h1").first.text() mustBe "Is your registration number still 7?"
        doc.select("legend").text() mustBe "Is your registration number still 7?"
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }
    }

    "first viewing page" should {

      val doc: Document = Jsoup.parse(view.apply(form, "7")(FakeRequest(), messages, appConfig).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe "Is your registration number still 7? - Agent services account - GOV.UK"
      }
    }


    "form is submitted with errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithErrors, "7")(FakeRequest(), messages, appConfig).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: Is your registration number still 7? - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Select yes if your registration number is still 7"
        errorLink.attr("href") mustBe "#accept"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Select yes if your registration number is still 7"
      }
    }
  }

}
