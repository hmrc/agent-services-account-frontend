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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class UpdateEmailSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: update_email = app.injector.instanceOf[update_email]
  private val form: Form[String] = UpdateDetailsForms.emailAddressForm

  object MessageLookup {
    object English {
      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "What’s the new email address?"
      val title: String = heading + govUkSuffix

      val header: String = "What’s the new email address?"
      val hint: String = "We’ll use this email address to contact the business about the agent services account, and to send updates about authorisation requests."

      val button: String = "Continue"
      val back: String = "Back"
    }
    object Welsh {
      private val govUkSuffix: String = " - Cyfrif gwasanaethau asiant - GOV.UK"
      val heading: String = "Beth yw’r cyfeiriad e-bost newydd?"
      val title: String = heading + govUkSuffix

      val header: String = "Beth yw’r cyfeiriad e-bost newydd?"
      val hint: String = "Byddwn yn defnyddio’r cyfeiriad e-bost hwn i gysylltu â’r busnes ynghylch y cyfrif gwasanaethau asiant, ac i anfon diweddariadau ynghylch ceisiadau am awdurdodiad."

      val button: String = "Yn eich blaen"
      val back: String = "Yn ôl"
    }
  }

  "update_email" should {
    "render correctly" when {
      "the selected lang is english" in {
        val messages: Messages = MessagesImpl(langs.head, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(form)(messages, FakeRequest(), appConfig).body)

        doc.title() mustBe MessageLookup.English.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.English.header
        doc.select(".govuk-hint").asScala.head.text mustBe MessageLookup.English.hint

        doc.select(".govuk-button").asScala.head.text mustBe MessageLookup.English.button

        doc.select(".govuk-back-link").first.text() mustBe MessageLookup.English.back
        doc.select(".govuk-back-link").first.attr("href") mustBe "#"
      }
      "the selected lang is welsh" in {
        val messages: Messages = MessagesImpl(langs.last, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(form)(messages, FakeRequest(), appConfig).body)

        doc.title() mustBe MessageLookup.Welsh.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.header
        doc.select(".govuk-hint").asScala.head.text mustBe MessageLookup.Welsh.hint

        doc.select(".govuk-button").asScala.head.text mustBe MessageLookup.Welsh.button

        doc.select(".govuk-back-link").first.text() mustBe MessageLookup.Welsh.back
      }
    }
  }
}
