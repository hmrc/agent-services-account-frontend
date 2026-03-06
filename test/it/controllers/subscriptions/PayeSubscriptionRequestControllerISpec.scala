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

package it.controllers.subscriptions

import org.jsoup.Jsoup
import play.api.test.Helpers
import play.api.test.Helpers._
import support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.routes
import uk.gov.hmrc.agentservicesaccount.models.paye._
import stubs.AgentAssuranceStubs._
import stubs.AgentServicesAccountStubs._
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.PayeSubscriptionRequestController

class PayeSubscriptionRequestControllerISpec
  extends BaseISpec {

  val cyaData: PayeCyaData =
    PayeCyaData(
      agentName = "Example Agent Ltd",
      contactName = "Jane Agent",
      telephoneNumber = Some("01632 960 001"),
      emailAddress = Some("jane.agent@example.com"),
      address = PayeAddress(
        line1 = "1 High Street",
        line2 = "Village",
        line3 = Some("County"),
        line4 = None,
        postCode = "AA1 1AA"
      )
    )

  val controller: PayeSubscriptionRequestController = inject[PayeSubscriptionRequestController]

  "showConfirm" should {
    "return OK and render the confirmation screen when eligible" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenSubscriptionInfoResponse()

      val response = controller.showConfirm()(fakeRequest())

      status(response) shouldBe OK
      val html = Jsoup.parse(contentAsString(await(response)))
      ////      TODO: 10593 Better assertions in here
      //      assertPageHasTitle("Check your details before requesting a PAYE subscription")(result)
      //
      //      result.body should include("Example Agent Ltd")
      //      result.body should include("Jane Agent")
      //      result.body should include("Return to your agent services account")
    }
  }

  "submit" should {
    "redirect to submitted screen when eligible" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenSubscriptionInfoResponse()
      givenPayeStartSubscriptionResponse(OK)

      val response = controller.submit()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.PayeSubscriptionRequestController.showSubmitted.url)
    }
  }

  "showSubmitted" should {
    "return OK and render submitted screen" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenSubscriptionInfoResponse()

      val response = controller.showSubmitted()(fakeRequest())

      status(response) shouldBe OK
      val html = Jsoup.parse(contentAsString(await(response)))
      //      //      TODO: 10593 Better assertions in here
      //      assertPageHasTitle("PAYE subscription request submitted")(result)
      //      result.body should include("Return to your agent services account")
    }
  }
}
