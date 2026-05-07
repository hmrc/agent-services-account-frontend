/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.forms.subscriptions

import org.apache.commons.lang3.RandomStringUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm._
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA

class ChangeSubscriptionAddressFormSpec
extends AnyWordSpec
with Matchers {

  private val regimes = Seq(PAYE, SA)

  private val validUkParams = Map(
    line1Key -> "25 Any Street",
    line2Key -> "Any District",
    line3Key -> "",
    line4Key -> "",
    postcodeKey -> "AA1 1AA"
  )

  private val validNonUkParams = Map(
    line1Key -> "25 Any Street",
    line2Key -> "Any District",
    line3Key -> "Any Region",
    countryCodeKey -> "FR"
  )

  private def tooLong(
    regime: LegacyRegime,
    row: Int
  ): String = RandomStringUtils.insecure().nextAlphanumeric(maxLen(regime, row) + 1)

  private def assertFieldErrors(
    validatedForm: Form[BusinessAddress],
    expectedErrors: Map[String, String]
  ): Unit = {
    validatedForm.hasErrors shouldBe true

    expectedErrors.foreach { case (field, message) =>
      withClue(s"Unexpected errors for $field: ${validatedForm.errors}") {
        validatedForm.error(field).get.message shouldBe message
      }
    }

    validatedForm.errors.map(_.key).distinct.sorted shouldBe expectedErrors.keys.toSeq.sorted
  }

  "ChangeSubscriptionAddressForm.ukForm" should {
    "bind and unbind a valid UK address" in {
      regimes.foreach { regime =>
        val form = ukForm(regime)

        form.bind(validUkParams).value shouldBe Some(BusinessAddress(
          addressLine1 = "25 Any Street",
          addressLine2 = Some("Any District"),
          addressLine3 = None,
          addressLine4 = None,
          postalCode = Some("AA1 1AA"),
          countryCode = "GB"
        ))

        form.mapping.unbind(BusinessAddress(
          addressLine1 = "25 Any Street",
          addressLine2 = Some("Any District"),
          addressLine3 = None,
          addressLine4 = None,
          postalCode = Some("AA1 1AA"),
          countryCode = "GB"
        )) shouldBe Map(
          line1Key -> "25 Any Street",
          line2Key -> "Any District",
          postcodeKey -> "AA1 1AA"
        )
      }
    }

    "return required errors for the mandatory UK fields for each regime" in {
      regimes.foreach { regime =>
        assertFieldErrors(
          ukForm(regime).bind(validUkParams ++ Map(
            line1Key -> "",
            line2Key -> "",
            postcodeKey -> ""
          )),
          Map(
            line1Key -> s"${regime.msgPrefix}.error.addressLine1.required",
            line2Key -> s"${regime.msgPrefix}.error.addressLine2.required",
            postcodeKey -> s"${regime.msgPrefix}.error.postcode.required"
          )
        )
      }
    }

    "return PAYE length errors for all address lines at the same time" in {
      assertFieldErrors(
        ukForm(PAYE).bind(validUkParams ++ Map(
          line1Key -> tooLong(PAYE, 1),
          line2Key -> tooLong(PAYE, 2),
          line3Key -> tooLong(PAYE, 3),
          line4Key -> tooLong(PAYE, 4)
        )),
        Map(
          line1Key -> s"${PAYE.msgPrefix}.error.addressLine1.length",
          line2Key -> s"${PAYE.msgPrefix}.error.addressLine2.length",
          line3Key -> s"${PAYE.msgPrefix}.error.addressLine3.length",
          line4Key -> s"${PAYE.msgPrefix}.error.addressLine4.length"
        )
      )
    }

    "return non-PAYE length errors for all address lines at the same time" in {
      assertFieldErrors(
        ukForm(SA).bind(validUkParams ++ Map(
          line1Key -> tooLong(SA, 1),
          line2Key -> tooLong(SA, 2),
          line3Key -> tooLong(SA, 3),
          line4Key -> tooLong(SA, 4)
        )),
        Map(
          line1Key -> s"${SA.msgPrefix}.error.addressLine1.length",
          line2Key -> s"${SA.msgPrefix}.error.addressLine2.length",
          line3Key -> s"${SA.msgPrefix}.error.addressLine3.length",
          line4Key -> s"${SA.msgPrefix}.error.addressLine4.length"
        )
      )
    }

    "return PAYE invalid-character errors for all address lines at the same time" in {
      assertFieldErrors(
        ukForm(PAYE).bind(validUkParams ++ Map(
          line1Key -> "Bad&Line",
          line2Key -> "Bad&Line",
          line3Key -> "Bad&Line",
          line4Key -> "Bad&Line"
        )),
        Map(
          line1Key -> s"${PAYE.msgPrefix}.error.addressLine1.invalid",
          line2Key -> s"${PAYE.msgPrefix}.error.addressLine2.invalid",
          line3Key -> s"${PAYE.msgPrefix}.error.addressLine3.invalid",
          line4Key -> s"${PAYE.msgPrefix}.error.addressLine4.invalid"
        )
      )
    }

    "return non-PAYE invalid-character errors for all address lines at the same time" in {
      assertFieldErrors(
        ukForm(SA).bind(validUkParams ++ Map(
          line1Key -> "Bad!Line",
          line2Key -> "Bad!Line",
          line3Key -> "Bad!Line",
          line4Key -> "Bad!Line"
        )),
        Map(
          line1Key -> s"${SA.msgPrefix}.error.addressLine1.invalid",
          line2Key -> s"${SA.msgPrefix}.error.addressLine2.invalid",
          line3Key -> s"${SA.msgPrefix}.error.addressLine3.invalid",
          line4Key -> s"${SA.msgPrefix}.error.addressLine4.invalid"
        )
      )
    }

    "return postcode format errors for each regime" in {
      regimes.foreach { regime =>
        assertFieldErrors(
          ukForm(regime).bind(validUkParams + (postcodeKey -> "INVALID")),
          Map(
            postcodeKey -> s"${regime.msgPrefix}.error.postcode.invalid"
          )
        )
      }
    }
  }

  "ChangeSubscriptionAddressForm.nonUkForm" should {
    "bind and unbind a valid non-UK address" in {
      regimes.foreach { regime =>
        val form = nonUkForm(regime)

        form.bind(validNonUkParams).value shouldBe Some(BusinessAddress(
          addressLine1 = "25 Any Street",
          addressLine2 = Some("Any District"),
          addressLine3 = Some("Any Region"),
          addressLine4 = None,
          postalCode = None,
          countryCode = "FR"
        ))

        form.mapping.unbind(BusinessAddress(
          addressLine1 = "25 Any Street",
          addressLine2 = Some("Any District"),
          addressLine3 = Some("Any Region"),
          addressLine4 = None,
          postalCode = None,
          countryCode = "FR"
        )) shouldBe validNonUkParams
      }
    }

    "return required errors for the mandatory non-UK fields for each regime" in {
      regimes.foreach { regime =>
        assertFieldErrors(
          nonUkForm(regime).bind(validNonUkParams ++ Map(
            line1Key -> "",
            line2Key -> "",
            line3Key -> "",
            countryCodeKey -> ""
          )),
          Map(
            line1Key -> s"${regime.msgPrefix}.error.addressLine1.required",
            line2Key -> s"${regime.msgPrefix}.error.addressLine2.required",
            line3Key -> s"${regime.msgPrefix}.error.addressLine3.required",
            countryCodeKey -> s"${regime.msgPrefix}.error.country.required"
          )
        )
      }
    }

    "return country code length errors for each regime" in {
      regimes.foreach { regime =>
        assertFieldErrors(
          nonUkForm(regime).bind(validNonUkParams + (countryCodeKey -> "FRA")),
          Map(
            countryCodeKey -> s"${regime.msgPrefix}.error.country.required"
          )
        )
      }
    }
  }

}
