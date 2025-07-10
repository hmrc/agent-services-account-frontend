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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import play.api.mvc.Results.Redirect
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentservicesaccount.controllers.amlsJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait AmlsJourneySupport {

  implicit val ec: ExecutionContext
  val sessionCacheService: SessionCacheService

  def withUpdateAmlsJourney(body: UpdateAmlsJourney => Future[Result])(
    implicit request: RequestHeader
  ): Future[Result] = {
    sessionCacheService.get(amlsJourneyKey).flatMap {
      case Some(amlsJourney) => body(amlsJourney)
      case None => Future successful Redirect(routes.AgentServicesController.manageAccount)
    }
  }

  def saveAmlsJourney(amlsJourney: UpdateAmlsJourney)(implicit request: RequestHeader): Future[Unit] = sessionCacheService.put(amlsJourneyKey, amlsJourney).map(
    _ => ()
  )

}
