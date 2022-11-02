/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.data.Forms._
import play.api.data._
import uk.gov.hmrc.agentservicesaccount.models.BetaInviteContactDetails

object BetaInviteContactDetailsForm {

  // matches [anything]@[anything].[anything]
  private val emailRegex = """^.{1,252}@.{1,256}\..{1,256}$"""
  private val phoneRegex = """^[0-9 +()]{0,25}$"""

  def form: Form[BetaInviteContactDetails] = Form(
    mapping(
      "name" -> text
        .verifying("error.required.name", _.trim.nonEmpty)
        .verifying("error.max-length.name", _.trim.length < 81),
      "email" -> text
        .verifying("error.required.email", _.trim.nonEmpty)
        .verifying("error.max-length.email", _.trim.length < 255)
        .verifying("error.invalid.email", x => x.trim.matches(emailRegex) || x.trim.isEmpty),
      "phone" -> optional(text
        .verifying("error.max-length.phone", x => x.trim.length < 21 || x.trim.isEmpty)
        .verifying("error.invalid.phone", x => x.trim.matches(phoneRegex) || x.trim.isEmpty)
      )
    )(BetaInviteContactDetails.apply)(BetaInviteContactDetails.unapply)
  )
}
