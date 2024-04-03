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
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthRequestWithAgentInfo}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.{DRAFT_SUBMITTED_BY, routes}
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.agentservicesaccount.views.html.pages.contact_details.update_your_details
import uk.gov.hmrc.agentservicesaccount.models.YourDetails
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourDetailsController @Inject()(
                                       actions: Actions,
                                       sessionCache: SessionCacheService,
                                       pcodRepository: PendingChangeOfDetailsRepository,
                                       updateYourDetailsView: update_your_details
                           )(implicit appConfig: AppConfig,
                              cc: MessagesControllerComponents,
                              ec: ExecutionContext
) extends FrontendController(cc) with I18nSupport with Logging {

  def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  private def ifFeatureEnabledAndNoPendingChanges(action: => Future[Result])(implicit request: AuthRequestWithAgentInfo[_]): Future[Result] = {
    ifFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          Future.successful(Redirect(routes.ContactDetailsController.showCurrentContactDetails))
      }
    }
  }
  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      sessionCache.get[YourDetails](DRAFT_SUBMITTED_BY).map {
        case Some(data) => Ok(updateYourDetailsView(UpdateDetailsForms.yourDetailsForm.fill(data)))
        case _ => Ok(updateYourDetailsView(UpdateDetailsForms.yourDetailsForm))
      }
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.yourDetailsForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(updateYourDetailsView(formWithErrors))),
          submittedBy => {
            updateSubmittedBy(submittedBy).map(_ =>
              Redirect(routes.ContactDetailsController.showCheckNewDetails)
            )
          }
        )
    }
  }

  private def updateSubmittedBy(f: YourDetails)(implicit request: Request[_]): Future[Unit] = for {
    _ <- sessionCache.put[YourDetails](DRAFT_SUBMITTED_BY, f)
  } yield ()
}
