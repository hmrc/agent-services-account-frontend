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
import play.api.data.Forms.{boolean, mapping, optional, single, text}
import uk.gov.hmrc.agentservicesaccount.models.{ApplyCtCodeChanges, ApplySaCodeChanges}
import uk.gov.hmrc.agentservicesaccount.models.{ApplySaCodeChanges, YourDetails}

object UpdateDetailsForms {
  private val BusinessNameRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{2,200}$""".r
  private val TelephoneNumberRegex = """^(\+44|0)\d{9,12}$""".r // remove all spaces from input before matching to ensure correct digit count
  private val EmailAddressRegex = """^.{1,252}@.{1,256}\..{1,256}$""".r
  private val SaCodeRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{6}$""".r
  private val CtCodeRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{6}$""".r
  private val InternationalTelephoneRegex = """^[0-9 +()]{0,25}$""".r
  private val NameRegex = "^[A-Za-z0-9 \\-,.&'\\/]*$".r

  private val trimmedText = text.transform[String](x => x.trim, x => x)

  val businessNameForm: Form[String] = Form(
    single("name" -> trimmedText
      .verifying("update-contact-details.name.error.empty", _.nonEmpty)
      .verifying("update-contact-details.name.error.invalid", x => x.isEmpty || BusinessNameRegex.matches(x))
    )
  )
  val telephoneNumberForm: Form[String] = Form(
    single("telephoneNumber" -> trimmedText
      .verifying("update-contact-details.phone.error.empty", _.nonEmpty)
      .verifying("update-contact-details.phone.error.invalid", x => x.isEmpty || TelephoneNumberRegex.matches(x.replace(" ","")))
    )
  )
  val emailAddressForm: Form[String] = Form(
    single("emailAddress" -> trimmedText
      .verifying("update-contact-details.email.error.empty", _.nonEmpty)
      .verifying("update-contact-details.email.error.invalid", x => x.isEmpty || EmailAddressRegex.matches(x))
    )
  )

  val applySaCodeChangesForm: Form[ApplySaCodeChanges] = Form(
    mapping(
      "applyChanges" -> optional(boolean)
        .verifying("update-contact-details.apply-sa-code-changes.error.empty", _.isDefined)
        .transform(_.get, (b: Boolean) => Some(b))
    )(ApplySaCodeChanges.apply)(ApplySaCodeChanges.unapply)
  )

  val saCodeForm: Form[String] = Form(
    single("saCode" -> trimmedText
      .verifying("update-contact-details.sa-code.error.empty", _.nonEmpty)
      .verifying("update-contact-details.sa-code.error.invalid", x => x.isEmpty || SaCodeRegex.matches(x.replace(" ","")))
    )
  )

  val yourDetailsForm: Form[YourDetails] = Form(
    mapping(
      "fullName" -> trimmedText
        .verifying("update-contact-details.your-details.name.error.empty", _.nonEmpty)
        .verifying("update-contact-details.your-details.name.error.invalid", x => x.isEmpty || NameRegex.matches(x.replace(" ","")))
        .verifying("update-contact-details.your-details.name.error.tooLong", x => x.isEmpty || x.length < 41),
      "telephone" -> trimmedText
        .verifying("update-contact-details.your-details.telephone.error.empty", _.nonEmpty)
        .verifying("update-contact-details.your-details.telephone.error.invalid", x => x.isEmpty || InternationalTelephoneRegex.matches(x.replace(" ","")))
    )(YourDetails.apply)(YourDetails.unapply)
  )

  val applyCtCodeChangesForm: Form[ApplyCtCodeChanges] = Form(
    mapping(
      "applyChanges" -> optional(boolean)
        .verifying("update-contact-details.apply-ct-code-changes.error.empty", _.isDefined)
        .transform(_.get, (b: Boolean) => Some(b))
    )(ApplyCtCodeChanges.apply)(ApplyCtCodeChanges.unapply)
  )

  val ctCodeForm: Form[String] = Form(
    single("ctCode" -> trimmedText
      .verifying("update-contact-details.ct-code.error.empty", _.nonEmpty)
      .verifying("update-contact-details.ct-code.error.invalid", x => x.isEmpty || CtCodeRegex.matches(x.replace(" ","")))
    )
  )
}
