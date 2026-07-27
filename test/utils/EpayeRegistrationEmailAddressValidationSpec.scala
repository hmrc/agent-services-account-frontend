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

package utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentservicesaccount.utils.EpayeRegistrationEmailAddressValidation

class EpayeRegistrationEmailAddressValidationSpec
extends PlaySpec {

  private val emailValidation = EpayeRegistrationEmailAddressValidation()

  "emailValidation.isValid" should {
    "allow a simple email address" in {
      emailValidation.isValid("a@domain.com") mustBe true
    }

    "allow email addresses with a hyphen, period, numbers, plus, underscore, exclamation, number sign or question mark" in {
      emailValidation.isValid("a-b@domain.com") mustBe true
      emailValidation.isValid("a.b@domain.com") mustBe true
      emailValidation.isValid("1@domain.com") mustBe true
      emailValidation.isValid("a+b@domain.com") mustBe true
      emailValidation.isValid("a_b@domain.com") mustBe true
      emailValidation.isValid("a!b@domain.com") mustBe true
      emailValidation.isValid("a#b@domain.com") mustBe true
      emailValidation.isValid("a?b@domain.com") mustBe true
    }

    "not allow an email address with a comma, colon, semicolon, parenthesis, pound sign or backslash" in {
      emailValidation.isValid("a,b@b.com") mustBe false
      emailValidation.isValid("a:b@b.com") mustBe false
      emailValidation.isValid("a;b@b.com") mustBe false
      emailValidation.isValid("a(b@b.com") mustBe false
      emailValidation.isValid("a)b@b.com") mustBe false
      emailValidation.isValid("a£b@b.com") mustBe false
      emailValidation.isValid("a\\b@b.com") mustBe false
    }

    "not allow an email address without an @ symbol" in {
      emailValidation.isValid("a.com") mustBe false
    }

    "not allow an email address with more than one @ symbol" in {
      emailValidation.isValid("a@b@c.com") mustBe false
    }

    "not allow an email address without a domain" in {
      emailValidation.isValid("a@") mustBe false
    }

    "not allow an email address without a local-part" in {
      emailValidation.isValid("@b.com") mustBe false
    }
  }

}
