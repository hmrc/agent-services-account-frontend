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

import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentservicesaccount.models.AmlsJourneySession
import uk.gov.hmrc.agentservicesaccount.repository.AmlsJourneySessionRepository
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

trait AmlsJourneySupport {

  val amlsJourneySessionRepository: AmlsJourneySessionRepository

  val dataKey = DataKey[AmlsJourneySession]("amlsJourney")

  def withAmlsJourneySession(body: AmlsJourneySession => Future[Result])(
    implicit request: Request[_], ec: ExecutionContext) = {
    amlsJourneySessionRepository.getFromSession(dataKey).flatMap{
      case Some(amlsJourney) => body(amlsJourney)
      case None => Future successful Redirect(routes.AgentServicesController.manageAccount)
    }
  }

  def testOnlyInitialiseAmlsJourneySession(implicit request: Request[_], ec: ExecutionContext) =
    amlsJourneySessionRepository.putSession(dataKey, AmlsJourneySession(status = "UKAMLS")).map(_ => ())

  def saveAmlsJourneySession(amlsJourney: AmlsJourneySession)(implicit request: Request[_], ec: ExecutionContext): Future[Unit] =
    amlsJourneySessionRepository.putSession(dataKey, amlsJourney).map(_ => ())

}
