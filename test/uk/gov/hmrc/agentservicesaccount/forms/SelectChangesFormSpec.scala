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

package uk.gov.hmrc.agentservicesaccount.forms

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SelectChanges

class SelectChangesFormSpec
extends AnyWordSpec
with Matchers {

  private val form: Form[SelectChanges] = SelectChangesForm.form

  "SelectChangesForm bound with values for field names" should {
    "return Some SelectChanges containing those fields" when {
      "field names are valid" in {
        val params = Map("businessName" -> "businessName", "telephone" -> "telephone")
        form.bind(params).hasErrors shouldBe false
        form.bind(params).value shouldBe Some(SelectChanges(
          Some("businessName"),
          None,
          None,
          Some("telephone")
        ))
      }
    }

    "return Some SelectChanges without that field" when {
      "field name is not valid" in {
        val params = Map("businessName" -> "businessName", "notAThing" -> "notAThing")
        form.bind(params).value shouldBe Some(SelectChanges(
          Some("businessName"),
          None,
          None,
          None
        ))
      }
    }

    "Be invalid with provided error message key" when {
      "no field names are present in params" in {
        val params: Map[String, String] = Map.empty
        form.bind(params).hasErrors shouldBe true
        form.bind(params).errors.head.message shouldBe "update-contact-details.select-changes.error"
      }
    }

    "unbind" in {
      form.mapping.unbind(SelectChanges.apply(
        Some("businessName"),
        None,
        Some("email"),
        None
      )) shouldBe
        Map("businessName" -> "businessName", "email" -> "email")
    }
  }

}
