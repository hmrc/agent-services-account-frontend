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

package uk.gov.hmrc.agentservicesaccount.controllers

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models.AmlsJourney
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AmlsJourneySupport {

  val agentAssuranceConnector: AgentAssuranceConnector

  def maybeAmlsJourneyRecord(body: Option[AmlsJourney] => Future[Result])(
    implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    agentAssuranceConnector.getAmlsJourney.flatMap(amls => body(amls))
  }

  def amlsJourneyRecord(body: AmlsJourney => Future[Result])(
    implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    agentAssuranceConnector.getAmlsJourney.flatMap{
      case Some(amlsJourney) => body(amlsJourney)
      case None => Future successful Redirect(routes.AgentServicesController.manageAccount)
    }
  }

}
