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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import uk.gov.hmrc.agentservicesaccount.models._

class AgentSizeSpec extends AnyWordSpec with Matchers {

  "AgentSize" should {
    "serialize and deserialize correctly" in {
      val smallJson = JsString("small")
      val mediumJson = JsString("medium")
      val largeJson = JsString("large")
      val xlargeJson = JsString("xlarge")

      AgentSize.agentSizeFormat.reads(smallJson) shouldBe JsSuccess(Small)
      AgentSize.agentSizeFormat.reads(mediumJson) shouldBe JsSuccess(Medium)
      AgentSize.agentSizeFormat.reads(largeJson) shouldBe JsSuccess(Large)
      AgentSize.agentSizeFormat.reads(xlargeJson) shouldBe JsSuccess(XLarge)

      AgentSize.agentSizeFormat.writes(Small) shouldBe smallJson
      AgentSize.agentSizeFormat.writes(Medium) shouldBe mediumJson
      AgentSize.agentSizeFormat.writes(Large) shouldBe largeJson
      AgentSize.agentSizeFormat.writes(XLarge) shouldBe xlargeJson
    }
  }
}



