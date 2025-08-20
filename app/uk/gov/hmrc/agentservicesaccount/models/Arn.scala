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

package uk.gov.hmrc.agentservicesaccount.models

import uk.gov.hmrc.domain.SimpleObjectReads
import uk.gov.hmrc.domain.SimpleObjectWrites
import uk.gov.hmrc.domain.TaxIdentifier

case class Arn(value: String)
extends TaxIdentifier

object Arn {

  private val arnPattern = "^[A-Z]ARN[0-9]{7}$".r

  def isValid(arn: String): Boolean =
    arn match {
      case arnPattern(_*) => ArnCheck.isValid(arn)
      case _ => false
    }

  implicit val arnReads: SimpleObjectReads[Arn] = new SimpleObjectReads[Arn]("value", Arn.apply)
  implicit val arnWrites: SimpleObjectWrites[Arn] = new SimpleObjectWrites[Arn](_.value)

}
