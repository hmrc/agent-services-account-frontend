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

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA

case class SubscriptionJourney(
  asaDetails: AgencyDetails,
  doYouAlreadyManage: Option[Boolean] = None,
  youMayNotNeedToApply: Option[Boolean] = None,
  payeContactName: Option[String] = None,
  useCustomBusinessName: Option[Boolean] = None,
  businessNameAnswer: Option[String] = None,
  useCustomPhoneNumber: Option[Boolean] = None,
  phoneNumberAnswer: Option[String] = None,
  useCustomEmail: Option[Boolean] = None,
  emailAnswer: Option[String] = None,
  useCustomAddress: Option[Boolean] = None,
  addressAnswer: Option[BusinessAddress] = None,
  isSubmitted: Boolean = false
) {

  private def answerComplete(
    booleanField: Option[Boolean],
    newAnswerField: Option[_]
  ): Boolean = {
    (booleanField, newAnswerField) match {
      case (Some(false), _) => true
      case (Some(true), Some(_)) => true
      case _ => false
    }
  }

  def isComplete(legacyRegime: LegacyRegime): Boolean = {
    val nameComplete =
      legacyRegime match {
        case LegacyRegime.PAYE => payeContactName.isDefined
        case _ => answerComplete(useCustomBusinessName, businessNameAnswer)
      }
    val pnComplete = answerComplete(useCustomPhoneNumber, phoneNumberAnswer)
    val eaComplete = answerComplete(useCustomEmail, emailAnswer)
    val addressComplete = answerComplete(useCustomAddress, addressAnswer) && addressValidForRegime(legacyRegime)
    nameComplete && pnComplete && eaComplete && addressComplete
  }

  // This is needed because address validation rules differ between ALF, ASA and the 3 legacy regimes,
  // so any selection of address (custom or ALF) must be validated against the regime-specific rules
  def addressValidForRegime(legacyRegime: LegacyRegime): Boolean = {
    val optAddress =
      if (useCustomAddress.contains(true))
        addressAnswer
      else
        asaDetails.agencyAddress
    (legacyRegime, optAddress) match {
      case (PAYE, Some(address)) =>
        Seq(
          address.addressLine1.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 1),
          address.addressLine2.exists(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 2)),
          address.addressLine3.fold(true)(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 3)),
          address.addressLine4.fold(true)(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 4)),
          address.postalCode.isDefined
        ).forall(identity)
      case (CT | SA, Some(address)) if address.isUk =>
        Seq(
          address.addressLine1.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 1),
          address.addressLine2.exists(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 2)),
          address.addressLine3.fold(true)(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 3)),
          address.addressLine4.fold(true)(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 4)),
          address.postalCode.isDefined,
          address.countryCode == "GB"
        ).forall(identity)
      case (CT | SA, Some(address)) if !address.isUk =>
        Seq(
          address.addressLine1.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 1),
          address.addressLine2.exists(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 2)),
          address.addressLine3.exists(_.length <= ChangeSubscriptionAddressForm.maxLen(legacyRegime, 3))
        ).forall(identity)
      case _ => false
    }
  }

}

object SubscriptionJourney {
  implicit val format: OFormat[SubscriptionJourney] = Json.format[SubscriptionJourney]
}
