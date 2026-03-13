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
import play.api.data.Mapping
import play.api.data.Forms._

case class CtBusinessNameFormValues(
  useDefault: Boolean,
  newBusinessName: String
)
//  TODO: Make newBusinessName: Option[String]

object CtSubscriptionForms {

  private val BusinessNameRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{2,200}$""".r

  private val trimmedText = text.transform[String](x => x.trim, x => x)

//  TODO: NEED YES/NO FOR SUBSCRIPTION BUSINESS NAME

  private val useDefaultMapping: Mapping[Boolean] = optional(boolean)
    .verifying("NEED TO SELECT YES/NO", _.isDefined)
    .transform(_.get, (b: Boolean) => Option(b))

  private val newBusinessNameOptionalMapping: Mapping[String] = trimmedText
    .verifying("update-contact-details.name.error.empty", _.nonEmpty)
    .verifying("update-contact-details.name.error.invalid", x => x.isEmpty || BusinessNameRegex.matches(x))

  def newBusinessNameForm: Form[CtBusinessNameFormValues] = {
    Form(
      mapping(
        "useDefault" -> useDefaultMapping,
        "newBusinessName" -> newBusinessNameOptionalMapping
      )(CtBusinessNameFormValues.apply)(CtBusinessNameFormValues.unapply)
    )
  }

}
