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

package uk.gov.hmrc.agentservicesaccount.controllers.testOnly


import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.models.{AmlsStatus, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestOnlyController @Inject()(updateAmlsJourneyRepository: UpdateAmlsJourneyRepository)(implicit
                                                                                             ec: ExecutionContext,
                                                                                             cc: MessagesControllerComponents) extends FrontendController(cc) {
  def initialiseAmls(amlsStatus: Option[AmlsStatus]): Action[AnyContent] = Action.async { implicit request =>
    updateAmlsJourneyRepository.putSession(DataKey[UpdateAmlsJourney]("amlsJourney"),
      UpdateAmlsJourney(
        status = amlsStatus.getOrElse(AmlsStatus.ValidAmlsDetailsUK
      ))).map(_ => Ok("successfully created amls"))
  }

}

