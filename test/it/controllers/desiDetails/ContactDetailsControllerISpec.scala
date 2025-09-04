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

package it.controllers.desiDetails

import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.ConfirmedResponseAddress
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.ConfirmedResponseAddressDetails
import uk.gov.hmrc.agentservicesaccount.models.addresslookup.Country
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AddressLookupStubs._
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentServicesAccountStubs.stubASAGetResponseError

class ContactDetailsControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val contactDetailsStartAddressLookupPath = s"$desiDetailsStartPath/new-address"
  private val contactDetailsFinishAddressLookupPath = s"$desiDetailsStartPath/address-lookup-finish"
  private val contactDetailsConfirmationPath = s"$desiDetailsStartPath/confirmation"
  private val contactDetailsShowBeforeYouStartPath = s"$desiDetailsStartPath/start-update"

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

  s"GET $contactDetailsStartAddressLookupPath" should {
    "redirect to the external service to look up an address" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenInitSuccess()
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(contactDetailsStartAddressLookupPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/alf-start"
    }
  }

  s"GET $contactDetailsFinishAddressLookupPath" should {
    "store the new address in session and redirect to review new details page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenGetAddressSuccess("bar", confirmedAddressResponse)
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = get(s"$contactDetailsFinishAddressLookupPath?id=bar")

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
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(s"$contactDetailsFinishAddressLookupPath")

      result.status shouldBe BAD_REQUEST
    }
  }

  s"GET $contactDetailsConfirmationPath" should {
    "display the confirmation page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(contactDetailsConfirmationPath)

      result.status shouldBe OK
      assertPageHasTitle("You have submitted new contact details")(result)
    }
  }

  s"GET $contactDetailsShowBeforeYouStartPath" should {
    "display the page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(contactDetailsShowBeforeYouStartPath)

      result.status shouldBe OK
      assertPageHasTitle("Contact details")(result)
    }
  }

}
