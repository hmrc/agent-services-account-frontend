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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.EmailAddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

object SubscriptionEmailAddressForm {

  val emailAddressUseAsaDataKey = "emailAddressUseAsaData"
  val emailAddressNewKey = "emailAddressNew"

  private def emailAddressUseAsaDataMapping(legacyRegime: LegacyRegime): Mapping[Boolean] = useAsaDataMapping(
    //    TODO: 11329 Need to pass in businessName
    s"${legacyRegime.msgPrefix}.email-address.use-asa.error.required"
  )

  private def emailAddressNewOptionalMapping(legacyRegime: LegacyRegime): Mapping[String] = trimmedText
    .verifying(s"${legacyRegime.msgPrefix}.email-address.new-input.error.empty", _.nonEmpty)
    .verifying(s"${legacyRegime.msgPrefix}.email-address.new-input.error.invalid", x => x.isEmpty || (x.length <= 50 && x.contains("@")))

  def form(legacyRegime: LegacyRegime): Form[EmailAddressFormValues] = {
    Form(
      mapping(
        emailAddressUseAsaDataKey -> emailAddressUseAsaDataMapping(legacyRegime),
        emailAddressNewKey -> mandatoryIfFalse(emailAddressUseAsaDataKey, emailAddressNewOptionalMapping(legacyRegime))
      )(EmailAddressFormValues.apply)(o => Some(o.useAsaData, o.newEmailAddress))
    )
  }

}
