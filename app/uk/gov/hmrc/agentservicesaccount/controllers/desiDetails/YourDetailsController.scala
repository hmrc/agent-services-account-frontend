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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.your_details
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class YourDetailsController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  updateYourDetailsView: your_details
)(implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  val ec: ExecutionContext,
  pcodRepository: PendingChangeRequestRepository
)
extends FrontendController(cc)
with I18nSupport
with DesiDetailsJourneySupport
with Logging {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      isOtherServicesPageRequestValid().flatMap {
        case true =>
          sessionCacheService.get[YourDetails](draftSubmittedByKey).map {
            case Some(data) => Ok(updateYourDetailsView(UpdateDetailsForms.yourDetailsForm.fill(data)))
            case _ => Ok(updateYourDetailsView(UpdateDetailsForms.yourDetailsForm))
          }
        case _ => Future.successful(Redirect(routes.ViewContactDetailsController.showPage))
      }

    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.yourDetailsForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(updateYourDetailsView(formWithErrors))),
          submittedBy => {
            updateSubmittedBy(submittedBy).map(_ =>
              Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
            )
          }
        )
    }
  }

  private def updateSubmittedBy(f: YourDetails)(implicit request: RequestHeader): Future[Unit] =
    for {
      _ <- sessionCacheService.put[YourDetails](draftSubmittedByKey, f)
    } yield ()

}
