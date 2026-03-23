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

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentservicesaccount.actions.CtJourney
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.controllers.{routes => homeRoutes}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesiDetailsJourney
object CtNextPageSelector {

//  TODO: 10902: Replace with CtJourney
  def getNextPage(
    oldJourney: DesiDetailsJourney,
    journey: CtJourney,
    currentPage: String
  ): Result = {
    if (oldJourney.journeyComplete) {
      Redirect(homeRoutes.AgentServicesController.root())
    }
    else {
      val nextPage: Option[String] = oldJourney.contactChangesNeeded.flatMap { pages =>
        if (pages.contains(currentPage)) {
          pages.toSeq.sliding(2).find {
            case Seq(current, _) => current == currentPage
            case _ => false
          }.flatMap {
            case Seq(_, next) => Some(next)
            case _ => None
          }
        }
        else
          pages.headOption
      }
      nextPage match {
        case Some("businessName") => Redirect(subscriptions.routes.CtUpdateBusinessNameController.showPage)
        case Some("emailAddress") => Redirect(subscriptions.routes.CtUpdateEmailAddressController.showPage)
        case None => Redirect(homeRoutes.AgentServicesController.root())
      }
    }
  }
}
