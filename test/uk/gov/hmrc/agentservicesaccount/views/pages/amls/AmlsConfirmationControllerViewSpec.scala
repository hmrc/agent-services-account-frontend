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

package uk.gov.hmrc.agentservicesaccount.views.pages.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.update_confirmation_received

class AmlsConfirmationControllerViewSpec
extends ViewBaseSpec {

  val view: update_confirmation_received = inject[update_confirmation_received]

  "update_confirmation_received view" when {
    "the user has changed existing amls details should render the page correctly" in {
      val doc: Document = Jsoup.parse(view.apply(amlsDetailsAlreadyExisted = true)(
        fakeRequest,
        messages,
        appConfig
      ).body)
      doc.select(".govuk-header__service-name").first.text() mustBe "Agent services account"
      doc.select(".govuk-header__service-name").first.attr("href") mustBe "/agent-services-account"

      doc.select(".govuk-panel__title").first().text() mustBe "You've changed your supervision details" // title
      doc.select(".govuk-heading-m").first().text() mustBe "What happens next" // h2
      doc.select(".govuk-body").first().text() mustBe "We'll update your anti-money laundering supervision details on your agent services accounts." // p

      doc.select(".govuk-link").get(2).text() mustBe "Return to manage account"
      doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
      doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
    }
    "the user has provided amls details for the first time should render the page correctly" in {
      val doc: Document = Jsoup.parse(view.apply(amlsDetailsAlreadyExisted = false)(
        fakeRequest,
        messages,
        appConfig
      ).body)
      doc.select(".govuk-header__service-name").first.text() mustBe "Agent services account"
      doc.select(".govuk-header__service-name").first.attr("href") mustBe "/agent-services-account"

      doc.select(".govuk-panel__title").first().text() mustBe "You've added your supervision details" // title
      doc.select(".govuk-heading-m").first().text() mustBe "What happens next" // h2
      doc.select(".govuk-body").first().text() mustBe "We'll update your anti-money laundering supervision details on your agent services accounts." // p

      doc.select(".govuk-link").get(2).text() mustBe "Return to manage account"
      doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
      doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
    }
  }

}
