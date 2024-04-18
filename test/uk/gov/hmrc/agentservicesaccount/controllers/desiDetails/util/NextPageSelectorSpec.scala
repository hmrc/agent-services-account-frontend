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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util

import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.stubs.SessionServiceMocks
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec

import scala.concurrent.ExecutionContextExecutor

class NextPageSelectorSpec extends BaseISpec with SessionServiceMocks {
  implicit val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val mockJourney1: DesiDetailsJourney = DesiDetailsJourney(Some(Set("businessName")), journeyComplete = false)
  val mockJourney2: DesiDetailsJourney = DesiDetailsJourney(Some(Set("email", "telephone")), journeyComplete = false)
  val mockJourney3: DesiDetailsJourney = DesiDetailsJourney(Some(Set("businessName", "email", "telephone")), journeyComplete = false)
  val mockJourneyAll: DesiDetailsJourney = DesiDetailsJourney(Some(Set("businessName", "address", "email", "telephone")), journeyComplete = false)
  val mockJourneyComplete: DesiDetailsJourney = DesiDetailsJourney(None, journeyComplete = true)
  val mockJourneyContactComplete: DesiDetailsJourney = DesiDetailsJourney(None, journeyComplete = false)
  val oldAgencyDetails: AgencyDetails = AgencyDetails(
    agencyName = Some("Old Name"),
    agencyAddress = Some(BusinessAddress("Old Address", None, None, None, Some("ZZ9Z 9TT"), "GB")),
    agencyEmail = Some("old@test.com"),
    agencyTelephone = Some("01234 55678")
  )
  val completeNewAgencyDetails: AgencyDetails = AgencyDetails(
    agencyName = Some("New Name"),
    agencyAddress = Some(BusinessAddress("New Address", None, None, None, Some("ZZ9Z 9TT"), "GB")),
    agencyEmail = Some("new@test.com"),
    agencyTelephone = Some("07999 999999")
  )

  val mockDesiDetails: DesignatoryDetails = DesignatoryDetails(
    agencyDetails = oldAgencyDetails,
    otherServices = OtherServices(
      saChanges = SaChanges(applyChanges = false, None),
      ctChanges = CtChanges(applyChanges = false, None)
    )
  )

  "getNextPage with no previous selections" should {
    "redirect to first page in list" when {
      "all pages selected" in {
        val response: Result = getNextPage(mockJourneyAll, "selectChanges")
        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateNameController.showPage.url)
      }

      "some pages selected" in {
        val response: Result = getNextPage(mockJourney2, "selectChanges")
        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress.url)
      }
    }

    "redirect to next page in list" when {
      "given a page part way through the list" in {
        val response: Result = getNextPage(mockJourneyAll.copy(contactChangesNeeded = Some(Set("telephone"))), "email")
        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateTelephoneController.showPage.url)
      }
    }

    "redirect to ApplySACodeChanges page" when {
      "given the last page in list" in {
        val response: Result = getNextPage(mockJourneyContactComplete, "telephone")
        redirectLocation(response) shouldBe Some(desiDetails.routes.ApplySACodeChangesController.showPage.url)
      }
    }
  }

  "getNextPage with previous selections" should {
    "redirect to first non-previously selected page in list" when {
      "all pages selected" in {

        val response: Result = getNextPage(mockJourneyContactComplete, "address")

        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress.url)
      }
    }

    "redirect to next non-previously selected page in list" when {
      "given a page part way through the list" in {
        val response: Result = getNextPage(mockJourneyAll, "address")
        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateTelephoneController.showPage.url)
      }

      "given a page that was part of the previous journey" in {
        val response: Result = getNextPage(mockJourney2, "email")
        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateTelephoneController.showPage.url)
      }

      "given a page that shouldn't have been part of journey" in {
        val response: Result = getNextPage(mockJourney2, "address")
        redirectLocation(response) shouldBe Some(desiDetails.routes.UpdateTelephoneController.showPage.url)
      }
    }

    "redirect to Check Your Answers page" when {
      "given the last page in list" in {
        val response: Result = getNextPage(mockJourneyComplete, "telephone")
        redirectLocation(response) shouldBe Some(desiDetails.routes.CheckYourAnswersController.showPage.url)
      }
    }
  }

}
