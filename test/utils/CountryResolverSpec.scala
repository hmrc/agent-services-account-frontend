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

package utils

import org.scalatest.matchers.must.Matchers.*
import uk.gov.hmrc.agentservicesaccount.utils.CountryResolver
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec

class CountryResolverSpec
extends ViewBaseSpec {

  val countryResolver = new CountryResolver(appConfig)

  "CountryResolver.countryName" should {

    "return country name when code exists in map" in {
      countryResolver.countryName("PT") mustBe "Portugal"
      countryResolver.countryName("PW") mustBe "Palau"
    }

    "return input code when country code is not found" in {
      countryResolver.countryName("XX") mustBe "XX"
    }

    "return standard version of long country name for submission when checkLengthForSubmission is set to false" in {
      countryResolver.countryName("AG", checkLengthForSubmission = false) mustBe "Antigua and Barbuda"
      countryResolver.countryName("BA", checkLengthForSubmission = false) mustBe "Bosnia and Herzegovina"
      countryResolver.countryName("CF", checkLengthForSubmission = false) mustBe "Central African Republic"
      countryResolver.countryName("CD", checkLengthForSubmission = false) mustBe "Congo (Democratic Republic)"
      countryResolver.countryName("ST", checkLengthForSubmission = false) mustBe "Sao Tome and Principe"
      countryResolver.countryName("TT", checkLengthForSubmission = false) mustBe "Trinidad and Tobago"
      countryResolver.countryName("AE", checkLengthForSubmission = false) mustBe "United Arab Emirates"
    }

    "return shortened version of long country name for submission when checkLengthForSubmission is set to true" in {
      countryResolver.countryName("AG", checkLengthForSubmission = true) mustBe "Antigua & Barbuda"
      countryResolver.countryName("BA", checkLengthForSubmission = true) mustBe "Bosnia-Herzegov."
      countryResolver.countryName("CF", checkLengthForSubmission = true) mustBe "Central African R."
      countryResolver.countryName("CD", checkLengthForSubmission = true) mustBe "DR Congo"
      countryResolver.countryName("ST", checkLengthForSubmission = true) mustBe "Sao Tome & Princ."
      countryResolver.countryName("TT", checkLengthForSubmission = true) mustBe "Trinidad & Tobago"
      countryResolver.countryName("AE", checkLengthForSubmission = true) mustBe "UAE"
    }
  }

}
