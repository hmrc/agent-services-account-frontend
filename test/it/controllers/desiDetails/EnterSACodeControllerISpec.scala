/*
 * Copyright 2025 HM Revenue & Customs
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

class EnterSACodeControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val designatoryDetails = DesignatoryDetails(
    agencyDetails = agentRecord.agencyDetails.get.copy(
      agencyEmail = Some("new@email.com")
    ),
    otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
  )

  private val yourDetails = YourDetails(fullName = "John Tester", telephone = "078187777831")

  private val enterSaCodePath = s"$desiDetailsStartPath/enter-SA-code"

  s"GET $enterSaCodePath" should {
    "display the page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set.empty[String]))

      val result = get(enterSaCodePath)

      result.status shouldBe OK
      assertPageHasTitle("What’s the agent code you use for Self Assessment?")(result)
    }

    "redirect to /view-details if other services request is invalid" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      val result = get(enterSaCodePath)

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe s"${desiDetails.routes.ViewContactDetailsController.showPage.url}"
    }
  }

  s"POST $enterSaCodePath" should {

    "redirect to /apply-code-CT and store data" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = post(enterSaCodePath)(body = Map("saCode" -> List("AB1234")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.ApplyCTCodeChangesController.showPage.url}"
    }

    "redirect to /check-your-answers if journey complete" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, yourDetails))

      val result = post(enterSaCodePath)(body = Map("saCode" -> List("AB1234")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.CheckYourAnswersController.showPage.url}"
    }

    "return BadRequest when invalid form submission" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = post(enterSaCodePath)(body = Map("saCode" -> List("?!@£")))

      result.status shouldBe BAD_REQUEST
      assertPageHasTitle("Error: What’s the agent code you use for Self Assessment?")(result)
    }
  }

}
