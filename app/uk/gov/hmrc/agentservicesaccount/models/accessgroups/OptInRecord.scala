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

package uk.gov.hmrc.agentservicesaccount.models.accessgroups

import play.api.libs.json._
import uk.gov.hmrc.agentservicesaccount.models.Arn

import java.time.LocalDateTime

sealed trait OptinStatus {
  val value: String
}

case object OptedInSingleUser
extends OptinStatus {
  override val value = "Opted-In_SINGLE_USER"
}
case object OptedOutSingleUser
extends OptinStatus {
  override val value = "Opted-Out_SINGLE_USER"
}
case object OptedOutWrongClientCount
extends OptinStatus {
  override val value = "Opted-Out_WRONG_CLIENT_COUNT"
}
case object OptedOutEligible
extends OptinStatus {
  override val value = "Opted-Out_ELIGIBLE"
}
case object OptedInReady
extends OptinStatus {
  override val value = "Opted-In_READY"
}
case object OptedInNotReady
extends OptinStatus {
  override val value = "Opted-In_NOT_READY"
}

object OptinStatus {

  implicit val reads: Reads[OptinStatus] = {
    case JsString(OptedInReady.value) => JsSuccess(OptedInReady)
    case JsString(OptedInNotReady.value) => JsSuccess(OptedInNotReady)
    case JsString(OptedInSingleUser.value) => JsSuccess(OptedInSingleUser)
    case JsString(OptedOutEligible.value) => JsSuccess(OptedOutEligible)
    case JsString(OptedOutWrongClientCount.value) => JsSuccess(OptedOutWrongClientCount)
    case JsString(OptedOutSingleUser.value) => JsSuccess(OptedOutSingleUser)
    case invalid => JsError(s"Invalid OptedIn value found: $invalid")
  }

  implicit val writes: Writes[OptinStatus] = (o: OptinStatus) => JsString(o.value)

}

sealed trait OptinEventType {
  val value: String = getClass.getSimpleName.dropRight(1)
}
case object OptedIn
extends OptinEventType
case object OptedOut
extends OptinEventType

object OptinEventType {

  implicit val reads: Reads[OptinEventType] = {
    case JsString(OptedIn.value) => JsSuccess(OptedIn)
    case JsString(OptedOut.value) => JsSuccess(OptedOut)
    case invalid => JsError(s"Invalid OptinEventType value found: $invalid")
  }

  implicit val writes: Writes[OptinEventType] = (o: OptinEventType) => JsString(o.value)

}

case class OptinEvent(
  optinEventType: OptinEventType,
  user: AgentUser,
  eventDateTime: LocalDateTime
)

object OptinEvent {
  implicit val formatOptinEvent: OFormat[OptinEvent] = Json.format[OptinEvent]
}

case class OptinRecord(
  arn: Arn,
  history: List[OptinEvent]
) {

  lazy val status: OptinEventType =
    history match {
      case Nil => OptedOut
      case events => events.sortWith(_.eventDateTime isAfter _.eventDateTime).head.optinEventType
    }
}

object OptinRecord {

  implicit val reads: Reads[OptinRecord] = Json.reads[OptinRecord]

  implicit val writes: Writes[OptinRecord] =
    (optinRecord: OptinRecord) =>
      Json.obj(
        fields = "arn" -> optinRecord.arn,
        "status" -> optinRecord.status,
        "history" -> optinRecord.history
      )

  implicit val format: Format[OptinRecord] = Format(reads, writes)

}
