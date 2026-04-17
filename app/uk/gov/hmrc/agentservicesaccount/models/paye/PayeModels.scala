/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.models.paye

import uk.gov.hmrc.agentservicesaccount.models.subscriptions.PayeSubscriptionRequest
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionAddress

final case class PayeAddress(
  line1: String,
  line2: String,
  line3: Option[String],
  line4: Option[String],
  postCode: String
)

final case class PayeCyaData(
  agentName: String,
  contactName: String,
  telephoneNumber: Option[String],
  emailAddress: Option[String],
  address: PayeAddress
) {

  implicit def toSubscriptionAddress(address: PayeAddress): SubscriptionAddress = {
    val subscriptionAddress = SubscriptionAddress(
      line1 = address.line1,
      line2 = address.line2,
      line3 = address.line3,
      line4 = address.line4,
      postCode = Some(address.postCode)
    )
    subscriptionAddress
  }

  def toPayeSubscriptionRequest: PayeSubscriptionRequest = {
    PayeSubscriptionRequest(
      agentName = agentName,
      contactName = contactName,
      phoneNumber = telephoneNumber,
      emailAddress = emailAddress,
      address = address: SubscriptionAddress
    )
  }

}
