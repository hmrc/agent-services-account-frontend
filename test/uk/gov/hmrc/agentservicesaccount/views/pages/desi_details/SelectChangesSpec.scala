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
import uk.gov.hmrc.agentservicesaccount.forms.SelectChangesForm
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SelectChanges
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class SelectChangesSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: select_changes = app.injector.instanceOf[select_changes]
  private val form: Form[SelectChanges] = SelectChangesForm.form

  object MessageLookup {
    object English {
      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "Which contact details do you want to change?"
      val title: String = heading + govUkSuffix
      val hint: String = "Select all that apply."

      val checkboxLabel1: String = "Business name shown to clients"
      val checkboxLabel2: String = "Address for agent services account"
      val checkboxLabel3: String = "Email address"
      val checkboxLabel4: String = "Telephone number"

      val button: String = "Continue"
    }

    object Welsh {
      private val govUkSuffix: String = " - Cyfrif gwasanaethau asiant - GOV.UK"
      val heading: String = "Pa fanylion cyswllt yr hoffech eu newid?"
      val title: String = heading + govUkSuffix
      val hint: String = "Dewiswch bob un sy’n berthnasol."

      val checkboxLabel1: String = "Enw’r busnes a ddangosir i gleientiaid"
      val checkboxLabel2: String = "Cyfeiriad ar gyfer y cyfrif gwasanaethau asiant"
      val checkboxLabel3: String = "Cyfeiriad e-bost"
      val checkboxLabel4: String = "Rhif ffôn"

      val button: String = "Yn eich blaen"
    }
  }

  "select_changes" should {
    "render correctly" when {
      "the selected lang is english" in {
        val messages: Messages = MessagesImpl(langs.head, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(form)(FakeRequest(), messages, appConfig).body)

        doc.title() mustBe MessageLookup.English.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
        doc.select(".govuk-hint").asScala.head.text mustBe MessageLookup.English.hint

        val checkboxLabels = doc.select(".govuk-checkboxes__label").asScala.toList.map(_.text)
        checkboxLabels.head mustBe MessageLookup.English.checkboxLabel1
        checkboxLabels(1) mustBe MessageLookup.English.checkboxLabel2
        checkboxLabels(2) mustBe MessageLookup.English.checkboxLabel3
        checkboxLabels.last mustBe MessageLookup.English.checkboxLabel4

        doc.select(".govuk-button").asScala.head.text mustBe MessageLookup.English.button
      }

      "the selected lang is Welsh" in {
        val messages: Messages = MessagesImpl(langs.last, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(form)(FakeRequest(), messages, appConfig).body)

        doc.title() mustBe MessageLookup.Welsh.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.heading
        doc.select(".govuk-hint").asScala.head.text mustBe MessageLookup.Welsh.hint

        val checkboxLabels = doc.select(".govuk-checkboxes__label").asScala.toList.map(_.text)
        checkboxLabels.head mustBe MessageLookup.Welsh.checkboxLabel1
        checkboxLabels(1) mustBe MessageLookup.Welsh.checkboxLabel2
        checkboxLabels(2) mustBe MessageLookup.Welsh.checkboxLabel3
        checkboxLabels.last mustBe MessageLookup.Welsh.checkboxLabel4

        doc.select(".govuk-button").asScala.head.text mustBe MessageLookup.Welsh.button
      }
    }
  }
}
