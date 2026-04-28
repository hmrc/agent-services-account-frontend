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
import uk.gov.hmrc.agentservicesaccount.repository.UpscanRepository
import stubs.AgentAssuranceStubs.givenAMLSDetailsForArn
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentAssuranceStubs.givenPostAMLSDetails
import uk.gov.hmrc.agentservicesaccount.models.upscan.FileUploadReference
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanSuccess

import java.time.Instant
import java.time.LocalDate

class CheckYourAnswersControllerISpec
extends ComponentBaseISpec {

  val checkYourAnswersPath = s"$amlsStartPath/check-your-answers"

  val repo = inject[SessionCacheRepository]
  val upscanRepo = inject[UpscanRepository]

  private def amlsJourney(
    status: AmlsStatus,
    isHmrc: Boolean
  ) = UpdateAmlsJourney(
    status = status,
    newAmlsBody =
      if (isHmrc)
        Some("HM Revenue and Customs (HMRC)")
      else
        Some("ABC"),
    newRegistrationNumber = Some("1234567890"),
    newExpirationDate = None,
    newEvidenceObjectReference =
      if (isHmrc)
        None
      else
        Some("123")
  )

  private val amlsRequest: AmlsRequest = AmlsRequest(
    ukRecord = true,
    supervisoryBody = "ABC",
    membershipNumber = "1234567890",
    membershipExpiresOn = None,
    evidenceObjectReference = Some("123")
  )

  private val amlsDetailsResponse = AmlsDetailsResponse(
    status = ValidAmlsDetailsUK,
    details = Some(AmlsDetails(
      supervisoryBody = "ABC",
      membershipNumber = Some("1234567890"),
      Some("123")
    ))
  )

  private val upscanSuccess = UpscanSuccess(
    FileUploadReference("123"),
    Instant.now,
    "download/url",
    "file.dat",
    "text",
    "checksum",
    1234
  )

  s"GET $checkYourAnswersPath" should {
    "return OK" when {
      "UK agent has successfully entered all the data for CYA page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)

        await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK, isHmrc = false)))
        await(upscanRepo.saveUpscanDetails(upscanSuccess))

        val result = get(checkYourAnswersPath)

        result.status shouldBe OK
        assertPageHasTitle("Check your answers")(result)
      }

      "HMRC AMLS agent has successfully entered all the data for CYA page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)

        await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK, isHmrc = true)))

        val result = get(checkYourAnswersPath)

        result.status shouldBe OK
      }
    }

    "Redirect to manage account page" when {
      "There's no update amls journey in session" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)

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
        givenGetAgentRecord(agentRecord)
        givenPostAMLSDetails(arn.value, amlsRequest)
        givenAMLSDetailsForArn(amlsDetailsResponse, arn.value)

        await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK, isHmrc = false)))

        val result = post(checkYourAnswersPath)(Map.empty)

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirmation-new-supervision"

      }
    }

    "return BadRequest" when {
      "invalid amls data held in session" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)

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
