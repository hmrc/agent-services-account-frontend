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

import play.api.data.Form
import play.api.data.Forms.{mapping, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators._
import uk.gov.hmrc.agentservicesaccount.models.UpdateMoneyLaunderingSupervisionDetails
import java.time.LocalDate
import scala.util.{Failure, Success, Try}


object UpdateMoneyLaunderingSupervisionForm {
  private val supervisoryBodyRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{0,200}$""".r
  private val supervisoryNumberRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{0,200}$""".r // remove all spaces from input before matching to ensure correct digit count
  private val trimmedText = text.transform[String](x => x.trim, x => x)

  private val invalidDateConstraint: Constraint[(String, String, String)] = Constraint[(String, String, String)] { data: (String, String, String) =>
    val (day, month, year)  = data

    Try {
      require(year.length == 4, "Year must be 4 digits")
      LocalDate.of(year.toInt, month.toInt, day.toInt)
    } match {
      case Failure(_) => Invalid(ValidationError("update-money-laundering-supervisory.error.date.invalid"))
      case Success(_) => Valid
    }
  }

  private val pastExpiryDateConstraint: Constraint[(String, String, String)] = Constraint[(String, String, String)] {
    data: (String, String, String) =>
      val (day, month, year) = data

      if (LocalDate.of(year.toInt, month.toInt, day.toInt).isAfter(LocalDate.now()))
        Valid
      else
        Invalid(ValidationError("update-money-laundering-supervisory.error.date.past"))
  }

  private val within13MonthsExpiryDateConstraint: Constraint[(String, String, String)] =
    Constraint[(String, String, String)] { data: (String, String, String) =>
      val (day, month, year) = data
      val futureDate = LocalDate.now().plusMonths(13)

      if (LocalDate.of(year.toInt, month.toInt, day.toInt).isBefore(futureDate))
        Valid
      else
        Invalid(ValidationError("update-money-laundering-supervisory.error.date.before"))
    }

  val form: Form[UpdateMoneyLaunderingSupervisionDetails] =
    Form(
      mapping(
        "body" -> trimmedText
          .verifying("update-money-laundering-supervisory.body-codes.error.empty", _.nonEmpty)
          .verifying("update-money-laundering-supervisory.body-codes.error.invalid", x => supervisoryBodyRegex.matches(x)),
        "number" -> trimmedText
          .verifying("update-money-laundering-supervisory.reg-number.error.empty", _.nonEmpty)
          .verifying("update-money-laundering-supervisory.reg-number.error.invalid", x => supervisoryNumberRegex.matches(x.replace(" ", ""))),
        "endDate" ->
          tuple(
            "day" -> text.verifying("update-money-laundering-supervisory.error.day", d => d.trim.nonEmpty || d.matches("^[0-9]{1,2}$")),
            "month" -> text.verifying("update-money-laundering-supervisory.error.month", m => m.trim.nonEmpty || m.matches("^[0-9]{1,2}$")),
            "year" -> text.verifying("update-money-laundering-supervisory.error.year", y => y.trim.nonEmpty || y.matches("^[0-9]{1,4}$"))
          ).verifying(checkOneAtATime(Seq(invalidDateConstraint, pastExpiryDateConstraint, within13MonthsExpiryDateConstraint)))
            .transform[LocalDate](
              { case (d, m, y) => LocalDate.of( y.trim.toInt, m.trim.toInt, d.trim.toInt) },
              (date: LocalDate) => (date.getYear.toString, date.getMonthValue.toString, date.getDayOfMonth.toString))
      )(UpdateMoneyLaunderingSupervisionDetails.apply)(UpdateMoneyLaunderingSupervisionDetails.unapply)
      )
}
