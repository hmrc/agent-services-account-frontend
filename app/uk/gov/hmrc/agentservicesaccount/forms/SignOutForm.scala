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

import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._
import play.api.i18n.Messages

object SignOutForm {

  val upperCaseText: Mapping[String] = of[String].transform(_.trim.toUpperCase, identity)

  val supportedSurveyKeys = Set("AGENTSUB", "AGENTHOME", "INVITAGENT")

  val form: Form[String] = Form(
    single(
      "surveyKey" -> optional(upperCaseText)
          .verifying("survey.empty", sk => supportedSurveyKeys.contains(sk.getOrElse("")))
          .transform(_.getOrElse(""), (Some(_)): String => Option[String])
    )
  )

  def surveyKeys(implicit messages: Messages) = supportedSurveyKeys.map(key =>
    key -> Messages(s"survey.form.label.${key.toLowerCase}")
  ).toSeq

}
