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

package uk.gov.hmrc.agentservicesaccount.forms.subscriptions

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.trimmedText
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

object ChangeSubscriptionAddressForm {

  val line1Key = "addressLine1"
  val line2Key = "addressLine2"
  val line3Key = "addressLine3"
  val line4Key = "addressLine4"
  val postcodeKey = "postcode"
  val countryCodeKey = "countryCode"

  def maxLen(
    legacyRegime: LegacyRegime,
    row: Int
  ): Int =
    if (legacyRegime == LegacyRegime.PAYE)
      35
    else if (row == 4)
      18
    else
      28

  private def lineMapping(
    legacyRegime: LegacyRegime,
    row: Int
  ) = {
    trimmedText
      .verifying(s"${legacyRegime.msgPrefix}.error.addressLine$row.required", _.nonEmpty)
      .verifying(s"${legacyRegime.msgPrefix}.error.addressLine$row.length", _.length <= maxLen(legacyRegime, row))
      .verifying(
        s"${legacyRegime.msgPrefix}.error.addressLine$row.invalid",
        address =>
          if (legacyRegime == LegacyRegime.PAYE)
            address.matches("^[a-zA-Z0-9 .,()!@-]*$")
          else
            address.matches("^[a-zA-Z0-9 ()&‘/,.-]*$")
      )
  }

  private def postcodeMapping(legacyRegime: LegacyRegime) = {
    trimmedText
      .verifying(s"${legacyRegime.msgPrefix}.error.postcode.required", _.nonEmpty)
      .verifying(
        s"${legacyRegime.msgPrefix}.error.postcode.invalid",
        address =>
          address.toUpperCase.matches("^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$")
      )
  }

  private def countryCodeMapping(legacyRegime: LegacyRegime) = {
    trimmedText
      .verifying(s"${legacyRegime.msgPrefix}.error.country.required", _.nonEmpty)
      .verifying(s"${legacyRegime.msgPrefix}.error.country.required", _.length == 2)
  }

  // To use when ALF returns a UK address that will not pass robotics validation
  def ukForm(legacyRegime: LegacyRegime): Form[BusinessAddress] = Form(mapping(
    line1Key -> lineMapping(legacyRegime, 1),
    line2Key -> lineMapping(legacyRegime, 2),
    line3Key -> optional(lineMapping(legacyRegime, 3)),
    line4Key -> optional(lineMapping(legacyRegime, 4)),
    postcodeKey -> postcodeMapping(legacyRegime)
  )(
    (
      l1,
      l2,
      l3,
      l4,
      postcode
    ) =>
      BusinessAddress(
        l1,
        Some(l2),
        l3,
        l4,
        Some(postcode),
        countryCode = "GB"
      )
  )(address =>
    Some(
      address.addressLine1,
      address.addressLine2.getOrElse(""),
      address.addressLine3,
      address.addressLine4,
      address.postalCode.getOrElse("")
    )
  ))

  // To use when ALF returns a non-UK address that will not pass robotics validation, or when the user chooses to edit a non-UK address from CYA
  def nonUkForm(legacyRegime: LegacyRegime): Form[BusinessAddress] = Form(mapping(
    line1Key -> lineMapping(legacyRegime, 1),
    line2Key -> lineMapping(legacyRegime, 2),
    line3Key -> lineMapping(legacyRegime, 3),
    countryCodeKey -> countryCodeMapping(legacyRegime)
  )(
    (
      l1,
      l2,
      l3,
      countryCode
    ) =>
      BusinessAddress(
        l1,
        Some(l2),
        Some(l3),
        None,
        None,
        countryCode
      )
  )(address =>
    Some(
      address.addressLine1,
      address.addressLine2.getOrElse(""),
      address.addressLine3.getOrElse(""),
      address.countryCode
    )
  ))

}
