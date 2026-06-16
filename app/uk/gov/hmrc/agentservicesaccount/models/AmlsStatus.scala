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

import enumeratum.Enum
import enumeratum.EnumEntry
import play.api.libs.json.Format
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.agentservicesaccount.utils.EnumFormat

sealed trait AmlsStatus
extends Product
with Serializable
with EnumEntry

object AmlsStatus {

  implicit val format: Format[AmlsStatus] = EnumFormat(AmlsStatuses)
  implicit val queryBindable: QueryStringBindable[AmlsStatus] =
    new QueryStringBindable[AmlsStatus] {
      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, AmlsStatus]] = QueryStringBindable.bindableString.bind(key, params).map {
        case Right(value) =>
          AmlsStatuses.withNameOption(value) match {
            case Some(status) => Right(status)
            case None => Left(s"Cannot parse param $key as AmlsStatus")
          }
        case Left(error) => Left(error)
      }

      override def unbind(
        key: String,
        value: AmlsStatus
      ): String = QueryStringBindable.bindableString.unbind(key, value.entryName)
    }

}

object AmlsStatuses
extends Enum[AmlsStatus] {

  val values: IndexedSeq[AmlsStatus] = findValues

  final case object NoAmlsDetailsNonUK
  extends AmlsStatus
  final case object ValidAmlsNonUK
  extends AmlsStatus
  final case object NoAmlsDetailsUK
  extends AmlsStatus
  final case object ValidAmlsDetailsUK
  extends AmlsStatus
  final case object ExpiredAmlsDetailsUK
  extends AmlsStatus
  final case object PendingAmlsDetails
  extends AmlsStatus
  final case object PendingAmlsDetailsRejected
  extends AmlsStatus

}
