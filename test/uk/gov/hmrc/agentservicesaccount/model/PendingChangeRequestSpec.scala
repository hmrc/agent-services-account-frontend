/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest.connectorReads
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest.connectorWrites
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec

import java.time.Instant
import scala.annotation.nowarn

class PendingChangeRequestSpec
extends UnitSpec {

  val arn = "XARN1234567"
  val date = "2024-01-01T01:02:03Z"

  val pendingChangeRequest: PendingChangeRequest = PendingChangeRequest(
    arn = Arn(arn),
    timeSubmitted = Instant.parse(date)
  )

  @nowarn("msg=possible missing interpolator")
  val pcrMongoJson: JsObject = Json.obj(
    "arn" -> arn,
    "timeSubmitted" -> Json.obj("$date" -> Json.obj("$numberLong" -> "1704070923000"))
  )

  val pcrConnectorJson: JsObject = Json.obj("arn" -> arn, "timeSubmitted" -> date)

  "A PendingChangeRequest" should {

    "read from JSON - mongo reads (default)" in {
      pcrMongoJson.as[PendingChangeRequest] shouldBe pendingChangeRequest
    }

    "read from JSON - connector reads" in {
      pcrConnectorJson.as[PendingChangeRequest](connectorReads) shouldBe pendingChangeRequest
    }

    "write to JSON - mongo writes (default)" in {
      Json.toJson(pendingChangeRequest) shouldBe pcrMongoJson
    }

    "write to JSON - connector writes" in {
      Json.toJson(pendingChangeRequest)(connectorWrites) shouldBe pcrConnectorJson
    }
  }

}
