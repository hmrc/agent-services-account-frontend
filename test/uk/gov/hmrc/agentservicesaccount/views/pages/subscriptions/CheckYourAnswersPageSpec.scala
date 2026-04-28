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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesImpl
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.routes
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.{CT, PAYE, SA}
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.components.models.SummaryListData
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.check_your_answers

import scala.jdk.CollectionConverters._

class CheckYourAnswersPageSpec
extends ViewBaseSpec {

  private implicit val langs: Seq[Lang] = Seq(Lang("en"))

  private val view: check_your_answers = app.injector.instanceOf[check_your_answers]

  private val legacyRegimes = List(CT, PAYE, SA)

  private def heading(legacyRegime: LegacyRegime) = messages(s"${legacyRegime.msgPrefix}.check-your-answers.h1")
  private def title(legacyRegime: LegacyRegime) = s"${heading(legacyRegime)} - Agent services account - GOV.UK"

  private def model(legacyRegime: LegacyRegime) = {
    val nameRow = if (legacyRegime == PAYE) {
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.contact-name",
        value = "Manager Employee",
        link = None
      )
    } else {
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.business-name",
        value = "Test Agency",
        link = None
      )
    }
    val commonRows = Seq(
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.phone-number",
        value = "1234567890",
        link = None
      ),
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.email",
        value = "test@test.com",
        link = None
      ),
      SummaryListData(
        key = s"${legacyRegime.msgPrefix}.check-your-answers.address",
        value = "Line 1<br/>Line 2",
        link = None
      )
    )
    nameRow +: commonRows
  }

  legacyRegimes.foreach(legacyRegime => {
    s"check_your_answers view for $legacyRegime" should {

      "render page with heading, summary list and submit button" in {

        val messages: Messages = MessagesImpl(langs.head, messagesApi)

        val doc: Document = Jsoup.parse(
          view(model(legacyRegime), legacyRegime)(
            messages,
            fakeRequest,
            appConfig
          ).body
        )

        doc.title() mustBe title(legacyRegime)

        doc.select("h1").text() mustBe heading(legacyRegime)

        val keys = doc.select(".govuk-summary-list__key").asScala.map(_.text()).toList
        if (legacyRegime == PAYE) {
          keys must contain("Contact name")
          keys must not contain("Business name")
        } else {
          keys must contain("Business name")
          keys must not contain("Contact name")
        }
        keys must contain("Telephone number")
        keys must contain("Email address")
        keys must contain("Address")

        val values = doc.select(".govuk-summary-list__value").asScala.map(_.text()).toList
        if (legacyRegime == PAYE) {
          values must contain("Manager Employee")
          values must not contain("Test Agency")
        } else {
          values must contain("Test Agency")
          values must not contain("Manager Employee")
        }
        values must contain("1234567890")
        values must contain("test@test.com")
        values.exists(_.contains("Line 1")) mustBe true

        val form = doc.select("form")
        form.attr("action") mustBe routes.CheckYourAnswersController.onSubmit(legacyRegime).url

        val button = doc.select("button")
        button.text() mustBe messages(s"${legacyRegime.msgPrefix}.check-your-answers.submit-button")
      }
    }
  })

}
