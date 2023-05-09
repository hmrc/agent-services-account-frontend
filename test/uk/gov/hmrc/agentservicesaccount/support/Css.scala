/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.support

object Css {
  val ERROR_SUMMARY_TITLE = "#error-summary-title"
  val ERROR_SUMMARY_LINK = ".govuk-list.govuk-error-summary__list li a"
  val errorSummaryLinkWithHref: String => String = (href: String)  => s".govuk-list.govuk-error-summary__list li a[href=$href]"
  val H1 = "main h1"
  val H2 = "main h2"
  val H3 = "main h3"
  val H4 = "main h4"
  val LI = "main ul li"
  val PRE_H1 = "main .govuk-caption-l"
  val paragraphs = "main p"
  val insetText = "div.govuk-inset-text"
  val detailsSummary = "details.govuk-details .govuk-details__summary-text"
  val detailsText = "details.govuk-details .govuk-details__text"
  val summaryListKeys = "dl.govuk-summary-list .govuk-summary-list__key"
  val summaryListValues = "dl.govuk-summary-list .govuk-summary-list__value"
  def errorSummaryForField(id: String): String = {
    s".govuk-error-summary__body li a[href=#$id]"
  }
  def errorForField(id: String): String = s"span#$id-error"
  def labelFor(id: String): String = s"label[for=$id]"
  val SUBMIT_BUTTON = "main form button"
  val linkStyledAsButton = "a.govuk-button"
  val link = "main a"
  val currentLanguage = "ul.hmrc-language-select__list li.hmrc-language-select__list-item span[aria-current=true]"
  val alternateLanguage = ".hmrc-language-select__list .hmrc-language-select__list-item a.govuk-link"
  val getHelpWithThisPageLink = "main a.govuk-link.hmrc-report-technical-issue"
  val backLink = "a.govuk-back-link"
  val secondaryNavLinks = "#secondary-nav a"
}
