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
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentAssuranceStubs.givenPostDesignatoryDetails
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import stubs.AgentServicesAccountStubs.stubASAPostResponse

class CheckYourAnswersControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

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

  private val submittedByDetails = YourDetails(
    fullName = "John Tester",
    telephone = "01903 209919"
  )

  private val checkYourAnswersPath = s"$desiDetailsStartPath/check-your-answers"

  s"GET $checkYourAnswersPath" should {
    "display the review details page if there are designatory details in the session" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, submittedByDetails))
      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      val result = get(checkYourAnswersPath)

      result.status shouldBe OK
      assertPageHasTitle("Check your answers")(result)
    }

    "redirect to /manage-account/contact-details/view if there are no new details in session" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftSubmittedByKey, submittedByDetails))
      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      val result = get(checkYourAnswersPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.ViewContactDetailsController.showPage.url}"
    }
  }

  s"POST $checkYourAnswersPath" should {
    "store the pending change of detail in repo and show the 'what happens next' page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenPostDesignatoryDetails(arn.value)
      stubASAPostResponse(204)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, submittedByDetails))
      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      val result = post(checkYourAnswersPath)(Map("" -> List("")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.ContactDetailsController.showChangeSubmitted.url}"
    }

    "redirect to /view-details if no details are in session" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(checkYourAnswersPath)(Map("" -> List("")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.ViewContactDetailsController.showPage.url}"
    }

    "return Internal Server Error if agent details not found" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, submittedByDetails))
      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      givenAgentRecordFound(emptyAgencyDetailsDesResponse)

      val result = post(checkYourAnswersPath)(Map("" -> List("")))

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return Internal Server Error if current selected changes empty" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, submittedByDetails))
      await(repo.putSession(currentSelectedChangesKey, Set.empty[String]))

      givenAgentRecordFound(emptyAgencyDetailsDesResponse)

      val result = post(checkYourAnswersPath)(Map("" -> List("")))

      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
