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

import play.api.data.Forms.*
import play.api.data.Form
import play.api.data.Mapping
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.trimmedAndNormalisedText
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.STT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.ContactNameFormValues

object SubscriptionContactNameForm {

  val contactNameKey = "contactName"

  private[subscriptions] val PAYE_CONTACT_NAME_MAX_LENGTH = 56
  private[subscriptions] val STT_CONTACT_NAME_MAX_LENGTH = 40

  private def contactNameRegex(agentRegime: AgentRegime) =
    agentRegime match
      case PAYE => s"^[A-Za-z0-9 .,()@!-]{1,$PAYE_CONTACT_NAME_MAX_LENGTH}$$".r
      case STT => s"^[A-Za-z0-9 .,()@!-]{1,$STT_CONTACT_NAME_MAX_LENGTH}$$".r

  private def contactNameMapping(agentRegime: AgentRegime): Mapping[String] =
    val maxLength =
      agentRegime match {
        case PAYE => PAYE_CONTACT_NAME_MAX_LENGTH
        case STT => STT_CONTACT_NAME_MAX_LENGTH
      }
    trimmedAndNormalisedText
      .verifying(s"${agentRegime.msgPrefix}.contact-name.input.error.empty", _.nonEmpty)
      .verifying(s"${agentRegime.msgPrefix}.contact-name.input.error.length", _.length <= maxLength)
      .verifying(
        s"${agentRegime.msgPrefix}.contact-name.input.error.invalid",
        x => x.isEmpty || x.length > maxLength || contactNameRegex(agentRegime).matches(x)
      )

  def form(agentRegime: AgentRegime): Form[ContactNameFormValues] = {
    Form(
      mapping(
        contactNameKey -> contactNameMapping(agentRegime)
      )(ContactNameFormValues.apply)(o => Some(o.contactName))
    )
  }

}
