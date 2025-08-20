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

import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.AgentUser

class AgentUserSpec
extends AnyFlatSpec
with Matchers {

  val testAgentUser: AgentUser = AgentUser(
    id = "foo",
    name = "Test Agent User"
  )
  val testAgentUserJson: JsValue = Json.parse("""{"id":"foo","name":"Test Agent User"}""")

  Json.toJson(testAgentUser) shouldBe testAgentUserJson withClue "serialise to json"
  testAgentUserJson.as[AgentUser] shouldBe testAgentUser withClue "deserialise from json"

}
