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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
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
  }

}
