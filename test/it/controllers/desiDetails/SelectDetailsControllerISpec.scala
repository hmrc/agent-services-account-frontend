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
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentServicesAccountStubs.stubASAGetResponseError

class SelectDetailsControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val selectDetailsPath = "/agent-services-account/manage-account/contact-changes/select-changes"

  s"GET $selectDetailsPath" should {
    "display the select changes page if " in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("email")))

      val result = get(selectDetailsPath)

      result.status shouldBe OK
    }

    "fill with previously entered answers" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("businessName")))

      val result = get(selectDetailsPath)

      result.status shouldBe OK

      result.body should include("businessName")
    }
  }

  s"POST $selectDetailsPath" should {
    "store the selected changes in session and redirect to first selected page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(selectDetailsPath)(body = Map("email" -> List("email"), "businessName" -> List("businessName")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.UpdateNameController.showPage.url}"
      await(repo.getFromSession(currentSelectedChangesKey)) shouldBe Some(Set("email", "businessName"))
    }

    "return Bad Request if the data submitted is invalid" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(selectDetailsPath)(body = Map("businessName" -> List("?!@£")))

      result.status shouldBe BAD_REQUEST
    }

    "return Bad Request if the data submitted is empty" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = post(selectDetailsPath)(body = Map("" -> List("")))

      result.status shouldBe BAD_REQUEST
    }
  }

}
