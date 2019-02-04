/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.views

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.test.UnitSpec

class ViewOpsSpec extends UnitSpec {
  "ArnOps" should {
    "provide prettify method for an ARN" should {
      import ViewOps.ArnOps

      "valid arn is prettified in expected format" in {
        Arn("TARN0000001").prettify shouldBe "TARN-000-0001"
      }

      "invalid arn throws an exception" in {
        an[Exception] shouldBe thrownBy(Arn("TARN-0000001").prettify)
      }
    }
  }
}
