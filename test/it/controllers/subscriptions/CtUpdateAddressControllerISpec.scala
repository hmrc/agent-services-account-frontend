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

package it.controllers.subscriptions

import play.api.test.Helpers._
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtJourney
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class CtUpdateAddressControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val updateAddressPath = s"$ctSubscriptionStartPath/address"

  private val baseJourney: CtJourney = CtJourney(
    asaDetails = AgencyDetails(
      agencyName = None,
      agencyEmail = Some("joe@bloggs.com"),
      agencyTelephone = None,
      agencyAddress = None
    ),
    useCustomBusinessName = None,
    businessNameAnswer = None,
    useCustomPhoneNumber = None,
    phoneNumberAnswer = None,
    useCustomEmail = None,
    emailAnswer = None,
    useCustomAddress = None,
    addressAnswer = None
  )

  s"GET $updateAddressPath" should {
    "display the enter address page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(updateAddressPath)

      result.status shouldBe OK
      assertPageHasTitle("What address should we use to send letters about Corporation Tax?")(result)
    }
  }

  s"POST $updateAddressPath" should {

    "update journey and redirect when using ASA address" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      repo.putSession(ctJourneyKey, baseJourney).futureValue

      val result =
        post(updateAddressPath)(body =
          Map(
            "addressUseAsaData" -> Seq("true")
          )
        )

      result.status shouldBe SEE_OTHER

      val updated = await(repo.getFromSession(ctJourneyKey))
      updated shouldBe defined
      updated.get.useCustomAddress shouldBe Some(false)
      updated.value.addressAnswer shouldBe None
    }

    "(if not using ASA address) redirect to the ALF external journey" in {

      givenFullAuthorisedAsAgentWith(
        arn = arn.value,
        providerId = "cred-id",
        email = "abc@abc.com"
      )
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result =
        post(updateAddressPath)(body =
          Map(
            "addressUseAsaData" -> Seq("false")
          )
        )

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${subscriptions.routes.CtAddressLookupController.startAddressLookup}"
    }
  }

}
