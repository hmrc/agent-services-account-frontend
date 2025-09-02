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

package it.controllers.amls

import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.amlsJourneyKey
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses.ValidAmlsDetailsUK
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses.ValidAmlsNonUK
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetailsResponse
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAMLSDetailsForArn
import stubs.AgentAssuranceStubs.givenAgentRecordFound

import java.time.LocalDate

class ConfirmSupervisoryBodyControllerISpec
extends ComponentBaseISpec {

  private val amlsDetailsResponse = AmlsDetailsResponse(
    status = ValidAmlsDetailsUK,
    details = Some(AmlsDetails(supervisoryBody = "ABC", membershipNumber = Some("1234567890")))
  )

  private def amlsJourney(isAmlsBodyStillTheSame: Option[Boolean]) = UpdateAmlsJourney(
    status = ValidAmlsDetailsUK,
    newAmlsBody = Some("ABCD"),
    newRegistrationNumber = Some("1234567890"),
    isAmlsBodyStillTheSame = isAmlsBodyStillTheSame,
    newExpirationDate = Some(LocalDate.now())
  )

  private val repo = inject[SessionCacheRepository]

  private val confirmSupervisoryBodyPath = s"$amlsStartPath/confirm-supervisory-body"

  s"GET $confirmSupervisoryBodyPath" should {
    "display the page with an empty form if first time" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse, arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(isAmlsBodyStillTheSame = None)))

      val result = get(confirmSupervisoryBodyPath)

      result.status shouldBe OK
    }

    "display the page with a filled out form if user is revisiting" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse, arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(isAmlsBodyStillTheSame = Some(true))))

      val result = get(confirmSupervisoryBodyPath)

      result.status shouldBe OK

      result.body should include("true")

    }

    s"POST $confirmSupervisoryBodyPath" should {

      "return 303 SEE_OTHER and redirect to /confirm-registration-number when YES is selected" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)
        givenAMLSDetailsForArn(amlsDetailsResponse, arn.value)

        await(repo.putSession(amlsJourneyKey, amlsJourney(isAmlsBodyStillTheSame = None)))

        val result = post(confirmSupervisoryBodyPath)(body = Map("accept" -> List("true")))

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-registration-number"

        val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

        updatedSession.isAmlsBodyStillTheSame shouldBe Some(true)
        updatedSession.newAmlsBody shouldBe Some("ABC")

      }

      "return 303 SEE_OTHER and redirect to /new-registration-number when YES is selected for overseas agent" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)
        givenAMLSDetailsForArn(amlsDetailsResponse.copy(status = ValidAmlsNonUK), arn.value)

        await(repo.putSession(amlsJourneyKey, amlsJourney(isAmlsBodyStillTheSame = None).copy(status = ValidAmlsNonUK)))

        val result = post(confirmSupervisoryBodyPath)(body = Map("accept" -> List("true")))

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"

        val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

        updatedSession.isAmlsBodyStillTheSame shouldBe Some(true)
        updatedSession.newAmlsBody shouldBe Some("ABC")
      }

      "return 303 SEE_OTHER and redirect to /new-supervisory-body when NO is selected" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)
        givenAMLSDetailsForArn(amlsDetailsResponse.copy(status = ValidAmlsDetailsUK), arn.value)

        await(repo.putSession(amlsJourneyKey, amlsJourney(isAmlsBodyStillTheSame = None)))

        val result = post(confirmSupervisoryBodyPath)(body = Map("accept" -> List("false")))

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-supervisory-body"

        val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

        updatedSession.isAmlsBodyStillTheSame shouldBe Some(false)
        updatedSession.newAmlsBody shouldBe Some("ABCD")
      }

      "return BadRequest when invalid form submission" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)
        givenAMLSDetailsForArn(amlsDetailsResponse.copy(status = ValidAmlsDetailsUK), arn.value)

        await(repo.putSession(amlsJourneyKey, amlsJourney(isAmlsBodyStillTheSame = None)))

        val result = post(confirmSupervisoryBodyPath)(body = Map("invalid" -> List("???")))

        result.status shouldBe BAD_REQUEST
      }
    }
  }

}
