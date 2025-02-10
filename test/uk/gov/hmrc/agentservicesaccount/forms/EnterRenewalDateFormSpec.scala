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

package uk.gov.hmrc.agentservicesaccount.forms

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class RenewalDateFormSpec extends AnyWordSpec with Matchers {

  val renewalDateDay = "renewalDate.day"
  val renewalDateMonth = "renewalDate.month"
  val renewalDateYear = "renewalDate.year"
  val renewalDateField = "renewalDate"

  val local_valid_date_stub: LocalDate = LocalDate.now.plusMonths(6)
  val local_future_date_stub: LocalDate = LocalDate.now.plusYears(2)
  val local_past_date_stub: LocalDate = LocalDate.now()

  val validFormSubmission: Map[String, String] = Map(
    renewalDateDay -> local_valid_date_stub.getDayOfMonth.toString,
    renewalDateMonth -> local_valid_date_stub.getMonthValue.toString,
    renewalDateYear -> local_valid_date_stub.getYear.toString,
  )

  val formSubmissionWithNoRegDate: Map[String, String] = Map(
    renewalDateDay -> "",
    renewalDateMonth -> "",
    renewalDateYear -> ""
  )

  private def invalidateFormSubmission(badData: (String, String)): Map[String, String] =
    (validFormSubmission - badData._1) ++ List(badData)

  private def partialDateFormSubmission(datePart: (String, String)): Map[String, String] =
    (formSubmissionWithNoRegDate - datePart._1) ++ List(datePart)

  "RenewalDateForm binding" should {
    "be successful valid data is submitted" in {
      val params = validFormSubmission
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.value shouldBe
        Some(local_valid_date_stub)
    }

    "get form data from a completed 'EnterRenewalDate' model" in {
      val form = RenewalDateForm.form.fill(local_valid_date_stub)
      val params = validFormSubmission
      form.data shouldBe params
    }

    s"error when no data is submitted" in {
      val params = Map(
        renewalDateDay -> "",
        renewalDateMonth -> "",
        renewalDateYear -> ""
      )
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.length shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date"
    }

    "error when date is not supplied" in {
      val params = formSubmissionWithNoRegDate
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date"
    }

    "error when day is not supplied" in {
      val params = invalidateFormSubmission(renewalDateDay -> "")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.day"
    }

    "error when month is not supplied" in {
      val params = invalidateFormSubmission(renewalDateMonth -> "")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateMonth).get.message shouldBe "update-money-laundering-supervisory.error.month"
    }

    "error when year is not supplied" in {
      val params = invalidateFormSubmission(renewalDateYear -> "")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateYear).get.message shouldBe "update-money-laundering-supervisory.error.year"
    }

    "error when day and month is not supplied" in {
      val params = partialDateFormSubmission(renewalDateYear -> local_valid_date_stub.getYear.toString)
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.day-and-month"
    }

    "error when day and year is not supplied" in {
      val params = partialDateFormSubmission(renewalDateMonth -> local_valid_date_stub.getMonthValue.toString)
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.day-and-year"
    }

    "error when month and year is not supplied" in {
      val params = partialDateFormSubmission(renewalDateDay -> local_valid_date_stub.getDayOfMonth.toString)
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateMonth).get.message shouldBe "update-money-laundering-supervisory.error.month-and-year"
    }

    s"error when the day is invalid" in {
      val params = invalidateFormSubmission(renewalDateDay -> "222")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when the month is invalid" in {
      val params = invalidateFormSubmission(renewalDateMonth -> "333")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when the year is invalid" in {
      val params = invalidateFormSubmission(renewalDateYear -> "###")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when the registration date is not a real date" in {
      val params = Map(
        renewalDateDay -> "31",
        renewalDateMonth -> "02",
        renewalDateYear -> local_valid_date_stub.getYear.toString
      )
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when the registration date is an invalid date (edge case)" in {
      val params = invalidateFormSubmission(renewalDateMonth -> "-1")
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when the registration date is more than 13 Months in the future" in {
      val params = Map(
        renewalDateDay -> local_future_date_stub.getDayOfMonth.toString,
        renewalDateMonth -> local_future_date_stub.getMonthValue.toString,
        renewalDateYear -> local_future_date_stub.getYear.toString
      )
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.before"
    }

    s"error when the registration date is before today's date" in {
      val params = Map(
        renewalDateDay -> local_past_date_stub.getDayOfMonth.toString,
        renewalDateMonth -> local_past_date_stub.getMonthValue.toString,
        renewalDateYear -> local_past_date_stub.getYear.toString,
      )
      val validatedForm = RenewalDateForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(renewalDateDay).get.message shouldBe "update-money-laundering-supervisory.error.date.past"
    }
  }
}