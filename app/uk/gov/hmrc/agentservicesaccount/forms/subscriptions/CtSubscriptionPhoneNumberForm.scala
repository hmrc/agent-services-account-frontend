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
import play.api.data.Form
import play.api.data.Mapping
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.trimmedText
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.useAsaDataMapping
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtBusinessNameFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtPhoneNumberFormValues
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

object CtSubscriptionPhoneNumberForm {

  val phoneNumberUseAsaDataKey = "phoneNumberUseAsaData"
  val phoneNumberNewKey = "phoneNumberNew"

  // UI validation allows '+', spaces and parentheses for user-friendly input.
  // Raw value is stored in cache; formatting characters are stripped only
  // at final submission to meet API requirements of digits only.
  private val phoneNumberRegex = """^(?=.*\d)[0-9 +()]+$""".r

  private val phoneNumberUseAsaDataMapping = useAsaDataMapping("asa.legacy.ct.phone-number.use-asa.error.required")

  private def isPhoneNumberValid(x: String): Boolean = {
    val digits = x.replaceAll("[^0-9]", "")
    phoneNumberRegex.matches(x) && digits.length <= 20
  }

  private val phoneNumberNewOptionalMapping: Mapping[String] = trimmedText
    .verifying("asa.legacy.ct.phone-number.new-input.error.empty", _.nonEmpty)
    .verifying("asa.legacy.ct.phone-number.new-input.error.invalid", x => x.isEmpty || isPhoneNumberValid(x))

  def form: Form[CtPhoneNumberFormValues] = {
    Form(
      mapping(
        phoneNumberUseAsaDataKey -> phoneNumberUseAsaDataMapping,
        phoneNumberNewKey -> mandatoryIfFalse(phoneNumberUseAsaDataKey, phoneNumberNewOptionalMapping)
      )(CtPhoneNumberFormValues.apply)(o => Some(o.useAsaData, o.newPhoneNumber))
    )
  }

}
