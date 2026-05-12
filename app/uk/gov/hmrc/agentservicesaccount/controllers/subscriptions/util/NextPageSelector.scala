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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util

import play.api.mvc.Call
import uk.gov.hmrc.agentservicesaccount.controllers.arnKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.controllers.{routes => homeRoutes}
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.CT_SA_EMAIL_MAX_LENGTH
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA

object NextPageSelector {

  val updateBusinessNamePage = "businessName"
  val payeUpdateContactNamePage = "payeContactName"
  val updatePhoneNumberPage = "phoneNumber"
  val updateEmailAddressPage = "emailAddress"
  val emailVerificationFinish = "emailVerificationFinish"
  val updateAddressPage = "address"
  val changeAddressPage = "changeAddress"
  val addressLookupFinish = "addressLookupFinish"
  val checkYourAnswersPage = "checkYourAnswers"

  private val nextPage: (
    String,
    Option[SubscriptionJourney],
    LegacyRegime
  ) => Call = {
    case (_, Some(journey), regime) if journey.isSubmitted => subscriptions.routes.ConfirmationController.showConfirmationPage(regime)
    case (_, Some(journey), regime) if journey.isComplete(regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
    case (`payeUpdateContactNamePage`, _, PAYE) => subscriptions.routes.UpdatePhoneNumberController.showPage(PAYE)
    case (`updateBusinessNamePage`, _, regime) => subscriptions.routes.UpdatePhoneNumberController.showPage(regime)
    case (`updatePhoneNumberPage`, _, regime) => subscriptions.routes.UpdateEmailAddressController.showPage(regime)
    case (`updateEmailAddressPage`, Some(journey), regime) =>
      (journey.useCustomEmail, regime, journey.asaDetails.agencyEmail.map(_.length)) match {
        case (Some(false), CT | SA, Some(length)) if length > CT_SA_EMAIL_MAX_LENGTH =>
          subscriptions.routes.UpdateEmailAddressController.showSaCtCustomPage(regime)
        case (Some(false), _, _) => subscriptions.routes.UpdateAddressController.showPage(regime)
        case _ => subscriptions.routes.UpdateEmailAddressController.showPage(regime)
      }
    case (`emailVerificationFinish`, _, regime) => subscriptions.routes.UpdateAddressController.showPage(regime)
    case (`updateAddressPage`, Some(journey), regime) =>
      journey.useCustomAddress match {
        case Some(false) if journey.addressValidForRegime(regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
        case Some(false) => subscriptions.routes.UpdateAddressController.showChange(regime, isInvalid = true)
        case _ => subscriptions.routes.UpdateAddressController.showPage(regime)
      }
    case (`addressLookupFinish`, Some(journey), regime) =>
      (journey.useCustomAddress, journey.addressAnswer) match {
        case (Some(true), Some(_)) if journey.addressValidForRegime(regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
        case (Some(true), Some(_)) => subscriptions.routes.UpdateAddressController.showChange(regime, isInvalid = true)
        case _ => subscriptions.routes.UpdateAddressController.showPage(regime)
      }
    case (`changeAddressPage`, _, regime) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
  }

  def getNextPage(
    currentPage: String,
    journey: Option[SubscriptionJourney] = None,
    legacyRegime: LegacyRegime
  ): Call = {
    nextPage(
      currentPage,
      journey,
      legacyRegime
    )
  }

}
