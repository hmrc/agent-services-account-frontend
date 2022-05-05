/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.models

import play.api.libs.json._

sealed trait OptinStatus {
  val value: String
}

case object OptedInSingleUser extends OptinStatus {
  override val value = "Opted-In_SINGLE_USER"
}
case object OptedOutSingleUser extends OptinStatus {
  override val value = "Opted-Out_SINGLE_USER"
}
case object OptedOutWrongClientCount extends OptinStatus {
  override val value = "Opted-Out_WRONG_CLIENT_COUNT"
}
case object OptedOutEligible extends OptinStatus {
  override val value = "Opted-Out_ELIGIBLE"
}
case object OptedInReady extends OptinStatus {
  override val value = "Opted-In_READY"
}
case object OptedInNotReady extends OptinStatus {
  override val value = "Opted-In_NOT_READY"
}


object OptinStatus {
  implicit val reads: Reads[OptinStatus] = new Reads[OptinStatus] {
    override def reads(json: JsValue): JsResult[OptinStatus] =
      json match {
        case JsString(OptedInReady.value)             => JsSuccess(OptedInReady)
        case JsString(OptedInNotReady.value)          => JsSuccess(OptedInNotReady)
        case JsString(OptedInSingleUser.value)        => JsSuccess(OptedInSingleUser)
        case JsString(OptedOutEligible.value)         => JsSuccess(OptedOutEligible)
        case JsString(OptedOutWrongClientCount.value) => JsSuccess(OptedOutWrongClientCount)
        case JsString(OptedOutSingleUser.value)       => JsSuccess(OptedOutSingleUser)
        case invalid                                  => JsError(s"Invalid OptedIn value found: $invalid")
      }
  }

  implicit val writes: Writes[OptinStatus] = new Writes[OptinStatus] {
    override def writes(o: OptinStatus): JsValue = JsString(o.value)
  }
}
