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

  private val subscriptionRequestPhoneNumber = phoneNumber.replaceAll("[^0-9]", "")

  private def toSubscriptionAddress(
    address: BusinessAddress,
    countryName: Option[String] = None
  ): SubscriptionAddress = {
    val line4: Option[String] =
      (address.countryCode == "GB", countryName.isDefined) match {
        case (false, true) => countryName
        case _ => address.addressLine4
      }
    val asaDetailsAgencyAddress = SubscriptionAddress(
      line1 = address.addressLine1,
      line2 = address.addressLine2.getOrElse(""),
      line3 = address.addressLine3,
      line4 = line4,
      postCode = address.postalCode
    )
    asaDetailsAgencyAddress
  }

  private def toCtSubscriptionRequest(
    countryName: String,
    isWelsh: Boolean
  ): CtSubscriptionRequest = {
    CtSubscriptionRequest(
      agentName = name,
      contactName = name,
      phoneNumber = Some(subscriptionRequestPhoneNumber),
      emailAddress = Some(email),
      address = toSubscriptionAddress(address, Some(countryName)),
      countryCode = address.countryCode,
      isWelsh = isWelsh
    )
  }

  private def toPayeSubscriptionRequest(
    asaAgentName: String,
    isWelsh: Boolean
  ): PayeSubscriptionRequest = {
    PayeSubscriptionRequest(
      agentName = asaAgentName,
      contactName = name,
      phoneNumber = Some(subscriptionRequestPhoneNumber),
      emailAddress = Some(email),
      address = toSubscriptionAddress(address),
      isWelsh = isWelsh
    )
  }

  private def toSaSubscriptionRequest(
    countryName: String,
    isWelsh: Boolean
  ): SaSubscriptionRequest = {
    SaSubscriptionRequest(
      agentName = name,
      contactName = name,
      phoneNumber = Some(subscriptionRequestPhoneNumber),
      emailAddress = Some(email),
      address = toSubscriptionAddress(address, Some(countryName)),
      countryCode = address.countryCode,
      isWelsh = isWelsh
    )
  }

  def toSubscriptionRequest(
    legacyRegime: LegacyRegime,
    countryName: String,
    isWelsh: Boolean,
    asaAgentNameOpt: Option[String] = None
  ): Option[SubscriptionRequest] = {
    (legacyRegime, address.countryCode != "GB", asaAgentNameOpt) match {
      case (PAYE, false, Some(asaAgentName)) => Some(toPayeSubscriptionRequest(asaAgentName, isWelsh))
      case (PAYE, _, _) => None
      case (CT, _, _) => Some(toCtSubscriptionRequest(countryName, isWelsh))
      case (SA, _, _) => Some(toSaSubscriptionRequest(countryName, isWelsh))
    }
  }

}

object SubscriptionCyaData {
  def subscriptionJourneyToCyaData(
    journey: SubscriptionJourney,
    legacyRegime: LegacyRegime
  ): Option[SubscriptionCyaData] = {
    def getCustomAnswerOrAsaDetailsDefault[A](
      useCustom: Option[Boolean],
      customAnswer: Option[A],
      asaDetailsDefault: Option[A]
    ): Option[A] = {
      useCustom match {
        case Some(true) => customAnswer
        case Some(false) => asaDetailsDefault
        case None => None
      }
    }
    for {
      name <-
        if (legacyRegime == PAYE) {
          journey.payeContactName
        }
        else {
          getCustomAnswerOrAsaDetailsDefault(
            journey.useCustomBusinessName,
            journey.businessNameAnswer,
            journey.asaDetails.agencyName
          )
        }
      phoneNumber <- getCustomAnswerOrAsaDetailsDefault(
        journey.useCustomPhoneNumber,
        journey.phoneNumberAnswer,
        journey.asaDetails.agencyTelephone
      )
      email <- getCustomAnswerOrAsaDetailsDefault(
        journey.useCustomEmail,
        journey.emailAnswer,
        journey.asaDetails.agencyEmail
      )
      address <- getCustomAnswerOrAsaDetailsDefault(
        journey.useCustomAddress,
        journey.addressAnswer,
        journey.asaDetails.agencyAddress
      )
    } yield SubscriptionCyaData(
      name = name,
      phoneNumber = phoneNumber,
      email = email,
      address = address
    )
  }
}
