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
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentServicesAccountStubs._

class ApplyCTCodeChangesControllerISpec
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

  private val applyCtCodeChangesPath = s"$desiDetailsStartPath/apply-code-CT"

  s"GET $applyCtCodeChangesPath" should {
    "display the page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set.empty[String]))

      val result = get(applyCtCodeChangesPath)

      result.status shouldBe OK
    }

    "redirect to /manage-account/contact-details/view when there are no changes selected" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(applyCtCodeChangesPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/contact-details/view"
    }
  }

  s"POST $applyCtCodeChangesPath" should {

    "redirect to /enter-CT-code when answer YES " in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = post(applyCtCodeChangesPath)(body = Map("applyChanges" -> List("true")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"$desiDetailsStartPath/enter-CT-code"

      await(repo.getFromSession(draftNewContactDetailsKey))
        .get.otherServices.ctChanges shouldBe CtChanges(applyChanges = true, None)
    }

    "redirect to /your-details when answer NO" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = post(applyCtCodeChangesPath)(body = Map("applyChanges" -> List("false")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"$desiDetailsStartPath/your-details"

      await(repo.getFromSession(draftNewContactDetailsKey))
        .get.otherServices.ctChanges shouldBe CtChanges(applyChanges = false, None)
    }

    "redirect to /check-your-answers when journey complete" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      await(repo.putSession(draftSubmittedByKey, YourDetails("name", "tel")))
      val result = post(applyCtCodeChangesPath)(body = Map("applyChanges" -> List("false")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"$desiDetailsStartPath/check-your-answers"

      await(repo.getFromSession(draftNewContactDetailsKey))
        .get.otherServices.ctChanges shouldBe CtChanges(applyChanges = false, None)
    }

    "return BadRequest when invalid form submission" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))

      val result = post(applyCtCodeChangesPath)(body = Map("invalid" -> List("???")))

      result.status shouldBe BAD_REQUEST
    }
  }

}
