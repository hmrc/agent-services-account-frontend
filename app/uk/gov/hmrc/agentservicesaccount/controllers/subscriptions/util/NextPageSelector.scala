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
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.controllers.{routes => homeRoutes}
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionJourney
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

object NextPageSelector {

  val updateBusinessNamePage = "businessName"
  val updatePhoneNumberPage = "phoneNumber"
  val updateEmailAddressPage = "emailAddress"
  val emailVerificationFinish = "emailVerificationFinish"
  val updateAddressPage = "address"
  val addressLookupFinish = "addressLookupFinish"
  val checkYourAnswersPage = "checkYourAnswers"
  val confirmationPage = "confirmationAnswers"

  private val nextPage: (
    String,
    Option[SubscriptionJourney],
    LegacyRegime
  ) => Call = {
    case (_, Some(journey), regime) if journey.isComplete => subscriptions.routes.CheckYourAnswersController.showPage(regime)
//    TODO: 11186 Need to add for PAYE Contact Name Page
    case (`updateBusinessNamePage`, _, regime) => subscriptions.routes.UpdatePhoneNumberController.showPage(regime)
    case (`updatePhoneNumberPage`, _, regime) => subscriptions.routes.UpdateEmailAddressController.showPage(regime)
    case (`updateEmailAddressPage`, Some(journey), regime) =>
      journey.useCustomEmail match {
        case Some(false) => subscriptions.routes.UpdateAddressController.showPage(regime)
        case _ => subscriptions.routes.UpdateEmailAddressController.showPage(regime)
      }
    case (`emailVerificationFinish`, _, regime) => subscriptions.routes.UpdateAddressController.showPage(regime)
    case (`updateAddressPage`, Some(journey), regime) =>
      journey.useCustomAddress match {
        case Some(false) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
        case _ => subscriptions.routes.UpdateAddressController.showPage(regime)
      }
    case (`addressLookupFinish`, Some(journey), regime) =>
      (journey.useCustomAddress, journey.addressAnswer) match {
        case (Some(true), Some(_)) => subscriptions.routes.CheckYourAnswersController.showPage(regime)
        case _ => subscriptions.routes.UpdateAddressController.showPage(regime)
      }
    case (`checkYourAnswersPage`, _, regime) => subscriptions.routes.ConfirmationController.showConfirmationPage(regime)
    case (`confirmationPage`, _, _) => homeRoutes.AgentServicesController.root()
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
