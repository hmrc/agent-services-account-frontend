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

package uk.gov.hmrc.agentservicesaccount.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentservicesaccount.models.SelectChanges

object SelectChangesForm {

  def form: Form[SelectChanges] = {
    Form(
      mapping(
        "businessName" -> optional(nonEmptyText),
        "address" -> optional(nonEmptyText),
        "email" -> optional(nonEmptyText),
        "telephone" -> optional(nonEmptyText)
      )(SelectChanges.apply)(SelectChanges.unapply)
        .verifying("contact-details.select-changes.error", _.atLeastOneSelected)
    )
  }
}
