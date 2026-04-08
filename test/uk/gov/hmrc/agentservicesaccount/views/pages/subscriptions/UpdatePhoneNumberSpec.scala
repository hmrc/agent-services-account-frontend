/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.views.pages.subscriptions

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionPhoneNumberForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.{CtPhoneNumberFormValues, LegacyRegime}
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.update_phone_number

class UpdatePhoneNumberSpec
extends ViewBaseSpec {

  private val view: update_phone_number = inject[update_phone_number]
  private val subscriptionPhoneNumber = "07700 900123"

  private val legacyRegime = LegacyRegime.CT

  private val phoneNumberForm: Form[CtPhoneNumberFormValues] = CtSubscriptionPhoneNumberForm.form

  private val formWithErrors: Form[CtPhoneNumberFormValues] = phoneNumberForm.withError("phoneNumberUseAsaData", Messages("error.required"))

  def render(form: Form[CtPhoneNumberFormValues]): Document = Jsoup.parse(
    view(form, subscriptionPhoneNumber, legacyRegime)(
      messages,
      fakeRequest,
      appConfig
    ).body
  )

  private val title = messages("asa.legacy.ct.phone-number.title")

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

    "have the correct heading" in {
      doc.select("h1").text() mustBe title
    }

    "have two radio options" in {
      doc.select(".govuk-radios__item").size() mustBe 2
    }

    "have correct first radio (existing number)" in {
      doc.select(".govuk-radios__item").get(0).text() must include(subscriptionPhoneNumber)
    }

    "have correct second radio (new number option)" in {
      doc.select(".govuk-radios__item").get(1).text() must include(messages("asa.legacy.ct.phone-number.use-asa.false"))
    }

    "have continue button" in {
      doc.select(".govuk-button").text() mustBe "Continue"
    }
  }

  "update_phone_number view" when {

    "first loaded" should {

      val doc: Document = render(phoneNumberForm)

      testServiceStaticContent(doc)
      testPageStaticContent(doc)

      "have correct page title" in {
        doc.title() mustBe s"$title - Agent services account - GOV.UK"
      }

      "not display error summary" in {
        doc.select(".govuk-error-summary").size() mustBe 0
      }
    }

    "form submitted with errors" should {

      val doc: Document = render(formWithErrors)

      testServiceStaticContent(doc)
      testPageStaticContent(doc)

      "have error prefix in title" in {
        doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
      }

      "display error summary" in {
        doc.select(".govuk-error-summary").size() mustBe 1
      }

      "display error link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.attr("href") mustBe "#phoneNumberUseAsaData"
      }

      "highlight radio group with error" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }
    }

    "when 'new phone number' option is selected" should {

      val filledForm: Form[CtPhoneNumberFormValues] = phoneNumberForm.fill(
        CtPhoneNumberFormValues(
          useAsaData = false,
          newPhoneNumber = Some("07123456789")
        )
      )

      val doc: Document = render(filledForm)

      "show the conditional input section" in {
        val conditional = doc.select(".govuk-radios__conditional").first()
        conditional.hasClass("govuk-radios__conditional--hidden") mustBe false
      }

      "have the new phone number input present" in {
        doc.select("#phoneNumberNew").size() mustBe 1
      }

      "pre-fill the new phone number input" in {
        doc.select("#phoneNumberNew").`val`() mustBe "07123456789"
      }

      "have the correct radio selected" in {
        val radios = doc.select("input[name=phoneNumberUseAsaData]")
        radios.get(1).hasAttr("checked") mustBe true
      }
    }

    "when existing phone number is selected" should {

      val filledForm: Form[CtPhoneNumberFormValues] = phoneNumberForm.fill(
        CtPhoneNumberFormValues(
          useAsaData = true,
          newPhoneNumber = None
        )
      )

      val doc: Document = render(filledForm)

      "select the first radio option" in {
        val radios = doc.select("input[name=phoneNumberUseAsaData]")
        radios.get(0).hasAttr("checked") mustBe true
      }

      "keep conditional input hidden" in {
        val conditional = doc.select(".govuk-radios__conditional").first()
        conditional.hasClass("govuk-radios__conditional--hidden") mustBe true
      }
    }
  }

}
