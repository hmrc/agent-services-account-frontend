package uk.gov.hmrc.agentservicesaccount.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.Result
import uk.gov.hmrc.agentservicesaccount.services.WhiteListService
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class ContinueUrlValidator @Inject()(whiteListService: WhiteListService) {
  def isRelativeOrAbsoluteWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = {
    if (!continueUrl.isRelativeUrl) whiteListService.isAbsoluteUrlWhiteListed(continueUrl)
    else Future.successful(true)
  }
}

trait WhitelistedContinueUrl {
  def continueUrlValidator: ContinueUrlValidator

  def withWhitelistedContinueUrl(continue: ContinueUrl)(body: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    continueUrlValidator.isRelativeOrAbsoluteWhiteListed(continue).flatMap {
      if (_) body
      else Future.failed(new Exception("not valid continue url")) //FIXME
    }
  }
}


