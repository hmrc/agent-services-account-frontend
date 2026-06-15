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

package uk.gov.hmrc.agentservicesaccount.forms.subscriptions

import play.api.data.Forms._
import play.api.data.Form
import play.api.data.Mapping
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.useAsaDataMapping
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.YouMayNotNeedToApplyFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

object YouMayNotNeedToApplyForm {

  val doYouStillWantToApplyKey = "doYouStillWantToApply"

  private def doYouStillWantToApplyMapping(legacyRegime: LegacyRegime): Mapping[Boolean] = useAsaDataMapping(
    s"${legacyRegime.msgPrefix}.you-may-not-need-to-apply.error.required"
  )

  def form(legacyRegime: LegacyRegime): Form[YouMayNotNeedToApplyFormValues] = Form(
    mapping(
      doYouStillWantToApplyKey -> doYouStillWantToApplyMapping(legacyRegime)
    )(YouMayNotNeedToApplyFormValues.apply)(values => Some(values.doYouStillWantToApply))
  )

}
