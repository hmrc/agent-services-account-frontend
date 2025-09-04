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

package uk.gov.hmrc.agentservicesaccount.model

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatus
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses

class AmlsStatusSpec
extends UnitSpec {

  "AmlsStatus" should {
    "bind from a QueryString parameter" in {
      val status = AmlsStatus.queryBindable.bind("status", Map("status" -> Seq("NoAmlsDetailsUK")))

      status shouldBe Some(Right(AmlsStatuses.NoAmlsDetailsUK))
    }

    "serialise to a string in JSON" in {
      Json.toJson[AmlsStatus](AmlsStatuses.ExpiredAmlsDetailsUK) shouldBe JsString("ExpiredAmlsDetailsUK")
    }

    "parse a value from JSON" in {
      val json: JsObject = Json.parse(""" { "status": "PendingAmlsDetailsRejected" } """).as[JsObject]
      json.value("status").as[AmlsStatus] shouldBe AmlsStatuses.PendingAmlsDetailsRejected
    }
  }
}
