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
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class UpdateNameSpec
extends BaseISpec {

  private implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private implicit val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: update_name = app.injector.instanceOf[update_name]
  private val form: Form[String] = UpdateDetailsForms.businessNameForm

  object MessageLookup {

    object English {

      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "What’s the new name you want to show to clients?"
      val title: String = heading + govUkSuffix

      val header: String = "What’s the new name you want to show to clients?"
      val hint: String = "Clients will see this name when they accept or manage your authorisations."

      val button: String = "Continue"
      val back: String = "Back"

    }

    object Welsh {

      private val govUkSuffix: String = " - Cyfrif gwasanaethau asiant - GOV.UK"
      val heading: String = "Beth yw’r enw newydd yr hoffech ei ddangos i gleientiaid?"
      val title: String = heading + govUkSuffix

      val header: String = "Beth yw’r enw newydd yr hoffech ei ddangos i gleientiaid?"
      val hint: String = "Bydd cleientiaid yn gweld yr enw hwn wrth dderbyn neu reoli’ch awdurdodiadau."

      val button: String = "Yn eich blaen"
      val back: String = "Yn ôl"

    }

  }

  "update_name" should {
    "render correctly" when {
      "the selected lang is english" in {
        val messages: Messages = MessagesImpl(langs.head, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(form)(
          messages,
          fakeRequest(),
          appConfig
        ).body)

        doc.title() mustBe MessageLookup.English.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.English.header
        doc.select(".govuk-hint").asScala.head.text mustBe MessageLookup.English.hint

        doc.select(".govuk-button").asScala.head.text mustBe MessageLookup.English.button

        doc.select(".govuk-back-link").first.text() mustBe MessageLookup.English.back
        doc.select(".govuk-back-link").first.attr("href") mustBe "#"
      }

      "the selected lang is welsh" in {
        val messages: Messages = MessagesImpl(langs.last, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(form)(
          messages,
          fakeRequest(),
          appConfig
        ).body)

        doc.title() mustBe MessageLookup.Welsh.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.header
        doc.select(".govuk-hint").asScala.head.text mustBe MessageLookup.Welsh.hint

        doc.select(".govuk-button").asScala.head.text mustBe MessageLookup.Welsh.button

        doc.select(".govuk-back-link").first.text() mustBe MessageLookup.Welsh.back
      }
    }
  }

}
