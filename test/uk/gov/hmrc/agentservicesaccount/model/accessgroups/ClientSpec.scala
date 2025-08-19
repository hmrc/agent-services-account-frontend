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

package uk.gov.hmrc.agentservicesaccount.model.accessgroups

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.Client

class ClientSpec
extends AnyFlatSpec
with Matchers {

  val testClient: Client = Client(
    enrolmentKey = "HMRC-MTD-IT~MTDITID~XX12345",
    friendlyName = "Test Client"
  )
  val json: JsValue = Json.toJson(testClient)

  "Client" should "serialise to JSON" in {
    json.toString shouldBe """{"enrolmentKey":"HMRC-MTD-IT~MTDITID~XX12345","friendlyName":"Test Client"}"""
  }

  "Client" should "deserialise from JSON" in {
    json.as[Client] shouldBe testClient
  }

}
