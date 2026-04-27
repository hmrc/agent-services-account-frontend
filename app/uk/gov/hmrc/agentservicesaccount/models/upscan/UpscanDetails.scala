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

package uk.gov.hmrc.agentservicesaccount.models.upscan

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.JsonNaming
import play.api.libs.json.Reads
import play.api.libs.json.__
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

sealed trait UpscanDetails {

  val reference: FileUploadReference
  val timestamp: Instant

}
case class UpscanInProgress(
  reference: FileUploadReference,
  timestamp: Instant
)
extends UpscanDetails

case class UpscanSuccess(
  reference: FileUploadReference,
  timestamp: Instant,
  downloadUrl: String,
  fileName: String,
  mimeType: String,
  checksum: String,
  sizeInBytes: Long
)
extends UpscanDetails

case class UpscanFailure(
  reference: FileUploadReference,
  timestamp: Instant,
  failureReason: String,
  messageFromUpscan: String
)
extends UpscanDetails {
  lazy val isInQuarantine: Boolean = failureReason == "QUARANTINE"
}

object UpscanDetails {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat // important to allow Mongo TTL index to work
  implicit val inProgressFormat: Format[UpscanInProgress] = Json.format[UpscanInProgress]
  implicit val successFormat: Format[UpscanSuccess] = Json.format[UpscanSuccess]
  implicit val failureFormat: Format[UpscanFailure] = Json.format[UpscanFailure]
  implicit val format: Format[UpscanDetails] = Json.format[UpscanDetails]
  private val notificationSuccessReads: Reads[UpscanSuccess] =
    (
      (__ \ "reference").read[FileUploadReference] and
        Reads.pure[Instant](Instant.now()) and
        (__ \ "downloadUrl").read[String] and
        (__ \ "uploadDetails" \ "fileName").read[String] and
        (__ \ "uploadDetails" \ "fileMimeType").read[String] and
        (__ \ "uploadDetails" \ "checksum").read[String] and
        (__ \ "uploadDetails" \ "size").read[Long]
    )(UpscanSuccess.apply _)

  private val notificationFailureReads: Reads[UpscanFailure] =
    (
      (__ \ "reference").read[FileUploadReference] and
        Reads.pure[Instant](Instant.now()) and
        (__ \ "failureDetails" \ "failureReason").read[String] and
        (__ \ "failureDetails" \ "message").read[String]
    )(UpscanFailure.apply _)

  val callbackReads: Reads[UpscanDetails] = Reads { json =>
    (json \ "fileStatus").validate[String] match {
      case JsSuccess("READY", _) => json.validate[UpscanSuccess](notificationSuccessReads)
      case JsSuccess("FAILED", _) => json.validate[UpscanFailure](notificationFailureReads)
      case _ => JsError("Unknown 'fileStatus' in notification response")
    }

  }

}
