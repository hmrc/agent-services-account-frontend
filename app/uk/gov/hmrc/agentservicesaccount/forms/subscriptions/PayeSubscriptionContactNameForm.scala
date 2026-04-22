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
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.trimmedAndNormalisedText
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.useAsaDataMapping
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.PayeContactNameFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

//TODO: 11186 Need to correct this code
object PayeSubscriptionContactNameForm {

  val businessNameUseAsaDataKey = "businessNameUseAsaData"
  val businessNameNewKey = "businessNameNew"

  private val businessNameRegex = """^[A-Za-z0-9\(\)&\-\'‘’\/,\. ]{1,54}$""".r

  private def businessNameUseAsaDataMapping(legacyRegime: LegacyRegime): Mapping[Boolean] = useAsaDataMapping(
    s"${legacyRegime.msgPrefix}.contact-name.use-asa.error.required"
  )

  private def businessNameNewOptionalMapping(legacyRegime: LegacyRegime): Mapping[String] = trimmedAndNormalisedText
    .verifying(s"${legacyRegime.msgPrefix}.contact-name.new-input.error.empty", _.nonEmpty)
    .verifying(s"${legacyRegime.msgPrefix}.contact-name.new-input.error.invalid", x => x.isEmpty || businessNameRegex.matches(x))

  def form(legacyRegime: LegacyRegime): Form[PayeContactNameFormValues] = {
    Form(
      mapping(
        businessNameUseAsaDataKey -> businessNameUseAsaDataMapping(legacyRegime),
        businessNameNewKey -> mandatoryIfFalse(businessNameUseAsaDataKey, businessNameNewOptionalMapping(legacyRegime))
      )(PayeContactNameFormValues.apply)(o => Some(o.useAsaData, o.newBusinessName))
    )
  }

}
