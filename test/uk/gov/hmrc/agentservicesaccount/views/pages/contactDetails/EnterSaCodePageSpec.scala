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

package uk.gov.hmrc.agentservicesaccount.views.pages.contactDetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.saCodeForm
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.contact_details._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class EnterSaCodePageSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: enter_sa_code = app.injector.instanceOf[enter_sa_code]

  object MessageLookup {

    object English {
      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "Contact details"
      val title: String = heading + govUkSuffix


      val header1: String = "What's the agent code you use for Self Assessment?"

      val hint1: String = "This is a 6-character code made up of numbers and letters. For example, A1234B or 5678CD."
      val paragraph1: String = "If you cannot find the code, select 'Continue without code'."
      val button1: String = "Continue"
      val button2: String = "Continue without code"
    }

    object Welsh {
      private val govUkSuffix: String = " - Cyfrif gwasanaethau asiant - GOV.UK"
      val heading: String = "Manylion cyswllt"
      val title: String = heading + govUkSuffix


      val header1: String = "Beth yw’r cod asiant yr ydych yn ei ddefnyddio ar gyfer Hunanasesiad?"

      val hint1: String = "Cod 6 cymeriad yw hwn, sy’n cynnwys rhifau a llythrennau. Er enghraifft, A1234B neu 5678CD."
      val paragraph1: String = "Os na allwch chi ddod o hyd i’r cod, dewiswch ‘Yn eich blaen heb god’."
      val button1: String = "Yn eich blaen"
      val button2: String = "Yn eich blaen heb god"
    }


    "enter_sa_code" should {
      "render correctly" when {
        "the selected lang is english" when {
          val messages: Messages = MessagesImpl(langs.head, messagesApi)
          "there are contact details" in {
            val doc: Document = Jsoup.parse(view.apply(saCodeForm)(messages, FakeRequest(), appConfig).body)

            doc.title() mustBe MessageLookup.English.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.English.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.English.header1

            val paragraphKeys = doc.select(".govuk-body").asScala.toList.map(_.text)

            paragraphKeys.head mustBe MessageLookup.English.paragraph1

            val button = doc.select(".govuk-button").asScala.toList

            button(1).text() mustBe MessageLookup.English.button1
            button(1).attributes().get("href") mustBe "/"

            button(2).text() mustBe MessageLookup.English.button2
            button(2).attributes().get("href") mustBe "/"

          }

          "If you're a standard user you will be presented with a Forbidden error page" in {
            val doc: Document = Jsoup.parse(view.apply(saCodeForm)(messages, FakeRequest(), appConfig).body)

            doc.title() mustBe MessageLookup.English.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.English.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.English.header1
          }
        }

      }
    }
  }
}
