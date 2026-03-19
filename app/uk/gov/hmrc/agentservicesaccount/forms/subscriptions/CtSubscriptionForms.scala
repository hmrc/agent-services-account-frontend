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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtBusinessNameFormValues
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

//TODO: 10902 Add Form Spec

object CtSubscriptionForms {

  private val trimmedText = text.transform[String](x => x.trim, x => x)

  private val businessNameUseAsaDataKey = "businessNameUseAsaData"
  private val businessNameNewKey = "businessNameNew"

//  TODO: 10902: Check against subscription API regex
  private val BusinessNameRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{2,200}$""".r

  private val businessNameUseAsaDataMapping: Mapping[Boolean] = optional(boolean)
    .verifying("asa.legacy.ct.business-name.use-asa.error.required", _.isDefined)
    .transform(_.get, (b: Boolean) => Option(b))

  private val businessNameNewOptionalMapping: Mapping[String] = trimmedText
    .verifying("asa.legacy.ct.business-name.now-input.error.empty", _.nonEmpty)
    .verifying("asa.legacy.ct.business-name.now-input.error.invalid", x => x.isEmpty || BusinessNameRegex.matches(x))

  def newBusinessNameForm: Form[CtBusinessNameFormValues] = {
    Form(
      mapping(
        businessNameUseAsaDataKey -> businessNameUseAsaDataMapping,
        businessNameNewKey -> mandatoryIfFalse(businessNameUseAsaDataKey, businessNameNewOptionalMapping)
      )(CtBusinessNameFormValues.apply)(o => Some(o.useAsaData, o.newBusinessName))
    )
  }

}
