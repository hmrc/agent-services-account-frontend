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

class UpdateMoneyLaunderingSupervisionFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite{

  val bodyField = "body"
  val numberField = "number"
  val endDateDay = "endDate.day"
  val endDateMonth = "endDate.month"
  val endDateYear = "endDate.year"
  val endDateField = "endDate"



  val local_Valid_date_stub = LocalDate.now.plusMonths(6)
  val local_future_date_stub = LocalDate.now.plusYears(2)
  val local_past_date_stub = LocalDate.now()

  "UpdateMoneyLaunderingSupervisionForm binding" should {
    "be successful when not empty" in {
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",

        endDateDay -> local_Valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_Valid_date_stub.getMonthValue.toString,
        endDateYear-> local_Valid_date_stub.getYear.toString,
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)

      validatedForm.value shouldBe
        Some(UpdateMoneyLaunderingSupervisionDetails("Blah alkfh", "1122334455", local_Valid_date_stub))

    }

    s"error when $bodyField and $numberField and $endDateField are empty" in {
      val params = Map(
        bodyField -> "",
        numberField -> "",
        //endDateField -> "" needs to be looked into
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(bodyField).get.message shouldBe "update-contact-details.codes.body.error.empty" // check msg
      validatedForm.error(numberField).get.message shouldBe "update-contact-details.reg.number.error.empty"
      //validatedForm.error(endDateField).get.message shouldBe "day" needs to be looked into
      validatedForm.errors.length shouldBe 7
    }
    s"error when $bodyField is invalid" in {
      val params = Map(
        bodyField -> "###",
        numberField -> "11223344",
        endDateField -> local_Valid_date_stub.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 4
      validatedForm.error(bodyField).get.message shouldBe "update-contact-details.codes.body.error.invalid" // check msg
    }

    s"error when $numberField is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "###",
        endDateField -> local_Valid_date_stub.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 4
      validatedForm.error(numberField).get.message shouldBe "update-contact-details.reg.number.error.invalid" // check msg
    }


    s"error when $endDateDay is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "11223344",

      endDateDay -> "222",
      endDateMonth -> local_Valid_date_stub.getMonthValue.toString,
      endDateYear -> local_Valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 1
      validatedForm.error(endDateField).get.message shouldBe "error.updateMoneyLaunderingSupervisory.date.invalid" // "day" check msg
    }

    s"error when $endDateMonth is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "11223344",

        endDateDay -> local_Valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> "333",
        endDateYear -> local_Valid_date_stub.getYear.toString
      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 1
      validatedForm.error(endDateField).get.message shouldBe "error.updateMoneyLaunderingSupervisory.date.invalid" //"month" check msg
    }

    s"error when $endDateYear is invalid" in {
      val params = Map(
        bodyField -> "AABBCC",
        numberField -> "11223344",

        endDateDay -> local_Valid_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_Valid_date_stub.getMonthValue.toString,
        endDateYear -> "###"

      )
      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.errors.size shouldBe 1
      validatedForm.error(endDateField).get.message shouldBe "error.updateMoneyLaunderingSupervisory.date.invalid" // "year" check msg
    }

    s"error when $endDateField passes the regex format however is an invalid date" in{
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",

        endDateDay -> "31",
        endDateMonth -> "02",
        endDateYear -> local_Valid_date_stub.getYear.toString
      )

      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)

        validatedForm.error(endDateField).get.message shouldBe "error.updateMoneyLaunderingSupervisory.date.invalid"
    }

    s"error when $endDateField range isn't within 13Months of today's date" in{
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",

        endDateDay -> local_future_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_future_date_stub.getMonthValue.toString,
        endDateYear -> local_Valid_date_stub.getYear.toString
      )

      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)

        validatedForm.error(endDateField).get.message shouldBe "error.updateMoneyLaunderingSupervisory.date.past"
      // should pass using this msg "error.updateMoneyLaunderingSupervisory.date.before"

    }

    s"error when $endDateField is before today's date" in{
      val params = Map(
        bodyField -> "Blah alkfh",
        numberField -> "1122334455",

        endDateDay -> local_past_date_stub.getDayOfMonth.toString,
        endDateMonth -> local_past_date_stub.getMonthValue.toString,
        endDateYear -> local_past_date_stub.getYear.toString,
      )

      val validatedForm = UpdateMoneyLaunderingSupervisionForm.form.bind(params)

        validatedForm.error(endDateField).get.message shouldBe "error.updateMoneyLaunderingSupervisory.date.past"
    }
  }
}