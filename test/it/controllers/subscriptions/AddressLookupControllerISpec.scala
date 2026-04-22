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
import stubs.AddressLookupStubs._
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.ConfirmedResponseAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.ConfirmedResponseAddressDetails
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.Country
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class AddressLookupControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val legacyRegimes = List(CT, PAYE, SA)

  private val confirmedAddressResponse = ConfirmedResponseAddress(
    auditRef = "foo",
    id = Some("bar"),
    address = ConfirmedResponseAddressDetails(
      lines = Some(Seq(
        "Line 1",
        "Line 2",
        "Line 3"
      )),
      postcode = Some("AA1 1AA"),
      country = Some(Country("GB", ""))
    )
  )

  private val address = BusinessAddress(
    addressLine1 = "Line 1",
    addressLine2 = Some("Line 2"),
    addressLine3 = Some("Line 3"),
    addressLine4 = None,
    postalCode = Some("AA1 1AA"),
    countryCode = "GB"
  )

  legacyRegimes.foreach(legacyRegime => {
    val startAddressLookupPath = s"$subscriptionStartPath/$legacyRegime/address-lookup-start"
    val finishAddressLookupPath = s"$subscriptionStartPath/$legacyRegime/address-lookup-finish"

    s"GET $startAddressLookupPath" should {
      "redirect to the external service to look up an address" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)
        givenInitSuccess()
        stubASAGetResponseError(arn, NOT_FOUND)

        val result = get(startAddressLookupPath)

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/alf-start"
      }
    }

    s"GET $finishAddressLookupPath" should {

      val journeyWithRedirectLocations = List(
        (subscriptionBaseJourney, "check-your-answers", "not complete"),
        (subscriptionFullJourney, "check-your-answers", "complete")
      )

      journeyWithRedirectLocations.foreach(journeyWithRedirectLocation => {
        s"update journey with new address and redirect to ${journeyWithRedirectLocation._2}" +
          s"when journey ${journeyWithRedirectLocation._3}" in {

            givenAuthorisedAsAgentWith(arn.value)
            givenGetAgentRecord(agentRecord)
            stubASAGetResponseError(arn, NOT_FOUND)
            givenGetAddressSuccess("bar", confirmedAddressResponse)

            await(repo.putSession(subscriptionJourneyKey(legacyRegime), journeyWithRedirectLocation._1))

            val result = get(s"$finishAddressLookupPath?id=bar")
            result.status shouldBe SEE_OTHER
            result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/${journeyWithRedirectLocation._2}")

            val updatedJourney = await(repo.getFromSession(subscriptionJourneyKey(legacyRegime)))
            updatedJourney shouldBe defined
            updatedJourney.get.useCustomAddress shouldBe Some(true)
            updatedJourney.get.addressAnswer shouldBe Some(address)
          }
      })

      "return bad request when no id provided in a query param" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        val result = get(s"$finishAddressLookupPath")

        result.status shouldBe BAD_REQUEST
      }
    }
  })

}
