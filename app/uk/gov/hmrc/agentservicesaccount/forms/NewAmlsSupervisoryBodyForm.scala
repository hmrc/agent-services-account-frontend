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

import play.api.data.Forms.single
import play.api.data.Forms.text
import play.api.data.validation._
import play.api.data.Form
import play.api.data.Mapping

object NewAmlsSupervisoryBodyForm {

  private val trimmedText = text.transform[String](x => x.trim, x => x)
  private val amlsBodyRegex = "^[A-Za-z0-9 \\-,.'&()\\/]*$"
  private val amlsBodyMaxLength = 100

  def amlsSupervisoryBodyUKConstraint(
    bodies: Set[String],
    agentName: String
  ): Constraint[String] = Constraint[String] { fieldValue =>
    Constraints.nonEmpty.apply(fieldValue) match {
      case _: Invalid => Invalid(ValidationError("amls.new-supervisory-body.error", agentName))
      case _ if !bodies.contains(fieldValue) => Invalid(ValidationError("amls.new-supervisory-body.error"))
      case _ => Valid
    }
  }

  def amlsSupervisoryBodyOSConstraint(agentName: String): Constraint[String] = Constraint[String] { fieldValue =>
    Constraints.nonEmpty.apply(fieldValue) match {
      case _: Invalid => Invalid(ValidationError("amls.new-supervisory-body.error", agentName))
      case _ if !fieldValue.matches(amlsBodyRegex) => Invalid(ValidationError("amls.new-supervisory-body.error.os.regex"))
      case _ if fieldValue.length > amlsBodyMaxLength => Invalid(ValidationError("amls.new-supervisory-body.error.os.max-length"))
      case _ => Valid
    }
  }
  private def amlsSupervisoryBodyMapping(
    bodies: Set[String],
    isUkAgent: Boolean,
    agentName: String
  ): Mapping[String] =
    trimmedText verifying (if (isUkAgent)
                             amlsSupervisoryBodyUKConstraint(bodies, agentName)
                           else
                             amlsSupervisoryBodyOSConstraint(agentName))

  def form(amlsBodies: Map[String, String])(
    isUk: Boolean,
    agentName: Option[String]
  ): Form[String] = Form(
    single(
      "body" -> amlsSupervisoryBodyMapping(
        amlsBodies.keySet,
        isUk,
        agentName.getOrElse("")
      )
    )
  )

}
