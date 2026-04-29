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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA

case class SubscriptionCyaData(
  name: String,
  phoneNumber: String,
  email: String,
  address: BusinessAddress
) {

  private def toSubscriptionAddress(
    address: BusinessAddress,
    countryName: Option[String] = None
  ): SubscriptionAddress = {
    val line4: Option[String] =
      (address.countryCode == "GB", countryName.isDefined) match {
        case (false, true) => countryName
        case _ => address.addressLine4
      }
    val subscriptionAddress = SubscriptionAddress(
      line1 = address.addressLine1,
      line2 = address.addressLine2.getOrElse(""),
      line3 = address.addressLine3,
      line4 = line4,
      postCode = address.postalCode
    )
    subscriptionAddress
  }

  private def toCtSubscriptionRequest(countryName: String): CtSubscriptionRequest = {
    CtSubscriptionRequest(
      agentName = name,
      contactName = name,
      phoneNumber = Some(phoneNumber),
      emailAddress = Some(email),
      address = toSubscriptionAddress(address, Some(countryName)),
      countryCode = address.countryCode
    )
  }

  private def toPayeSubscriptionRequest: PayeSubscriptionRequest = {
    //      TODO: 11188 Pass in agentName for PAYE
    PayeSubscriptionRequest(
      agentName = name,
      contactName = name,
      phoneNumber = Some(phoneNumber),
      emailAddress = Some(email),
      address = toSubscriptionAddress(address)
    )
  }

  private def toSaSubscriptionRequest(countryName: String): SaSubscriptionRequest = {
    SaSubscriptionRequest(
      agentName = name,
      contactName = name,
      phoneNumber = Some(phoneNumber),
      emailAddress = Some(email),
      address = toSubscriptionAddress(address, Some(countryName)),
      countryCode = address.countryCode
    )
  }

  def toSubscriptionRequest(
    legacyRegime: LegacyRegime,
    countryName: String
  ): SubscriptionRequest = {
    (legacyRegime, address.countryCode != "GB") match {
      case (PAYE, true) => null
      case (PAYE, false) => toPayeSubscriptionRequest
      case (CT, _) => toCtSubscriptionRequest(countryName)
      case (SA, _) => toSaSubscriptionRequest(countryName)
    }
  }

}

object SubscriptionCyaData {
  def subscriptionJourneyToCyaData(
    journey: SubscriptionJourney,
    legacyRegime: LegacyRegime
  ): Option[SubscriptionCyaData] = {
//    TODO: 11188 Reduce complexity if possible
    for {
      name <-
        if (legacyRegime == PAYE) {
          journey.payeContactName
        }
        else {
          journey.useCustomBusinessName match {
            case Some(true) => journey.businessNameAnswer
            case _ => journey.asaDetails.agencyName
          }
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
      name = name,
      phoneNumber = phoneNumber,
      email = email,
      address = address
    )
  }
}
