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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.agentservicesaccount.models.UpdateMoneyLaunderingSupervisionDetails

import java.time.LocalDate

class UpdateMoneyLaunderingSupervisionFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val bodyField = "body"
  val numberField = "number"
  val endDateDay = "endDate.day"
  val endDateMonth = "endDate.month"
  val endDateYear = "endDate.year"
  val endDateField = "endDate"

  val local_valid_date_stub: LocalDate = LocalDate.now.plusMonths(6)
  val local_future_date_stub: LocalDate = LocalDate.now.plusYears(2)
  val local_past_date_stub: LocalDate = LocalDate.now()

  "UpdateMoneyLaunderingSupervisionForm binding" should {
    "be successful when not empty" in {
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",
        endDateDay -> local_valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_valid_date_stub.getMonthValue.toString,
        endDateYear-> local_valid_date_stub.getYear.toString,
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.value shouldBe
        Some(UpdateMoneyLaunderingSupervisionDetails("Blah alkfh", "1122334455", local_valid_date_stub))
    }
    s"error when $bodyField and $numberField and $endDateField are empty" in {
      val params = Map(
        bodyField -> "",
        numberField -> "",
        endDateDay -> "",
        endDateMonth -> "",
        endDateYear -> ""
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(bodyField).get.message shouldBe "update-money-laundering-supervisory.body-codes.error.empty"
      validatedForm.error(numberField).get.message shouldBe "update-money-laundering-supervisory.reg-number.error.empty"
      validatedForm.error(endDateDay).get.message shouldBe "update-money-laundering-supervisory.error.day"
      validatedForm.error(endDateMonth).get.message shouldBe "update-money-laundering-supervisory.error.month"
      validatedForm.error(endDateYear).get.message shouldBe "update-money-laundering-supervisory.error.year"
      validatedForm.errors.length shouldBe 5
    }
    s"error when $bodyField is invalid" in {
      val params = Map(
        bodyField -> "###",
        numberField -> "11223344",
        endDateDay -> local_valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_valid_date_stub.getMonthValue.toString,
        endDateYear-> local_valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(bodyField).get.message shouldBe "update-money-laundering-supervisory.body-codes.error.invalid"
    }
    s"error when $numberField is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "###",
        endDateDay -> local_valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_valid_date_stub.getMonthValue.toString,
        endDateYear-> local_valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.errors.size shouldBe 1
      validatedForm.error(numberField).get.message shouldBe "update-money-laundering-supervisory.reg-number.error.invalid"
    }
    s"error when $endDateDay is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "11223344",
        endDateDay -> "222",
        endDateMonth -> local_valid_date_stub.getMonthValue.toString,
        endDateYear -> local_valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 1
      validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid" // "day" check msg
    }
    s"error when $endDateMonth is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "11223344",
        endDateDay -> local_valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> "333",
        endDateYear -> local_valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 1
      validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid" //"month" check msg
    }
    s"error when $endDateYear is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "11223344",
        endDateDay -> local_valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_valid_date_stub.getMonthValue.toString,
        endDateYear -> "###"
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 1
      validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid" // "year" check msg
    }
    s"error when $endDateField passes the regex format however is an invalid date" in{
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",
        endDateDay -> "31",
        endDateMonth -> "02",
        endDateYear -> local_valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
        validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when $endDateField passes the regex format however is an invalid date (edge case)" in {
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",
        endDateDay -> "0",
        endDateMonth -> "-1",
        endDateYear -> local_valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.invalid"
    }

    s"error when $endDateField range isn't within 13 Months of today's date" in {
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",
        endDateDay -> local_future_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_future_date_stub.getMonthValue.toString,
        endDateYear -> local_future_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.before"
    }
    s"error when $endDateField is before today's date" in {
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",
        endDateDay -> local_past_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_past_date_stub.getMonthValue.toString,
        endDateYear -> local_past_date_stub.getYear.toString,
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.error(endDateField).get.message shouldBe "update-money-laundering-supervisory.error.date.past"
    }
  }
}