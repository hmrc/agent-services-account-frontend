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
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentServicesAccountStubs.stubASAGetResponseError

class UpdateNameControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val updateNamePath = s"$desiDetailsStartPath/new-name"

  private val designatoryDetails = DesignatoryDetails(
    agencyDetails = agentRecord.agencyDetails.get.copy(
      agencyEmail = Some("new@email.com")
    ),
    otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
  )

  s"GET $updateNamePath" should {
    "display the enter business name page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      val result = get(updateNamePath)

      result.status shouldBe OK
    }

    "redirect to manage-account/contact-changes/select-changes when contact details page request is invalid" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("email")))

      val result = get(updateNamePath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.SelectDetailsController.showPage.url}"
    }
  }

  s"POST $updateNamePath" should {
    "redirect to apply SA code page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = post(updateNamePath)(body = Map("name" -> List("new name")))

      result.header("Location").get shouldBe s"${desiDetails.routes.ApplySACodeChangesController.showPage.url}"

      await(repo.getFromSession(draftNewContactDetailsKey)).get.agencyDetails.agencyName shouldBe Some("new name")

    }

    "return Bad Request if the data submitted is invalid" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(updateNamePath)(body = Map("invalid" -> List("???")))

      result.status shouldBe BAD_REQUEST
    }
  }

}
