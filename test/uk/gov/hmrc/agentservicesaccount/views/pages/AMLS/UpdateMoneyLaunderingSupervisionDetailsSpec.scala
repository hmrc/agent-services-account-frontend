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

package uk.gov.hmrc.agentservicesaccount.views.pages.AMLS

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateMoneyLaunderingSupervisionForm
import uk.gov.hmrc.agentservicesaccount.models.UpdateMoneyLaunderingSupervisionDetails
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.update_money_laundering_supervision_details


class UpdateMoneyLaunderingSupervisionDetailsSpec extends BaseISpec{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")

  val view: update_money_laundering_supervision_details = app.injector.instanceOf[update_money_laundering_supervision_details]
  val messages: Messages = MessagesImpl(lang, messagesApi)

  val form: Form[UpdateMoneyLaunderingSupervisionDetails] =
    UpdateMoneyLaunderingSupervisionForm.form  // ideally should be static so if code changes test breaks
  val formWithErrors: Form[UpdateMoneyLaunderingSupervisionDetails] =
    UpdateMoneyLaunderingSupervisionForm.form
      .withError(key = "number", message = "update-contact-details.reg.number.error.empty")
      .withError(key = "body", message = "update-contact-details.codes.body.error.empty")
      .withError(key = "endDate", message = "error.updateMoneyLaunderingSupervisory.date.invalid")
      .withError(key = "endDate.day", message = "error.updateMoneyLaunderingSupervisory.day")


  "update_money_laundering_supervision_details view" when {
    "first viewing page" should {
      val doc: Document = Jsoup.parse(view.apply(form, Map[String, String](elems = "AA" -> "AgentService"))(messages, FakeRequest(), appConfig).body)

      "display the correct page title" in {
        doc.title() mustBe "What are your money laundering supervision registration details? - Agent services account - GOV.UK"
      }
      "display correct registration body label " in {
        val errorLink: Element = doc.select(".govuk-label").get(0)
        errorLink.text() mustBe "Name of money laundering supervisory body"
      }
      "display correct amls codes hint" in {
        val errorLink: Element = doc.select(".govuk-hint").get(0)
        errorLink.text() mustBe "Start typing and select your supervisory body from the list."
      }
      "display correct registration number label " in {
        val errorLink: Element = doc.select(".govuk-label").get(1)
        errorLink.text() mustBe "Your registration number"
      }
      "display correct heading title" in {
        val errorLink: Element = doc.select("legend").get(0)
        errorLink.text() mustBe "What are your money laundering supervision registration details?"
      }
      "display correct date title" in {
        val errorLink: Element = doc.select(".govuk-hint").get(1)
        errorLink.text() mustBe "For example, 31 3 2024"
      }
      "display correct date hint" in {
        val errorLink: Element = doc.select(".govuk-hint").get(1)
        errorLink.text() mustBe "For example, 31 3 2024"
      }
      "display correct error date " in {
        val errorLink: Element = doc.select(".govuk-date-input").first()
        errorLink.text() mustBe "Day Month Year"
      }
      "have the correct service name link" in {
        doc.select(".hmrc-header__service-name").first.text() mustBe "Agent services account"
        doc.select(".hmrc-header__service-name").first.attr("href") mustBe "/agent-services-account"
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
    "form is submitted with errors should" should {
      val doc: Document = Jsoup.parse(view.apply(formWithErrors, Map[String, String](elems = "AA" -> "AgentService"))(messages, FakeRequest(), appConfig).body)

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Enter your money laundering supervision registration number"
        errorLink.attr("href") mustBe "#number"
      }
      "display correct amls codes within the select component" in {
        val component: Element = doc.select("#body").first()
        component.select("option").get(0).text() mustBe "AgentService"
        component.select("option").get(0).attr("value") mustBe "AA"
      }
      "display error message on form for amls body" in {
        val errorLink: Element = doc.select(".govuk-error-message").get(0)
        errorLink.text() mustBe "Error: Enter your money laundering supervisory body"
      }
      "display error message on form for date" in {
        val errorLink: Element = doc.select(".govuk-error-message").get(2)
        errorLink.text() mustBe "Error: Enter a real date"
      }
      "display error if registration number is invalid or empty" in {
        val errorLink: Element = doc.select(".govuk-error-message").get(1)
        errorLink.text() mustBe "Error: Enter your money laundering supervision registration number"

        errorLink.attr("href") mustBe ""
      }
      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 3
      }

      "display correct error enter-renewal-date " in {
        val errorLink: Element = doc.select(".govuk-fieldset").get(1)
        errorLink.text() mustBe "Your next registration renewal date For example, 31 3 2024 Error: Enter a real date Day Month Year"
      }
      "display error message on form" in {
        doc.select(".govuk-error-message")
          .text() mustBe "Error: Enter your money laundering supervisory body Error: Enter your money laundering supervision registration number Error: Enter a real date"
      }
    }
  }
}
