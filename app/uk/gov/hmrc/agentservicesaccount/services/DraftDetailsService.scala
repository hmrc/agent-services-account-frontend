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

package uk.gov.hmrc.agentservicesaccount.services

import play.api.mvc.Request
import uk.gov.hmrc.agentservicesaccount.controllers.DRAFT_NEW_CONTACT_DETAILS
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class DraftDetailsService @Inject()(sessionCacheService: SessionCacheService)(implicit ec: ExecutionContext) {
  def updateDraftDetails(f: DesignatoryDetails => DesignatoryDetails)
                        (implicit request: Request[_]): Future[Unit] =
    for {
      optDraftDetailsInSession <- sessionCacheService.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
      draftDetails <- optDraftDetailsInSession match {
        case Some(details) => Future.successful(details)
        case None =>
          Future.successful(
              DesignatoryDetails(
                agencyDetails = AgencyDetails(None, None, None, None),
                otherServices = OtherServices(
                  saChanges = SaChanges(
                    applyChanges = false,
                    saAgentReference = None),
                  ctChanges = CtChanges(
                    applyChanges = false,
                    ctAgentReference = None
                  )
                )
              )
            )
      }
      updatedDraftDetails = f(draftDetails)
      _ <- sessionCacheService.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, updatedDraftDetails)
    } yield ()

}
