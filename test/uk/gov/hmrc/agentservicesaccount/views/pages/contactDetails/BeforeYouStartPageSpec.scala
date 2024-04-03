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
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

import scala.jdk.CollectionConverters.CollectionHasAsScala

class BeforeYouStartPageSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: before_you_start_page = app.injector.instanceOf[before_you_start_page]
  private val testAgencyDetailsFull = Some(AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress("Test Street", Some("Test Town"), None, None, Some("TE5 7ED"), "GB")))
  )
  private val testAgencyDetailsNoName = Some(AgencyDetails(
    agencyName = Some(""),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress("Test Street", Some("Test Town"), None, None, Some("TE5 7ED"), "GB")))
  )

  object MessageLookup {

    object English {
      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "Contact details"
      val title: String = heading + govUkSuffix


      val header1: String = "Update contact details"
      val header2: String = "Before you start"

      val bulletPoint1: String = "director"
      val bulletPoint2: String = "company secretary"
      val bulletPoint3: String = "sole trader"
      val bulletPoint4: String = "proprietor"
      val bulletPoint5: String = "partner"

      val paragraph1: String = "To update contact details for {0}, you must be authorised by a:"
      val paragraph2: String = "If your role is on this list, you do not need additional authorisation."
      val paragraph3: String = "We'll ask you to confirm that you have authorisation before you submit any changes. We'll also need your name and telephone number."
      val paragraph4: String = "In some cirumstances we'll ask for your agent codes for Self Assessment and Corporation Tax. The codes were sent by letter when your organisation asked for agent access to those services."
      val paragraph5: String = "Once you submit changes, you cannot amend the contact details again for 4 weeks."
      val paragraph6: String = "To update contact details, you must be authorised by a:"
      val button: String = "Continue"
    }

    object Welsh {
      private val govUkSuffix: String = " - Cyfrif gwasanaethau asiant - GOV.UK"
      val heading: String = "Manylion cyswllt"
      val title: String = heading + govUkSuffix


      val header1: String = "Diweddaru manylion cyswllt"
      val header2: String = "Cyn i chi ddechrau"

      val bulletPoint1: String = "cyfarwyddwr"
      val bulletPoint2: String = "ysgrifennydd y cwmni"
      val bulletPoint3: String = "unig fasnachwr"
      val bulletPoint4: String = "perchennog"
      val bulletPoint5: String = "partner"

      val paragraph1: String = "I ddiweddaru’r manylion cyswllt ar gyfer {}, mae’n rhaid eich bod wedi’ch awdurdodi gan y canlynol:"
      val paragraph2: String = "Os yw’ch swydd wedi’i nodi ar y rhestr hon, does dim angen awdurdod ychwanegol arnoch. "
      val paragraph3: String = "'Byddwn yn gofyn i chi gadarnhau fod gennych awdurdod cyn i chi gyflwyno unrhyw newidiadau. Bydd angen eich enw a'ch rhif ffôn arnom hefyd."
      val paragraph4: String = "tMewn rhai amgylchiadau, byddwn yn gofyn am eich codau asiant ar gyfer Hunanasesiad a Threth Gorfforaeth.Anfonwyd y codau mewn llythyr pan ofynnodd eich sefydliad am fynediad asiant i?r gwasanaethau hynny."
      val paragraph5: String = "Ar ôl i chi gyflwyno newidiadau, ni fyddwch yn gallu diwygio?r manylion cyswllt eto am 4 wythnos."
      val paragraph6: String = "I ddiweddaru''r manylion cyswllt ar, mae''n rhaid eich bod wedi''ch awdurdodi gan y canlynol:"

      val button: String = "Yn eich blaen"
    }


    "before_you_start_page" should {
      "render correctly" when {
        "the selected lang is english" when {
          val messages: Messages = MessagesImpl(langs.head, messagesApi)
          "there are contact details" in {
            val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull)(FakeRequest(), messages, appConfig).body)

            doc.title() mustBe MessageLookup.English.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.English.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.English.header1

            val paragraphKeys = doc.select(".govuk-body").asScala.toList.map(_.text)

            paragraphKeys.head mustBe MessageLookup.English.paragraph1
            paragraphKeys(1) mustBe MessageLookup.English.paragraph2
            paragraphKeys(2) mustBe MessageLookup.English.paragraph3
            paragraphKeys(3) mustBe MessageLookup.English.paragraph4
            paragraphKeys(4) mustBe MessageLookup.English.paragraph5

            val bulletPoint = doc.select(".govuk-list govuk-list--bullet").asScala.toList

            bulletPoint.head mustBe MessageLookup.English.bulletPoint1
            bulletPoint(1) mustBe MessageLookup.English.bulletPoint2
            bulletPoint(2) mustBe MessageLookup.English.bulletPoint3
            bulletPoint(3) mustBe MessageLookup.English.bulletPoint4
            bulletPoint(4) mustBe MessageLookup.English.bulletPoint5

            val button = doc.select(".govuk-button").asScala.toList

            button(1).text() mustBe MessageLookup.English.button
            button(1).attributes().get("href") mustBe "/"
          }
          "there are contact details however missing a agent name" in {
            val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsNoName)(FakeRequest(), messages, appConfig).body)

            doc.title() mustBe MessageLookup.English.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.English.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.English.header1

            val paragraphKeys = doc.select(".govuk-body").asScala.toList.map(_.text)

            paragraphKeys.head mustBe MessageLookup.English.paragraph6
            paragraphKeys(1) mustBe MessageLookup.English.paragraph2
            paragraphKeys(2) mustBe MessageLookup.English.paragraph3
            paragraphKeys(3) mustBe MessageLookup.English.paragraph4
            paragraphKeys(4) mustBe MessageLookup.English.paragraph5

            val bulletPoint = doc.select(".govuk-list govuk-list--bullet").asScala.toList

            bulletPoint.head mustBe MessageLookup.English.bulletPoint1
            bulletPoint(1) mustBe MessageLookup.English.bulletPoint2
            bulletPoint(2) mustBe MessageLookup.English.bulletPoint3
            bulletPoint(3) mustBe MessageLookup.English.bulletPoint4
            bulletPoint(4) mustBe MessageLookup.English.bulletPoint5

            val button = doc.select(".govuk-button").asScala.toList

            button(1).text() mustBe MessageLookup.English.button
            button(1).attributes().get("href") mustBe "/"
          }
          "If you're a standard user you will be presented with a Forbidden error page" in {
            val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull)(FakeRequest(), messages, appConfig).body)

            doc.title() mustBe MessageLookup.English.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.English.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.English.header1
          }
        }

        "the selected lang is welsh" when {
          val messages: Messages = MessagesImpl(langs.last, messagesApi)
          "there are contact details" in {
            val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull)(FakeRequest(), messages, appConfig).body)

            doc.title() mustBe MessageLookup.Welsh.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.Welsh.header2

            val paragraphKeys = doc.select(".govuk-body").asScala.toList.map(_.text)

            paragraphKeys.head mustBe MessageLookup.Welsh.paragraph1
            paragraphKeys(1) mustBe MessageLookup.Welsh.paragraph2
            paragraphKeys(2) mustBe MessageLookup.Welsh.paragraph3
            paragraphKeys(3) mustBe MessageLookup.Welsh.paragraph4
            paragraphKeys(4) mustBe MessageLookup.Welsh.paragraph5

            val bulletPoint = doc.select(".govuk-list govuk-list--bullet").asScala.toList


            bulletPoint.head mustBe MessageLookup.Welsh.bulletPoint1
            bulletPoint(1) mustBe MessageLookup.Welsh.bulletPoint2
            bulletPoint(2) mustBe MessageLookup.Welsh.bulletPoint3
            bulletPoint(3) mustBe MessageLookup.Welsh.bulletPoint4
            bulletPoint(4) mustBe MessageLookup.Welsh.bulletPoint5

            val button = doc.select(".govuk-button").asScala.toList

            button(1).text() mustBe MessageLookup.Welsh.button
            button(1).attributes().get("href") mustBe "/"
          }
          "there are contact details however missing a agent name" in {
            val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsNoName)(FakeRequest(), messages, appConfig).body)

            doc.title() mustBe MessageLookup.Welsh.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.English.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.English.header1

            val paragraphKeys = doc.select(".govuk-body").asScala.toList.map(_.text)

            paragraphKeys.head mustBe MessageLookup.Welsh.paragraph6
            paragraphKeys(1) mustBe MessageLookup.Welsh.paragraph2
            paragraphKeys(2) mustBe MessageLookup.Welsh.paragraph3
            paragraphKeys(3) mustBe MessageLookup.Welsh.paragraph4
            paragraphKeys(4) mustBe MessageLookup.Welsh.paragraph5

            val bulletPoint = doc.select(".govuk-list govuk-list--bullet").asScala.toList

            bulletPoint.head mustBe MessageLookup.Welsh.bulletPoint1
            bulletPoint(1) mustBe MessageLookup.Welsh.bulletPoint2
            bulletPoint(2) mustBe MessageLookup.Welsh.bulletPoint3
            bulletPoint(3) mustBe MessageLookup.Welsh.bulletPoint4
            bulletPoint(4) mustBe MessageLookup.Welsh.bulletPoint5

            val button = doc.select(".govuk-button").asScala.toList

            button(1).text() mustBe MessageLookup.Welsh.button
            button(1).attributes().get("href") mustBe "/"
          }
          "If you're a standard user you will be presented with a Forbidden error page" in {
            val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull)(FakeRequest(), messages, appConfig).body)

            doc.title() mustBe MessageLookup.Welsh.title
            doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.header1
            doc.select("h2").asScala.head.text mustBe MessageLookup.Welsh.header2
          }
        }
      }
    }
  }
}
