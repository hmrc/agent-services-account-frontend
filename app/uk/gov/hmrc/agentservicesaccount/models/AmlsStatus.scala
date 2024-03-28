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

import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.Format
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.agentservicesaccount.utils.{EnumFormat, ValueClassBinder}

import scala.collection.immutable

sealed trait AmlsStatus extends EnumEntry

object AmlsStatus {
  implicit val format: Format[AmlsStatus] = EnumFormat(AmlsStatuses)
  implicit val queryBindable: QueryStringBindable[AmlsStatus] = ValueClassBinder.queryStringValueBinder[AmlsStatus](_.entryName)
}

object AmlsStatuses  extends Enum[AmlsStatus] {
  final case object NoAmlsDetailsNonUK extends AmlsStatus
  final case object ValidAmlsNonUK extends AmlsStatus

  final case object NoAmlsDetailsUK extends AmlsStatus
  final case object ValidAmlsDetailsUK extends AmlsStatus
  final case object ExpiredAmlsDetailsUK extends AmlsStatus

  final case object PendingAmlsDetails  extends AmlsStatus
  final case object PendingAmlsDetailsRejected  extends AmlsStatus

  override def values: immutable.IndexedSeq[AmlsStatus] = findValues

}

//sealed trait TaxType extends EnumEntry
//
//object TaxType {
//  implicit val format: Format[TaxType] = EnumFormat(TaxTypes)
//  implicit val queryBindable: QueryStringBindable[TaxType] = ValueClassBinder.queryStringValueBinder[TaxType](_.entryName)
//}
//
//object TaxTypes extends Enum[TaxType] {
//  override def values: immutable.IndexedSeq[TaxType] = findValues
//
//  sealed trait BasicTax extends TaxType
//
//  case object COTAX extends BasicTax
//  case object NPS extends TaxType
//  case object NTC extends BasicTax
//  case object PAYE extends TaxType
//  case object SA extends BasicTax
//  case object SAFE extends BasicTax
//  case object SDLT extends BasicTax
//  case object VAT extends TaxType
//}