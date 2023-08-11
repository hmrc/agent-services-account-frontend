/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentservicesaccount.models._

class SubscriptionJourneyRecordSpec extends AnyFlatSpec with Matchers {

  "SubscriptionJourneyRecord" should "be serializable to JSON and back" in {
    val authProviderId = AuthProviderId("someId")
    val businessDetails = BusinessDetails(Utr("1234567890"))
    val subscriptionJourneyRecord = SubscriptionJourneyRecord(authProviderId, businessDetails)

    val json = Json.toJson(subscriptionJourneyRecord)
    val parsedRecord = json.as[SubscriptionJourneyRecord]

    parsedRecord shouldEqual subscriptionJourneyRecord
  }
}

class BusinessDetailsSpec extends AnyFlatSpec with Matchers {

  "BusinessDetails" should "be serializable to JSON and back" in {
    val utr = Utr("1234567890")
    val businessDetails = BusinessDetails(utr)

    val json = Json.toJson(businessDetails)
    val parsedDetails = json.as[BusinessDetails]

    parsedDetails shouldEqual businessDetails
  }
}
