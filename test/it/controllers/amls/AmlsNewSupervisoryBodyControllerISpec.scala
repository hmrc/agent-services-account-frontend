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

package it.controllers.amls

import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.amlsJourneyKey
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound

import java.time.LocalDate
import scala.concurrent.Future

class AmlsNewSupervisoryBodyControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val amlsDetails = AmlsDetails("HMRC")
  private val amlsDetailsResponse = Future.successful(amlsDetails)

  private val newSupervisoryBodyPath = s"$amlsStartPath/new-supervisory-body"
  private val checkYourAnswersPath = s"$amlsStartPath/check-your-answers"
  private val newRegistrationNumberPath = s"$amlsStartPath/new-registration-number"
  private val confirmRegistrationNumberPath = s"$amlsStartPath/confirm-registration-number"

  private val ukAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsDetailsUK,
    isAmlsBodyStillTheSame = Some(true),
    newAmlsBody = Some("ACCA")
  )

  private val overseasAmlsJourney = UpdateAmlsJourney(
    status = AmlsStatuses.ValidAmlsNonUK,
    newAmlsBody = Some("OS AMLS"),
    newRegistrationNumber = Some("AMLS123"),
    newExpirationDate = Some(LocalDate.parse("2024-10-10"))
  )

  private val amlsBodies = Map(
    "ACCA" -> "Association of Certified Chartered Accountants",
    "HMRC" -> "HM Revenue and Customs (HMRC)"
  )

  s"GET $newSupervisoryBodyPath" should {
    "display the page for UK agent" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, ukAmlsJourney))

      val result = get(newSupervisoryBodyPath)

      result.status shouldBe OK

      result.body should include("What’s the name of your supervisory body?")

    }

    "display the page for overseas agent" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, overseasAmlsJourney))

      val result = get(newSupervisoryBodyPath)

      result.status shouldBe OK

      result.body should include("What’s the name of your supervisory body?")
    }
  }

  s"POST $newSupervisoryBodyPath" should {

    s"return 303 SEE_OTHER and redirect to $confirmRegistrationNumberPath" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, ukAmlsJourney))

      val result: WSResponse = post(newSupervisoryBodyPath)(Map("body" -> List("ACCA")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe confirmRegistrationNumberPath
    }

    s"return 303 SEE_OTHER for CYA and redirect to $checkYourAnswersPath" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, ukAmlsJourney))

      val result: WSResponse = postQ(newSupervisoryBodyPath)(Map("body" -> List("ACCA")))(Seq("cya" -> "true"))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe checkYourAnswersPath
    }

    s"return 303 SEE_OTHER for CYA and redirect to $newRegistrationNumberPath when body has changed to HMRC" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, ukAmlsJourney))

      val result: WSResponse = postQ(newSupervisoryBodyPath)(Map("body" -> List("HMRC")))(queryParam = Seq("cya" -> "true"))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"$newRegistrationNumberPath?cya=true"
    }

    s"return 303 SEE_OTHER to $newRegistrationNumberPath for overseas agent" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, overseasAmlsJourney))

      val result: WSResponse = post(newSupervisoryBodyPath)(Map("body" -> List("HMRC")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe newRegistrationNumberPath
    }

    "return BadRequest when invalid form submission" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, overseasAmlsJourney))

      val result: WSResponse = post(newSupervisoryBodyPath)(Map("something" -> List("invalid")))

      result.status shouldBe BAD_REQUEST

    }
  }

}
