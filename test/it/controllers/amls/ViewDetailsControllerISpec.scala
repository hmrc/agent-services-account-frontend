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

import play.api.http.Status.OK
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.models._
import stubs.AgentAssuranceStubs.givenAMLSDetailsForArn
import stubs.AgentAssuranceStubs.givenAgentRecordFound

class ViewDetailsControllerISpec
extends ComponentBaseISpec {

  private val amlsDetails = AmlsDetails(supervisoryBody = "HMRC")
  private val amlsDetailsResponse = AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, Some(amlsDetails))
  private val noAmlsDetailsResponse = AmlsDetailsResponse(AmlsStatuses.NoAmlsDetailsUK, None)

  private val viewDetailsPath = "/manage-account/money-laundering-supervision/view-details"

  s"GET $viewDetailsPath" should {
    "display AMLS details when existing details found" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse, arn.value)

      val result = get(viewDetailsPath)

      result.status shouldBe OK
      assertPageHasTitle("Anti-money laundering supervision details")(result)
    }

    "display the page for the first time when AMLS details do not exist" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(noAmlsDetailsResponse, arn.value)

      val result = get(viewDetailsPath)

      result.status shouldBe OK
      assertPageHasTitle("Anti-money laundering supervision details")(result)

    }
  }

}
