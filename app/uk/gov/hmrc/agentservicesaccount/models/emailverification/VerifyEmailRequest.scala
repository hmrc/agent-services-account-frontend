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

package uk.gov.hmrc.agentservicesaccount.models.emailverification

import play.api.libs.json._

case class VerifyEmailRequest(
  credId: String,
  continueUrl: String,
  origin: String,
  deskproServiceName: Option[String],
  accessibilityStatementUrl: String,
  email: Option[Email],
  lang: Option[String],
  backUrl: Option[String],
  pageTitle: Option[String]
)

case class Email(
  address: String,
  enterUrl: String
)

object Email {
  implicit val format: Format[Email] = Json.format[Email]
}

object VerifyEmailRequest {
  implicit val writes: Writes[VerifyEmailRequest] = Json.writes[VerifyEmailRequest]
}

case class VerifyEmailResponse(redirectUri: String)

object VerifyEmailResponse {
  implicit val formats: Format[VerifyEmailResponse] = Json.format[VerifyEmailResponse]
}

case class CompletedEmail(
  emailAddress: String,
  verified: Boolean,
  locked: Boolean
) {
  def equalsTrimmed(email: String): Boolean = emailAddress.trim.equalsIgnoreCase(email.trim)
}

object CompletedEmail {
  implicit val format: Format[CompletedEmail] = Json.format[CompletedEmail]
}

case class VerificationStatusResponse(emails: List[CompletedEmail])

object VerificationStatusResponse {
  implicit val format: Format[VerificationStatusResponse] = Json.format[VerificationStatusResponse]
}
