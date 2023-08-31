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

package uk.gov.hmrc.agentservicesaccount.forms

  import org.apache.commons.lang3.RandomStringUtils
  import org.scalatest.matchers.should.Matchers
  import org.scalatest.wordspec.AnyWordSpec
  import org.scalatestplus.play.guice.GuiceOneAppPerSuite
  import uk.gov.hmrc.agentservicesaccount.forms.SuspendDescriptionForm

  class SuspendDescriptionFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

    val descriptionField = "description"

    "SuspendDescriptionForm binding" should {
      "be successful when form is not empty" in {
        val params = Map(
          descriptionField -> "This is the description",
        )

        SuspendDescriptionForm.form.bind(params).value shouldBe Some("This is the description")

      }

      s"error when $descriptionField is empty" in {
        val params = Map(
          descriptionField -> ""
        )
        val validatedForm = SuspendDescriptionForm.form.bind(params)
        validatedForm.hasErrors shouldBe true
        validatedForm.error(descriptionField).get.message shouldBe "error.suspended-description.empty"
        validatedForm.errors.length shouldBe 1
      }

      s"error when $descriptionField is too long" in {
        val params = Map(
          descriptionField -> RandomStringUtils.randomAlphanumeric(251),
        )
        val validatedForm = SuspendDescriptionForm.form.bind(params)
        validatedForm.hasErrors shouldBe true
        validatedForm.error(descriptionField).get.message shouldBe "error.suspended-description.max-length"
        validatedForm.errors.length shouldBe 1
      }
    }
  }
