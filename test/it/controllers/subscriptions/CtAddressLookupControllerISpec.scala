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
import stubs.AgentServicesAccountStubs.{givenGetAgentRecord, stubASAGetResponseError}
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.{ConfirmedResponseAddress, ConfirmedResponseAddressDetails, Country}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

//TODO: 10906 Implements these ITs correctly
class CtAddressLookupControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val StartAddressLookupPath = s"$ctSubscriptionStartPath/new-address"
  private val FinishAddressLookupPath = s"$ctSubscriptionStartPath/address-lookup-finish"
  private val ConfirmationPath = s"$ctSubscriptionStartPath/confirmation"
  private val ShowBeforeYouStartPath = s"$ctSubscriptionStartPath/start-update"

  private val confirmedAddressResponse = ConfirmedResponseAddress(
    auditRef = "foo",
    id = Some("bar"),
    address = ConfirmedResponseAddressDetails(
      organisation = Some("My Agency"),
      lines = Some(Seq("26 New Street", "Telford")),
      postcode = Some("TF5 4AA"),
      country = Some(Country("GB", ""))
    )
  )

  private val designatoryDetails = DesignatoryDetails(
    agencyDetails = AgencyDetails(
      agencyName = None,
      agencyEmail = None,
      agencyTelephone = None,
      agencyAddress = None
    ),
    otherServices = OtherServices(
      saChanges = SaChanges(applyChanges = false, saAgentReference = None),
      ctChanges = CtChanges(applyChanges = false, ctAgentReference = None)
    )
  )

  s"GET $StartAddressLookupPath" should {
    "redirect to the external service to look up an address" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      givenInitSuccess()
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(StartAddressLookupPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/alf-start"
    }
  }

  s"GET $FinishAddressLookupPath" should {
    "store the new address in session and redirect to review new details page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenGetAddressSuccess("bar", confirmedAddressResponse)
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = get(s"$FinishAddressLookupPath?id=bar")

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/contact-details/apply-code-SA"
      await(repo.getFromSession(draftNewContactDetailsKey))
        .get.agencyDetails.agencyAddress shouldBe Some(
        BusinessAddress(
          "26 New Street",
          Some("Telford"),
          None,
          None,
          Some("TF5 4AA"),
          "GB"
        )
      )
    }

    "return bad request when no id provided in a query param" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(s"$FinishAddressLookupPath")

      result.status shouldBe BAD_REQUEST
    }
  }

  s"GET $ConfirmationPath" should {
    "display the confirmation page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(ConfirmationPath)

      result.status shouldBe OK
      assertPageHasTitle("You have submitted new contact details")(result)
    }
  }

  s"GET $ShowBeforeYouStartPath" should {
    "display the page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(ShowBeforeYouStartPath)

      result.status shouldBe OK
      assertPageHasTitle("Contact details")(result)
    }
  }

}
