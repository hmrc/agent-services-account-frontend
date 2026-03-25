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
import uk.gov.hmrc.agentservicesaccount.actions.CtJourney
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.controllers.{routes => homeRoutes}

object CtNextPageSelector {

  val UpdateBusinessNamePage = "businessName"
  val UpdatePhoneNumberPage = "phoneNumber"
  val UpdateEmailAddressPage = "emailAddress"

  private val nextPage: (
    String,
    Option[CtJourney]
  ) => Call = {
    case (UpdateBusinessNamePage, _) => subscriptions.routes.CtUpdatePhoneNumberController.showPage
    case (UpdatePhoneNumberPage, _) => subscriptions.routes.CtUpdateEmailAddressController.showPage
    case (UpdateEmailAddressPage, Some(journey)) =>
      journey.useCustomEmail match {
        case Some(false) => homeRoutes.AgentServicesController.root()
        case _ => subscriptions.routes.CtUpdateEmailAddressController.showPage
      }
  }

  def getNextPage(
    currentPage: String,
    journey: Option[CtJourney] = None
  ): Call = {
    nextPage(currentPage, journey)
  }

}
