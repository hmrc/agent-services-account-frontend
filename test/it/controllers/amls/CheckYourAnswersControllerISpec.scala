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
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAMLSDetailsForArn
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentAssuranceStubs.givenPostAMLSDetails

import java.time.LocalDate

class CheckYourAnswersControllerISpec
extends ComponentBaseISpec {

  val checkYourAnswersPath = s"$amlsStartPath/check-your-answers"

  val repo = inject[SessionCacheRepository]

  private def amlsJourney(status: AmlsStatus) = UpdateAmlsJourney(
    status = status,
    newAmlsBody = Some("ABC"),
    newRegistrationNumber = Some("1234567890"),
    newExpirationDate = Some(LocalDate.now())
  )

  private val amlsRequest: AmlsRequest =
    new AmlsRequest(
      ukRecord = true,
      supervisoryBody = "ABC",
      membershipNumber = "1234567890",
      membershipExpiresOn = Some(LocalDate.now())
    )

  private val amlsDetailsResponse = AmlsDetailsResponse(
    status = ValidAmlsDetailsUK,
    details = Some(AmlsDetails(supervisoryBody = "ABC", membershipNumber = Some("1234567890")))
  )

  s"GET $checkYourAnswersPath" should {
    "return OK" when {
      "UK agent has successfully entered all the data for CYA page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK)))

        val result = get(checkYourAnswersPath)

        result.status shouldBe OK
      }

      "overseas agent has successfully entered all the data for CYA page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsNonUK)))

        val result = get(checkYourAnswersPath)

        result.status shouldBe OK
      }
    }

    "Redirect to manage account page" when {
      "There's no update amls journey in session" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        val result = get(checkYourAnswersPath)

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account"

      }
    }
  }

  s"POST $checkYourAnswersPath" should {
    "redirect to /manage-account/money-laundering-supervision/confirmation-new-supervision" when {
      "agent has successfully entered all the data for CYA page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)
        givenPostAMLSDetails(arn.value, amlsRequest)
        givenAMLSDetailsForArn(amlsDetailsResponse, arn.value)

        await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK)))

        val result = post(checkYourAnswersPath)(Map.empty)

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirmation-new-supervision"

      }
    }

    "return BadRequest" when {
      "invalid amls data held in session" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        await(repo.putSession(
          amlsJourneyKey,
          UpdateAmlsJourney(
            status = ValidAmlsNonUK,
            newAmlsBody = None,
            newRegistrationNumber = None,
            newExpirationDate = None
          )
        ))

        val result = post(checkYourAnswersPath)(Map.empty)

        result.status shouldBe BAD_REQUEST
      }
    }
  }

}
