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

import play.api.data.Forms.{of, single}
import play.api.data.format.Formatter
import play.api.data.validation.{Valid, ValidationResult}
import play.api.data.{Form, FormError}

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

object RenewalDateForm {

  private def invalidDateCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    Try {
      require(year.length == 4, "Year must be 4 digits")
      LocalDate.of(year.toInt, month.toInt, day.toInt)
    } match {
      case Failure(_) => Left("day" -> "update-money-laundering-supervisory.error.date.invalid")
      case Success(_) => Right(Valid)
    }
  }

  private def pastExpiryDateCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (LocalDate.of(year.toInt, month.toInt, day.toInt).isAfter(LocalDate.now())) Right(Valid)
    else Left("day" -> "update-money-laundering-supervisory.error.date.past")
  }

  private def within13MonthsExpiryDateCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    val futureDate = LocalDate.now().plusMonths(13)

    if (LocalDate.of(year.toInt, month.toInt, day.toInt).isBefore(futureDate)) Right(Valid)
    else Left("day" -> "update-money-laundering-supervisory.error.date.before")
  }

  private def noDateCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (s"$day$month$year".isEmpty) Left("day" -> "update-money-laundering-supervisory.error.date")
    else Right(Valid)
  }

  private def noDayAndMonthCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (day.isEmpty && month.isEmpty && year.nonEmpty) {
      Left("day" -> "update-money-laundering-supervisory.error.day-and-month")
    } else Right(Valid)
  }

  private def noDayAndYearCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (day.isEmpty && month.nonEmpty && year.isEmpty) {
      Left("day" -> "update-money-laundering-supervisory.error.day-and-year")
    } else Right(Valid)
  }

  private def noMonthAndYearCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (day.nonEmpty && month.isEmpty && year.isEmpty) {
      Left("month" -> "update-money-laundering-supervisory.error.month-and-year")
    } else Right(Valid)
  }

  private def noDayCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (day.isEmpty && month.nonEmpty && year.nonEmpty) {
      Left("day" -> "update-money-laundering-supervisory.error.day")
    } else Right(Valid)
  }

  private def noMonthCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (day.nonEmpty && month.isEmpty && year.nonEmpty) {
      Left("month" -> "update-money-laundering-supervisory.error.month")
    } else Right(Valid)
  }

  private def noYearCheck(day: String, month: String, year: String): Either[(String, String), ValidationResult] = {
    if (day.nonEmpty && month.nonEmpty && year.isEmpty) {
      Left("year" -> "update-money-laundering-supervisory.error.year")
    } else Right(Valid)
  }

  implicit val dateFormatter: Formatter[LocalDate] = new Formatter[LocalDate] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
      val day: String = data.getOrElse(s"$key.day", "")
      val month: String = data.getOrElse(s"$key.month", "")
      val year: String = data.getOrElse(s"$key.year", "")

      val result = for {
        _ <- noDateCheck(day, month, year)
        _ <- noDayAndMonthCheck(day, month, year)
        _ <- noDayAndYearCheck(day, month, year)
        _ <- noMonthAndYearCheck(day, month, year)
        _ <- noDayCheck(day, month, year)
        _ <- noMonthCheck(day, month, year)
        _ <- noYearCheck(day, month, year)
        _ <- invalidDateCheck(day, month, year)
        _ <- pastExpiryDateCheck(day, month, year)
        _ <- within13MonthsExpiryDateCheck(day, month, year)
        date <- Right(LocalDate.of(year.toInt, month.toInt, day.toInt))
      } yield date

      result match {
        case Left((field, message)) => Left(Seq(FormError(s"$key.$field", message)))
        case Right(date) => Right(date)
      }
    }

    override def unbind(key: String, value: LocalDate): Map[String, String] = Map(
      s"$key.day" -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year" -> value.getYear.toString
    )
  }

  val form: Form[LocalDate] = Form(
    single(
      "endDate" -> of[LocalDate]
    )
  )

}
