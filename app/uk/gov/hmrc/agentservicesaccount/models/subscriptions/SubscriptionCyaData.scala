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

case class SubscriptionCyaData(
  businessName: String,
  phoneNumber: String,
  email: String,
  address: BusinessAddress
) {
  def toCtSubscriptionRequest: CtSubscriptionRequest = {
    val subscriptionAddress = SubscriptionAddress(
      line1 = address.addressLine1,
      line2 = address.addressLine2.getOrElse(""),
      line3 = address.addressLine3,
      line4 = address.addressLine4,
      postCode = address.postalCode
    )
    CtSubscriptionRequest(
      agentName = businessName,
      contactName = businessName,
      phoneNumber = Some(phoneNumber),
      emailAddress = Some(email),
      address = subscriptionAddress,
      countryCode = address.countryCode
    )
  }

  def toSaSubscriptionRequest: SaSubscriptionRequest = {
    val subscriptionAddress = SubscriptionAddress(
      line1 = address.addressLine1,
      line2 = address.addressLine2.getOrElse(""),
      line3 = address.addressLine3,
      line4 = address.addressLine4,
      postCode = address.postalCode
    )
    SaSubscriptionRequest(
      agentName = businessName,
      contactName = businessName,
      phoneNumber = Some(phoneNumber),
      emailAddress = Some(email),
      address = subscriptionAddress,
      countryCode = address.countryCode
    )
  }
}

object SubscriptionCyaData {
  implicit def subscriptionJourneyToCyaData(journey: SubscriptionJourney): Option[SubscriptionCyaData] = {
    for {
      businessName <-
        journey.useCustomBusinessName match {
          case Some(true) => journey.businessNameAnswer
          case _ => journey.asaDetails.agencyName
        }
      phoneNumber <-
        journey.useCustomPhoneNumber match {
          case Some(true) => journey.phoneNumberAnswer
          case _ => journey.asaDetails.agencyTelephone
        }
      email <-
        journey.useCustomEmail match {
          case Some(true) => journey.emailAnswer
          case _ => journey.asaDetails.agencyEmail
        }
      address <-
        journey.useCustomAddress match {
          case Some(true) => journey.addressAnswer
          case _ => journey.asaDetails.agencyAddress
        }
    } yield SubscriptionCyaData(
      businessName = businessName,
      phoneNumber = phoneNumber,
      email = email,
      address = address
    )
  }
}
