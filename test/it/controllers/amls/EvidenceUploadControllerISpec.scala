/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import uk.gov.hmrc.agentservicesaccount.repository.UpscanRepository
import uk.gov.hmrc.agentservicesaccount.models.upscan.FileUploadReference
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanInProgress
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanSuccess
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanFailure
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.UpscanStubs._
import stubs.ObjectStoreStubs._
import java.time.Instant

class EvidenceUploadControllerISpec
extends ComponentBaseISpec {

  val evidenceUploadPath = s"$amlsStartPath/upload-evidence"
  val evidenceUploadResultPath = s"$amlsStartPath/upload-result"
  val evidenceUploadErrorPath = s"$amlsStartPath/upload-error"
  val evidenceUploadStatusCheckPath = s"$amlsStartPath/upload-status-check"

  val repo = inject[SessionCacheRepository]
  val upscanRepo = inject[UpscanRepository]

  val amlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsDetailsUK,
    newAmlsBody = Some("ACCA")
  )

  val upscanReference = FileUploadReference("test-ref")
  val upscanInProgress = UpscanInProgress(upscanReference, Instant.now)
  val upscanSuccess = UpscanSuccess(
    reference = upscanReference,
    timestamp = Instant.now,
    downloadUrl = s"https://bucketName.s3.eu-west-2.amazonaws.com?$upscanReference",
    fileName = "evidence.pdf",
    mimeType = "application/pdf",
    checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    sizeInBytes = 12345L
  )
  val upscanFailure = UpscanFailure(
    reference = upscanReference,
    timestamp = Instant.now,
    failureReason = "QUARANTINE",
    messageFromUpscan = "File is quarantined"
  )

  s"GET $evidenceUploadPath" should {
    "render the evidence upload page if journey is valid" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      givenUpscanInitiateSucceeds(upscanReference.value)
      await(repo.putSession(amlsJourneyKey, amlsJourney))
      val result = get(evidenceUploadPath)
      result.status shouldBe OK
      assertPageHasTitle("Upload evidence")(result)
    }
    "redirect to new-supervisory-body if journey is missing newAmlsBody" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(repo.putSession(amlsJourneyKey, amlsJourney.copy(newAmlsBody = None)))
      val result = get(evidenceUploadPath)
      result.status shouldBe SEE_OTHER
      result.header("Location").get should include("new-supervisory-body")
    }
  }

  s"GET $evidenceUploadResultPath" should {
    "redirect to upload page if no key is provided" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(repo.putSession(amlsJourneyKey, amlsJourney))
      val result = get(evidenceUploadResultPath)
      result.status shouldBe SEE_OTHER
      result.header("Location").get should include(evidenceUploadPath)
    }
    "show progress page if upload is in progress" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(repo.putSession(amlsJourneyKey, amlsJourney))
      await(upscanRepo.saveUpscanDetails(upscanInProgress))
      val result = get(s"$evidenceUploadResultPath?key=${upscanReference.value}")
      result.status shouldBe OK
      assertPageHasTitle("We are checking your upload")(result)
    }
    "redirect to CYA if upload is successful" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      givenObjectStoreUploadFromUrlSucceeds()
      await(repo.putSession(amlsJourneyKey, amlsJourney))
      await(upscanRepo.saveUpscanDetails(upscanSuccess))
      val result = get(s"$evidenceUploadResultPath?key=${upscanReference.value}")
      result.status shouldBe SEE_OTHER
      result.header("Location").get should include("check-your-answers")
    }
    "redirect to upload page if reference not found" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(repo.putSession(amlsJourneyKey, amlsJourney))
      val result = get(s"$evidenceUploadResultPath?key=notfound")
      result.status shouldBe SEE_OTHER
      result.header("Location").get should include(evidenceUploadPath)
    }
  }

  s"GET $evidenceUploadStatusCheckPath" should {
    "return 202 Accepted if upload is successful" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(upscanRepo.saveUpscanDetails(upscanSuccess))
      val result = get(s"$evidenceUploadStatusCheckPath?key=${upscanReference.value}")
      result.status shouldBe ACCEPTED
    }
    "return 204 NoContent if upload is in progress" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(upscanRepo.saveUpscanDetails(upscanInProgress))
      val result = get(s"$evidenceUploadStatusCheckPath?key=${upscanReference.value}")
      result.status shouldBe NO_CONTENT
    }
    "return 409 Conflict if upload failed with QUARANTINE" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      await(upscanRepo.saveUpscanDetails(upscanFailure))
      val result = get(s"$evidenceUploadStatusCheckPath?key=${upscanReference.value}")
      result.status shouldBe CONFLICT
    }
    "return 400 BadRequest for generic failure" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      val genericFailure = upscanFailure.copy(failureReason = "SOME_OTHER_REASON")
      await(upscanRepo.saveUpscanDetails(genericFailure))
      val result = get(s"$evidenceUploadStatusCheckPath?key=${upscanReference.value}")
      result.status shouldBe BAD_REQUEST
    }
    "return 404 NotFound if reference does not exist" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      val result = get(s"$evidenceUploadPath/check-status/notfound")
      result.status shouldBe NOT_FOUND
    }
  }

}
