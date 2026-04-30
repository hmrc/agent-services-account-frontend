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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.DoYouAlreadyManageForm
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.DoYouAlreadyManageForm.doYouAlreadyManageKey
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.DoYouAlreadyManageFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.do_you_already_manage

class DoYouAlreadyManageViewSpec
extends ViewBaseSpec {

  private val view: do_you_already_manage = inject[do_you_already_manage]

  private val legacyRegime = LegacyRegime.SA
  private val subscriptionBusinessName = "Test Agency"

  private val form: Form[DoYouAlreadyManageFormValues] = DoYouAlreadyManageForm.form(legacyRegime)

  private val formWithError: Form[DoYouAlreadyManageFormValues] = form.withError(
    key = doYouAlreadyManageKey,
    message = messages(s"${legacyRegime.msgPrefix}.do-you-already-manage.error.required")
  )

  def render(form: Form[DoYouAlreadyManageFormValues]): Document = Jsoup.parse(
    view(
      form,
      legacyRegime,
      subscriptionBusinessName
    )(
      messages,
      fakeRequest,
      appConfig
    ).body
  )

  private val title: String = messages(s"asa.legacy.${legacyRegime.toString.toLowerCase}.do-you-already-manage.title", subscriptionBusinessName)

  "do_you_already_manage" when {

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
        doc.select("h1").first.text() mustBe title
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }
    }

    "first viewing page" should {

      val doc: Document = render(form)

      testServiceStaticContent(doc)
      testPageStaticContent(doc)

      "display correct page title" in {
        doc.title() mustBe s"$title - Agent services account - GOV.UK"
      }

      "display correct radio options" in {
        val radios = doc.select(".govuk-radios__item")

        radios.size() mustBe 2

        radios.get(0).text() mustBe messages("asa.legacy.do-you-already-manage.existing.true")
        radios.get(0).select("input").attr("name") mustBe doYouAlreadyManageKey
        radios.get(0).select("input").attr("value") mustBe "true"

        radios.get(1).text() mustBe messages(s"asa.legacy.${legacyRegime.toString.toLowerCase}.do-you-already-manage.existing.false")
        radios.get(1).select("input").attr("name") mustBe doYouAlreadyManageKey
        radios.get(1).select("input").attr("value") mustBe "false"
      }
    }

    "when form is pre-filled with true" should {

      val filledForm = form.fill(DoYouAlreadyManageFormValues(true))
      val doc = render(filledForm)

      "have the correct radio selected" in {
        val radios = doc.select(s"input[name=$doYouAlreadyManageKey]")
        radios.get(0).hasAttr("checked") mustBe true
      }
    }

    "when form is pre-filled with false" should {

      val filledForm = form.fill(DoYouAlreadyManageFormValues(false))
      val doc = render(filledForm)

      "have the correct radio selected" in {
        val radios = doc.select(s"input[name=$doYouAlreadyManageKey]")
        radios.get(1).hasAttr("checked") mustBe true
      }
    }

    "form submitted with error" should {

      val doc: Document = render(formWithError)

      testServiceStaticContent(doc)
      testPageStaticContent(doc)

      "display error prefix in title" in {
        doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
      }

      "display error summary" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()

        errorLink.text() mustBe messages(s"${legacyRegime.msgPrefix}.do-you-already-manage.error.required")
        errorLink.attr("href") mustBe s"#$doYouAlreadyManageKey"
      }

      "display error styling" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message" in {
        doc.select(".govuk-error-message").text() mustBe
          s"Error: ${messages(s"${legacyRegime.msgPrefix}.do-you-already-manage.error.required")}"
      }
    }
  }

}
