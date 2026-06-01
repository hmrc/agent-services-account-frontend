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

package uk.gov.hmrc.agentservicesaccount.controllers.internal

import play.api.Logging
import play.api.mvc.Action
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanDetails
import uk.gov.hmrc.agentservicesaccount.repository.UpscanRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpscanCallbackController @Inject() (
  upscanRepository: UpscanRepository,
  cc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
extends FrontendController(cc)
with Logging {

  def callback: Action[UpscanDetails] =
    Action.async(parse.json[UpscanDetails](UpscanDetails.callbackReads)) { implicit request =>
      upscanRepository.findByReference(request.body.reference).flatMap {
        case Some(details) if details.reference == request.body.reference =>
          upscanRepository
            .saveUpscanDetails(request.body)
            .map(_ => NoContent)
        case _ =>
          val msg = s"No upload details found for reference: ${request.body.reference}"
          logger.error(msg)
          Future.successful(NotFound(msg))
      }
    }

}
