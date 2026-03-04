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

package uk.gov.hmrc.agentservicesaccount.models.subscriptions

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class SubscriptionAddress(
                                line1: String,
                                line2: String,
                                line3: Option[String],
                                line4: Option[String],
                                postCode: Option[String]
                              )

object SubscriptionAddress {
  implicit val format: Format[SubscriptionAddress] = Json.format[SubscriptionAddress]
  val payeRegistrationWrites: Writes[SubscriptionAddress] = Writes { address =>
    Json.obj(
      "addressLine1" -> address.line1,
      "addressLine2" -> address.line2,
      "addressLine3" -> address.line3,
      "addressLine4" -> address.line4,
      "postCode" -> address.postCode
    )
  }
}
