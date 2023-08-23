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
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object SuspendDescriptionForm {

    private def suspendDescriptionConstraint: Constraint[String] = Constraint[String]{ input: String =>
      if (input.trim.isEmpty) Invalid(ValidationError("error.suspend.description.empty"))
      else if(input.trim.length > 250 ) Invalid(ValidationError("error.suspend.description.max-length"))
      else Valid
    }
    private val suspendDescriptionMapping: Mapping[String] = text.verifying(suspendDescriptionConstraint)

    val form = Form(
      single(
        "description" -> suspendDescriptionMapping
      )
    )
  }

