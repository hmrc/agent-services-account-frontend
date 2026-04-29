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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.PayeContactNameFormValues

object PayeSubscriptionContactNameForm {

  val contactNameKey = "contactName"

  private val contactNameRegex = """^[A-Za-z0-9\(\)&\-\'‘’\/,\. ]{1,54}$""".r

  private val contactNameMapping: Mapping[String] = trimmedAndNormalisedText
    .verifying("asa.legacy.paye.contact-name.input.error.empty", _.nonEmpty)
    .verifying("asa.legacy.paye.contact-name.input.error.invalid", x => x.isEmpty || contactNameRegex.matches(x))

  def form: Form[PayeContactNameFormValues] = {
    Form(
      mapping(
        contactNameKey -> contactNameMapping
      )(PayeContactNameFormValues.apply)(o => Some(o.contactName))
    )
  }

}
