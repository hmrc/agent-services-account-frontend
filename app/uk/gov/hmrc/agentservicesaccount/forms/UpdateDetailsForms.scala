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

package uk.gov.hmrc.agentservicesaccount.forms

import play.api.data.Form
import play.api.data.Forms.{single, text}

object UpdateDetailsForms {
  // TODO - verify the validity of these regexes. There must be standard validators for these things in some HMRC library!
  private val BusinessNameRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]+$""".r
  private val TelephoneNumberRegex = """^\+[0-9 ]{1,18}$|^[0-9 ]{1,19}$|^(?=.{2,22}$)\+[0-9 ]*\(0\)[0-9 ]*$|^(?=.{1,22}$)[0-9 ]*\(0\)[0-9 ]*$""".r
  private val EmailAddressRegex = """^.+[@].+[.].+$""".r

  private val trimmedText = text.transform[String](x => x.trim, x => x)

  val businessNameForm: Form[String] = Form(
    single("name" -> trimmedText)
      .verifying("update-contact-details.name.error.empty", _.nonEmpty)
      .verifying("update-contact-details.name.error.invalid", x => x.isEmpty || BusinessNameRegex.matches(x))
  )
  val telephoneNumberForm: Form[String] = Form(
    single("telephoneNumber" -> trimmedText)
      .verifying("update-contact-details.phone.error.empty", _.nonEmpty)
      .verifying("update-contact-details.phone.error.invalid", x => x.isEmpty || TelephoneNumberRegex.matches(x))
  )
  val emailAddressForm: Form[String] = Form(
    single("emailAddress" -> trimmedText)
      .verifying("update-contact-details.email.error.empty", _.nonEmpty)
      .verifying("update-contact-details.email.error.invalid", x => x.isEmpty || EmailAddressRegex.matches(x))
  )
  // TODO implement other forms as necessary
}
