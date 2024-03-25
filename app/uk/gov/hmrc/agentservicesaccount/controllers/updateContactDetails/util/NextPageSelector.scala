package uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails.util

import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentservicesaccount.controllers.{SELECT_CHANGES_CONTACT_DETAILS, routes}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService

import scala.concurrent.{ExecutionContext, Future}

object NextPageSelector {

  def getNextPage(sessionCache: SessionCacheService, userIsOnCheckYourAnswersFlow: Boolean = false)(implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    for {
      pagesRequired <- sessionCache.get(SELECT_CHANGES_CONTACT_DETAILS)
      nextPage = getPage(pagesRequired, userIsOnCheckYourAnswersFlow)
      newPagesRequired = pagesRequired.map(_.drop(1)).getOrElse(Set.empty[String])
      _ <- sessionCache.put(SELECT_CHANGES_CONTACT_DETAILS, newPagesRequired)
    } yield nextPage
  }

  //TODO: use enums
  private def getPage(pagesRequired: Option[Set[String]], userIsOnCheckYourAnswersFlow: Boolean): Result = {
    pagesRequired.headOption.map {
      case "businessName" => Redirect(routes.ContactDetailsController.showChangeBusinessName)
      case "address" => Redirect(routes.ContactDetailsController.showChangeEmailAddress) //TODO: Update routing
      case "email" => Redirect(routes.ContactDetailsController.showChangeEmailAddress)
      case "telephone" => Redirect(routes.ContactDetailsController.showChangeTelephoneNumber)
    }.getOrElse {
      if (userIsOnCheckYourAnswersFlow) Redirect(routes.ContactDetailsController.showCurrentContactDetails)
      else Redirect(routes.ContactDetailsController.showCheckNewDetails)
    }
  }
}
