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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionCyaData
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney

class SubscriptionCyaDataSpec
extends AnyWordSpec
with Matchers {

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

  private val exampleGBCyaData = SubscriptionCyaData(
    name = "Test Name",
    phoneNumber = "123456",
    email = "test@test.com",
    address = businessAddress("GB", Some("Line 4"))
  )

  private val exampleNonGBCyaData = exampleGBCyaData.copy(address = businessAddress("PT", Some("Line 4")))

  List(CT, SA).foreach(legacyRegime => {
    s"SubscriptionCyaData.toSubscriptionRequest - $legacyRegime" should {

      "use businessName as agentName" in {
        val result = exampleGBCyaData.toSubscriptionRequest(legacyRegime, "")
        result.get.agentName shouldBe exampleGBCyaData.name
      }

      "use businessName as contactName" in {
        val result = exampleGBCyaData.toSubscriptionRequest(legacyRegime, "")
        result.get.contactName shouldBe exampleGBCyaData.name
      }

      "use addressLine4 when country is GB" in {
        val result = exampleGBCyaData.toSubscriptionRequest(legacyRegime, "Portugal")
        result.get.address.line4 shouldBe Some("Line 4")
      }

      "use countryName when country is not GB" in {
        val result = exampleNonGBCyaData.toSubscriptionRequest(legacyRegime, "Portugal")
        result.get.address.line4 shouldBe Some("Portugal")
      }

      "fallback to existing addressLine4 if non-GB and countryName is empty string" in {
        val result = exampleNonGBCyaData.toSubscriptionRequest(legacyRegime, "")
        result.get.address.line4 shouldBe Some("")
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

        result.get.address.line2 shouldBe ""
      }
    }

    s"SubscriptionCyaData.subscriptionJourneyToCyaData - $legacyRegime" should {

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

        val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, legacyRegime)

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

        val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, legacyRegime)

        result shouldBe Some(
          SubscriptionCyaData(
            "ASA Name",
            "999999",
            "asa@test.com",
            address
          )
        )
      }

      "return None when required data is missing - custom Some(true)" in {
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

        val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, legacyRegime)

        result shouldBe None
      }

      "return None when required data is missing - custom None" in {
        val journey = SubscriptionJourney(
          asaDetails = AgencyDetails(
            None,
            None,
            None,
            None
          ),
          useCustomBusinessName = None,
          businessNameAnswer = None,
          useCustomPhoneNumber = None,
          phoneNumberAnswer = None,
          useCustomEmail = None,
          emailAnswer = None,
          useCustomAddress = None,
          addressAnswer = None
        )

        val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, PAYE)

        result shouldBe None
      }
    }
  })

  "SubscriptionCyaData.toSubscriptionRequest - PAYE" should {
    val asaAgencyName = "Agency Name"

    "use ASA agencyName as agentName" in {
      val result = exampleGBCyaData.toSubscriptionRequest(PAYE, "", Some(asaAgencyName))
      result.get.agentName shouldBe asaAgencyName
    }

    "use payeContactName as contactName" in {
      val result = exampleGBCyaData.toSubscriptionRequest(PAYE, "", Some(asaAgencyName))
      result.get.contactName shouldBe exampleGBCyaData.name
    }

    "return None when ASA agencyName is None" in {
      val result = exampleGBCyaData.toSubscriptionRequest(PAYE, "", None)
      result shouldBe None
    }

    "use addressLine4 when country is GB" in {
      val result = exampleGBCyaData.toSubscriptionRequest(PAYE, "Portugal", Some(asaAgencyName))
      result.get.address.line4 shouldBe Some("Line 4")
    }

    "return None when country is not GB" in {
      val result = exampleNonGBCyaData.toSubscriptionRequest(PAYE, "Portugal", Some(asaAgencyName))
      result shouldBe None
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

      val result = cya.toSubscriptionRequest(PAYE, "Portugal", Some(asaAgencyName))

      result.get.address.line2 shouldBe ""
    }
  }

  "SubscriptionCyaData.subscriptionJourneyToCyaData - PAYE" should {

    "use custom values when flags are true" in {
      val address = businessAddress("GB")

      val journey = SubscriptionJourney(
        asaDetails = AgencyDetails(
          agencyName = Some("ASA Name"),
          agencyEmail = Some("asa@test.com"),
          agencyTelephone = Some("999999"),
          agencyAddress = Some(address)
        ),
        payeContactName = Some("Your Name"),
        useCustomPhoneNumber = Some(true),
        phoneNumberAnswer = Some("123456"),
        useCustomEmail = Some(true),
        emailAnswer = Some("custom@test.com"),
        useCustomAddress = Some(true),
        addressAnswer = Some(address)
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, PAYE)

      result shouldBe Some(
        SubscriptionCyaData(
          "Your Name",
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
        payeContactName = Some("Your Name"),
        useCustomPhoneNumber = Some(false),
        phoneNumberAnswer = None,
        useCustomEmail = Some(false),
        emailAnswer = None,
        useCustomAddress = Some(false),
        addressAnswer = None
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, PAYE)

      result shouldBe Some(
        SubscriptionCyaData(
          "Your Name",
          "999999",
          "asa@test.com",
          address
        )
      )
    }

    "return None when required data is missing - custom Some(true)" in {
      val journey = SubscriptionJourney(
        asaDetails = AgencyDetails(
          None,
          None,
          None,
          None
        ),
        payeContactName = Some("Paye Contact"),
        useCustomPhoneNumber = Some(true),
        phoneNumberAnswer = None,
        useCustomEmail = Some(true),
        emailAnswer = None,
        useCustomAddress = Some(true),
        addressAnswer = None
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, PAYE)

      result shouldBe None
    }

    "return None when required data is missing - custom None" in {
      val journey = SubscriptionJourney(
        asaDetails = AgencyDetails(
          None,
          None,
          None,
          None
        ),
        payeContactName = Some("Paye Contact"),
        useCustomPhoneNumber = None,
        phoneNumberAnswer = None,
        useCustomEmail = None,
        emailAnswer = None,
        useCustomAddress = None,
        addressAnswer = None
      )

      val result = SubscriptionCyaData.subscriptionJourneyToCyaData(journey, PAYE)

      result shouldBe None
    }
  }

}
