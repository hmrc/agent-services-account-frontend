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

import play.api.data.validation._

import java.time.LocalDate
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object CommonValidators {

  def checkOneAtATime[A](constraints: Seq[Constraint[A]]): Constraint[A] = Constraint[A] { fieldValue: A =>
    @tailrec
    def loop(c: List[Constraint[A]]): ValidationResult =
      c match {
        case Nil => Valid
        case head :: tail =>
          head(fieldValue) match {
            case i@Invalid(_) => i
            case Valid => loop(tail)
          }
      }

    loop(constraints.toList)
  }

   val invalidDateConstraint: Constraint[(String, String, String)] = Constraint[(String, String, String)] { data: (String, String, String) =>
    val (day, month, year)  = data

    Try {
      require(year.length == 4, "Year must be 4 digits")
      LocalDate.of(year.toInt, month.toInt, day.toInt)
    } match {
      case Failure(_) => Invalid(ValidationError("error.updateMoneyLaunderingSupervisory.date.invalid"))
      case Success(_) => Valid
    }
  }

   val pastExpiryDateConstraint: Constraint[(String, String, String)] = Constraint[(String, String, String)] {
    data: (String, String, String) =>
      val (day, month, year) = data

      if (LocalDate.of(year.toInt, month.toInt, day.toInt).isAfter(LocalDate.now()))
        Valid
      else
        Invalid(ValidationError("error.updateMoneyLaunderingSupervisory.date.past"))
  }

   val within13MonthsExpiryDateConstraint: Constraint[(String, String, String)] =
    Constraint[(String, String, String)] { data: (String, String, String) =>
      val (day, month, year ) = data

      val futureDate = LocalDate.now().plusMonths(13)

      if (LocalDate.of(year.toInt, month.toInt, day.toInt).isBefore(futureDate))
        Valid
      else
        Invalid(ValidationError("error.updateMoneyLaunderingSupervisory.date.before"))
    }



}
