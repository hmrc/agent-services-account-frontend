/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentServicesAccountStubs.stubASAGetResponseError

class YourDetailsControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val yourDetailsPath = s"$desiDetailsStartPath/your-details"

  private val designatoryDetails = DesignatoryDetails(
    agencyDetails = agentRecord.agencyDetails.get.copy(
      agencyEmail = Some("new@email.com")
    ),
    otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
  )

  private val yourDetails = YourDetails(fullName = "John Tester", telephone = "078187777831")

  s"GET $yourDetailsPath" should {
    "display the Your details page normally if there is no change pending" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set.empty[String]))
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = get(yourDetailsPath)

      result.status shouldBe OK
    }

    "render page with previously entered answers" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set.empty[String]))
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, yourDetails))

      val result = get(yourDetailsPath)

      result.status shouldBe OK
      result.body should include("John Tester")
    }

    "redirect to manage-account/contact-details/view if other services page request is invalid" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, yourDetails))

      val result = get(yourDetailsPath)

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe s"${desiDetails.routes.ViewContactDetailsController.showPage}"
    }
  }

  s"POST $yourDetailsPath" should {
    "redirect to /check-your-answers" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(yourDetailsPath)(body = Map("fullName" -> List("Bob"), "telephone" -> List("010101")))

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe s"${desiDetails.routes.CheckYourAnswersController.showPage.url}"
    }

    "return Bad Request if invalid data supplied" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(yourDetailsPath)(body = Map("fullName" -> List("?!@£"), "telephone" -> List("?!@£")))

      result.status shouldBe BAD_REQUEST
    }
  }

}
