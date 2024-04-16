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

package uk.gov.hmrc.agentservicesaccount.models

import play.api.libs.json._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDate, ZoneId}

case class PendingChangeRequest(
                                 arn: Arn,
                                 timeSubmitted: Instant
                                 ) {
  def localDateSubmitted: LocalDate = timeSubmitted.atZone(ZoneId.of("Europe/London")).toLocalDate
}

object PendingChangeRequest {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat // important to allow Mongo TTL index to work
  implicit val format: Format[PendingChangeRequest] = Json.format[PendingChangeRequest]
}
