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

package uk.gov.hmrc.agentservicesaccount.forms.subscriptions

import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.trimmedText
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.{CtBusinessNameFormValues, CtEmailAddressFormValues}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

object CtSubscriptionEmailAddressForm {

//  TODO: 10904: Check against subscription API regex
  private val EmailAddressRegex = """^.{1,252}@.{1,256}\..{1,256}$""".r

  val emailAddressUseAsaDataKey = "emailAddressUseAsaData"
  val emailAddressNewKey = "emailAddressNew"

  private val emailAddressUseAsaDataMapping: Mapping[Boolean] = optional(boolean)
    .verifying("asa.legacy.ct.email-address.use-asa.error.required", _.isDefined)
    .transform(_.get, (b: Boolean) => Option(b))

  private val emailAddressNewOptionalMapping: Mapping[String] = trimmedText
    .verifying("asa.legacy.ct.email-address.new-input.error.empty", _.nonEmpty)
    .verifying("asa.legacy.ct.email-address.new-input.error.invalid", x => x.isEmpty || EmailAddressRegex.matches(x))

  def form: Form[CtEmailAddressFormValues] = {
    Form(
      mapping(
        emailAddressUseAsaDataKey -> emailAddressUseAsaDataMapping,
        emailAddressNewKey -> mandatoryIfFalse(emailAddressUseAsaDataKey, emailAddressNewOptionalMapping)
      )(CtEmailAddressFormValues.apply)(o => Some(o.useAsaData, o.newEmailAddress))
    )
  }

}
