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

package uk.gov.hmrc.agentservicesaccount.models.subscriptions

import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress

case class CtCyaData(
  agencyName: String,
  agencyEmail: String,
  agencyTelephone: String,
  agencyAddress: BusinessAddress
) {
  def toSubscriptionRequest: CtSubscriptionRequest = {
    val address = SubscriptionAddress(
      line1 = agencyAddress.addressLine1,
      line2 = agencyAddress.addressLine2.getOrElse(""),
      line3 = agencyAddress.addressLine3,
      line4 = agencyAddress.addressLine4,
      postCode = agencyAddress.postalCode
    )
    CtSubscriptionRequest(
      agentName = agencyName,
      contactName = agencyName,
      phoneNumber = Some(agencyTelephone),
      emailAddress = Some(agencyEmail),
      address = address,
      countryCode = agencyAddress.countryCode
    )
  }
}
