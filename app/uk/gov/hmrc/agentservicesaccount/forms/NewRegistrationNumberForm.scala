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
import play.api.data.Forms.{single, text}
import play.api.data.validation._

object NewRegistrationNumberForm {

  // remove all spaces from input before matching to ensure correct digit count
  private val trimmedText = text.transform[String](x => x.trim, x => x)
  private val supervisoryNumberRegexNonHmrc = """^[A-Za-z0-9\,\.\'\-\/\ ]{0,100}$""".r
  private val supervisoryNumberRegexHmrc = "X[A-Z]ML00000[0-9]{6}".r


  private def registrationNumberConstraint(isHmrc: Boolean): Constraint[String] =
    Constraint[String] {
      fieldValue: String =>
        if (fieldValue.isEmpty)
          Invalid(ValidationError("amls.enter-registration-number.error.empty"))
        else if (isHmrc && !supervisoryNumberRegexHmrc.matches(fieldValue))
          Invalid(ValidationError("amls.enter-registration-number.error.hmrc.invalid"))
        else if (!supervisoryNumberRegexNonHmrc.matches(fieldValue))
          Invalid(ValidationError("amls.enter-registration-number.error.not-hmrc.invalid"))
        else Valid
    }

  def form(isHmrc: Boolean): Form[String] = Form(
    single(
      "number" -> text.verifying(registrationNumberConstraint(isHmrc))
    )
  )
}
