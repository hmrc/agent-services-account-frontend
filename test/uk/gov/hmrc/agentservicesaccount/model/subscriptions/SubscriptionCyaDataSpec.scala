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

package uk.gov.hmrc.agentservicesaccount.model.subscriptions

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionCyaData
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney

class SubscriptionCyaDataSpec
extends AnyWordSpec
with Matchers {

//  TODO: 11188 Implement for all 3 legacyRegimes
  private val legacyRegime = SA

  private def businessAddress(
    countryCode: String,
    line4: Option[String] = Some("Line 4")
  ) = BusinessAddress(
    addressLine1 = "Line 1",
    addressLine2 = Some("Line 2"),
    addressLine3 = Some("Line 3"),
    addressLine4 = line4,
    postalCode = Some("AA1 1AA"),
    countryCode = countryCode
  )

  "SubscriptionCyaData.toSubscriptionRequest" should {

    "use addressLine4 when country is GB" in {
      val cya = SubscriptionCyaData(
        name = "Test Name",
        phoneNumber = "123456",
        email = "test@test.com",
        address = businessAddress("GB", Some("Line 4"))
      )

      val result = cya.toSubscriptionRequest(legacyRegime, "Portugal")

      result.address.line4 shouldBe Some("Line 4")
    }

    "use countryName when country is not GB" in {
      val cya = SubscriptionCyaData(
        name = "Test Name",
        phoneNumber = "123456",
        email = "test@test.com",
        address = businessAddress("PT", Some("Line 4"))
      )

      val result = cya.toSubscriptionRequest(legacyRegime, "Portugal")

      result.address.line4 shouldBe Some("Portugal")
    }

    "fallback to existing addressLine4 if non-GB and countryName is empty string" in {
      val cya = SubscriptionCyaData(
        name = "Test Name",
        phoneNumber = "123456",
        email = "test@test.com",
        address = businessAddress("PT", Some("Line 4"))
      )

      val result = cya.toSubscriptionRequest(legacyRegime, "")

      result.address.line4 shouldBe Some("")
    }

    "set empty string when addressLine2 is None" in {
      val address = BusinessAddress(
        addressLine1 = "Line 1",
        addressLine2 = None,
        addressLine3 = Some("Line 3"),
        addressLine4 = Some("Line 4"),
        postalCode = Some("AA1 1AA"),
        countryCode = "GB"
      )

      val cya = SubscriptionCyaData(
        "Test Name",
        "123456",
        "test@test.com",
        address
      )

      val result = cya.toSubscriptionRequest(legacyRegime, "Portugal")

      result.address.line2 shouldBe ""
    }
  }

  "SubscriptionCyaData.subscriptionJourneyToCyaData" should {

    "use custom values when flags are true" in {
      val address = businessAddress("GB")

      val journey = SubscriptionJourney(
        asaDetails = AgencyDetails(
          agencyName = Some("ASA Name"),
          agencyEmail = Some("asa@test.com"),
          agencyTelephone = Some("999999"),
          agencyAddress = Some(address)
        ),
        useCustomBusinessName = Some(true),
        businessNameAnswer = Some("Custom Name"),
        useCustomPhoneNumber = Some(true),
        phoneNumberAnswer = Some("123456"),
        useCustomEmail = Some(true),
        emailAnswer = Some("custom@test.com"),
        useCustomAddress = Some(true),
        addressAnswer = Some(address)
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey)

      result shouldBe Some(
        SubscriptionCyaData(
          "Custom Name",
          "123456",
          "custom@test.com",
          address
        )
      )
    }

    "fallback to ASA details when flags are false" in {
      val address = businessAddress("GB")

      val journey = SubscriptionJourney(
        asaDetails = AgencyDetails(
          agencyName = Some("ASA Name"),
          agencyEmail = Some("asa@test.com"),
          agencyTelephone = Some("999999"),
          agencyAddress = Some(address)
        ),
        useCustomBusinessName = Some(false),
        businessNameAnswer = None,
        useCustomPhoneNumber = Some(false),
        phoneNumberAnswer = None,
        useCustomEmail = Some(false),
        emailAnswer = None,
        useCustomAddress = Some(false),
        addressAnswer = None
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey)

      result shouldBe Some(
        SubscriptionCyaData(
          "ASA Name",
          "999999",
          "asa@test.com",
          address
        )
      )
    }

    "return None when required data is missing" in {
      val journey = SubscriptionJourney(
        asaDetails = AgencyDetails(
          None,
          None,
          None,
          None
        ),
        useCustomBusinessName = Some(true),
        businessNameAnswer = None,
        useCustomPhoneNumber = Some(true),
        phoneNumberAnswer = None,
        useCustomEmail = Some(true),
        emailAnswer = None,
        useCustomAddress = Some(true),
        addressAnswer = None
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey)

      result shouldBe None
    }
  }

}
