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
      val heading: String = "What is the email address you want to use for your agent services account?"
      val title: String = heading + govUkSuffix

      val header: String = "What is the email address you want to use for your agent services account?"
      val hint: String = "We will use this email to contact you about your agent services account and to update you about your authorisation requests."

      val button: String = "Save and continue"
      val back: String = "Back"
    }
  }

  "update_name" should {
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
    }
  }
}
