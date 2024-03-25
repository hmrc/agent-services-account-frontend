package uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.{SELECT_CHANGES_CONTACT_DETAILS, ToFuture}
import uk.gov.hmrc.agentservicesaccount.forms.SelectChangesForm
import uk.gov.hmrc.agentservicesaccount.models.SelectChanges
import uk.gov.hmrc.agentservicesaccount.repository.UpdateContactDetailsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.contact_details.select_changes
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SelectDetailsController @Inject()(actions: Actions,
                                        val updateContactDetailsJourneyRepository: UpdateContactDetailsJourneyRepository,
                                        sessionCache: SessionCacheService,
                                        select_changes_view: select_changes
                                       )(implicit appConfig: AppConfig,
                                         cc: MessagesControllerComponents,
                                         ec: ExecutionContext
                                        ) extends FrontendController(cc)
                                          with UpdateContactDetailsJourneySupport with I18nSupport with Logging{

  def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      Ok(select_changes_view(SelectChangesForm.form)).toFuture
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      SelectChangesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Ok(select_changes_view(formWithErrors)).toFuture
          },
          (selectedChanges: SelectChanges) => {
            sessionCache.put(SELECT_CHANGES_CONTACT_DETAILS, selectedChanges.pagesSelected).flatMap {
              _ => getNextPage(sessionCache)
            }
          }
        )
    }
  }
}
