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
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.applySaCodeChangesForm
import uk.gov.hmrc.agentservicesaccount.models.ApplySaCodeChanges
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

class ApplySaCodePageSpec
extends BaseISpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  val view: apply_sa_code_changes = app.injector.instanceOf[apply_sa_code_changes]
  implicit val messages: Messages = MessagesImpl(lang, messagesApi)

  val form: Form[ApplySaCodeChanges] = applySaCodeChangesForm
  val formWithNameErrors: Form[ApplySaCodeChanges] = form.withError(
    key = "applyChanges",
    message = Messages("update-contact-details.apply-sa-code-changes.error.empty")
  )

  "apply_sa_code_changes" when {

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

      "have the correct h1 heading" in {
        doc.select("h1").first.text() mustBe "Do you want to apply these changes to your Self Assessment agent code?"
      }

      "have radio button options for yes and no" in {
        doc.select(".govuk-radios__item > label[for='Yes']").text() mustBe "Yes"
        doc.select(".govuk-radios__item > label[for='No']").text() mustBe "No"
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }

    }

    "first viewing page" should {

      val doc: Document = Jsoup.parse(view.apply(form)(
        messages,
        FakeRequest(),
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe "Do you want to apply these changes to your Self Assessment agent code? - Agent services account - GOV.UK"
      }
    }

    "form is submitted with error should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithNameErrors)(
        messages,
        FakeRequest(),
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: Do you want to apply these changes to your Self Assessment agent code? - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Select yes if you want to apply these changes to your Self Assessment agent code."
        errorLink.attr("href") mustBe "#applyChanges"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Select yes if you want to apply these changes to your Self Assessment agent code."
      }
    }

  }

}
