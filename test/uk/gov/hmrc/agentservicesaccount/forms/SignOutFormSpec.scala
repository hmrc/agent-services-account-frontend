/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.forms

import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec

class SignOutFormSpec extends UnitSpec {

  "SignOutForm" should {
    "return no error message for valid selection" in {
      val data = Json.obj("surveyKey" -> "AGENTSUB")
      val form = SignOutForm.form.bind(data, 1024)
      form.hasErrors shouldBe false
    }

    "return an error message if empty selection" in {
      val data = Json.obj("surveyKey" -> "")
      val form = SignOutForm.form.bind(data, 1024)
      form.hasErrors shouldBe true
      form.errors.length shouldBe 1
    }
  }

}
