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

package it.controllers.subscriptions

import play.api.test.Helpers._
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm.line1Key
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm.line2Key
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm.line3Key
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm.postcodeKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionAddressForm.addressUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class UpdateAddressControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val legacyRegimes = List(CT, PAYE, SA)

  private val changeRouteCases = List(
    ("address-change", "Change your address"),
    ("address-fix", "Change the address lines that are too long")
  )

  private val changedAddress = BusinessAddress(
    addressLine1 = "12 Changed Street",
    addressLine2 = Some("Changed District"),
    addressLine3 = Some("Changed Town"),
    addressLine4 = None,
    postalCode = Some("AA1 1AA"),
    countryCode = "GB"
  )

  private val invalidAddress = BusinessAddress(
    addressLine1 = "123456789012345678901234567890", // 30 characters, which is too long for CT/SA
    addressLine2 = Some("123456789012345678901234567890"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = None, // Required for PAYE
    countryCode = "GB"
  )

  legacyRegimes.foreach(legacyRegime => {
    val updateAddressPath = s"$subscriptionStartPath/$legacyRegime/address"

    s"GET $updateAddressPath" should {
      "display the enter address page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        val result = get(updateAddressPath)

        result.status shouldBe OK
        val expectedTitle: String =
          (legacyRegime: LegacyRegime) match {
            case CT => "What address should we use to send letters about Corporation Tax?"
            case PAYE => "What address should we use to send letters about PAYE?"
            case SA => "What address should we use to send letters about Self Assessment?"
          }
        assertPageHasTitle(expectedTitle)(result)
      }
    }

    s"POST $updateAddressPath" should {

      val journeyWithRedirectLocations = List(
        (subscriptionBaseJourney, "check-your-answers"),
        (subscriptionFullJourney(legacyRegime), "check-your-answers"),
        (subscriptionFullJourney(legacyRegime).copy(asaDetails = subscriptionAgencyDetails.copy(agencyAddress = Some(invalidAddress))), "address-fix")
      )

      journeyWithRedirectLocations.foreach(journeyWithRedirectLocation => {
        s"update journey and redirect to ${journeyWithRedirectLocation._2} when using ASA address " +
          s"and journey ${completeString(journeyWithRedirectLocation._1, legacyRegime)}" in {
            givenAuthorisedAsAgentWith(arn.value)
            givenGetAgentRecord(agentRecord)
            stubASAGetResponseError(arn, NOT_FOUND)

            repo.putSession(subscriptionJourneyKey(legacyRegime), journeyWithRedirectLocation._1).futureValue

            val result =
              post(updateAddressPath)(body =
                Map(
                  addressUseAsaDataKey -> Seq("true")
                )
              )
            result.status shouldBe SEE_OTHER
            result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/${journeyWithRedirectLocation._2}")

            val updated = await(repo.getFromSession(subscriptionJourneyKey(legacyRegime)))
            updated shouldBe defined
            updated.get.useCustomAddress shouldBe Some(false)
            updated.value.addressAnswer shouldBe None
          }
      })

      "(if not using ASA address) redirect to the ALF external journey" in {

        givenFullAuthorisedAsAgentWith(
          arn = arn.value,
          providerId = "cred-id",
          email = "abc@abc.com"
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        val result =
          post(updateAddressPath)(body =
            Map(
              addressUseAsaDataKey -> Seq("false")
            )
          )

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe s"${subscriptions.routes.AddressLookupController.startAddressLookup(legacyRegime)}"
      }
    }

    changeRouteCases.foreach { case (routeSuffix, expectedTitle) =>
      val changeAddressPath = s"$subscriptionStartPath/$legacyRegime/$routeSuffix"

      s"GET $changeAddressPath" should {
        s"display the change address page with title '$expectedTitle'" in {

          givenAuthorisedAsAgentWith(arn.value)
          givenGetAgentRecord(agentRecord)
          stubASAGetResponseError(arn, NOT_FOUND)

          repo.putSession(subscriptionJourneyKey(legacyRegime), subscriptionBaseJourney.copy(useCustomAddress = Some(false))).futureValue

          val result = get(changeAddressPath)

          result.status shouldBe OK
          assertPageHasTitle(expectedTitle)(result)
        }

        "redirect to the address selection page when there is no address to change" in {

          givenAuthorisedAsAgentWith(arn.value)
          givenGetAgentRecord(agentRecord)
          stubASAGetResponseError(arn, NOT_FOUND)

          repo.putSession(subscriptionJourneyKey(legacyRegime), subscriptionBaseJourney).futureValue

          val result = get(changeAddressPath)

          result.status shouldBe SEE_OTHER
          result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/address")
        }
      }

      s"POST $changeAddressPath" should {
        "update the journey and redirect to the address selection page when the form is valid" in {

          givenAuthorisedAsAgentWith(arn.value)
          givenGetAgentRecord(agentRecord)
          stubASAGetResponseError(arn, NOT_FOUND)

          repo.putSession(subscriptionJourneyKey(legacyRegime), subscriptionBaseJourney.copy(useCustomAddress = Some(false))).futureValue

          val result =
            post(changeAddressPath)(body =
              Map(
                line1Key -> Seq(changedAddress.addressLine1),
                line2Key -> Seq(changedAddress.addressLine2.get),
                line3Key -> Seq(changedAddress.addressLine3.get),
                postcodeKey -> Seq(changedAddress.postalCode.get)
              )
            )

          result.status shouldBe SEE_OTHER
          result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/check-your-answers")

          val updated = await(repo.getFromSession(subscriptionJourneyKey(legacyRegime)))
          updated shouldBe defined
          updated.get.useCustomAddress shouldBe Some(true)
          updated.value.addressAnswer shouldBe Some(changedAddress)
        }

        s"return BAD_REQUEST with title 'Error: $expectedTitle' when the form is invalid" in {

          givenAuthorisedAsAgentWith(arn.value)
          givenGetAgentRecord(agentRecord)
          stubASAGetResponseError(arn, NOT_FOUND)

          repo.putSession(subscriptionJourneyKey(legacyRegime), subscriptionBaseJourney.copy(useCustomAddress = Some(false))).futureValue

          val result = post(changeAddressPath)(body = Map.empty)

          result.status shouldBe BAD_REQUEST
          assertPageHasTitle(s"Error: $expectedTitle")(result)
        }

        "redirect to the address selection page when there is no address to change" in {

          givenAuthorisedAsAgentWith(arn.value)
          givenGetAgentRecord(agentRecord)
          stubASAGetResponseError(arn, NOT_FOUND)

          repo.putSession(subscriptionJourneyKey(legacyRegime), subscriptionBaseJourney).futureValue

          val result =
            post(changeAddressPath)(body =
              Map(
                line1Key -> Seq(changedAddress.addressLine1),
                line2Key -> Seq(changedAddress.addressLine2.get),
                line3Key -> Seq(changedAddress.addressLine3.get),
                postcodeKey -> Seq(changedAddress.postalCode.get)
              )
            )

          result.status shouldBe SEE_OTHER
          result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/address")
        }
      }
    }
  })

}
