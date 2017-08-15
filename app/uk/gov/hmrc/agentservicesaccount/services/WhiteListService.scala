package uk.gov.hmrc.agentservicesaccount.services

import java.net.URL
import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.agentservicesaccount.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.SsoConnector
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.{Success, Try}

@Singleton
class WhiteListService @Inject()(appConfig: AppConfig, ssoConnector: SsoConnector) {

  val domainWhiteList: Set[String] = appConfig.domainWhiteList

  def hasExternalDomain(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = {
    parseURL(continueUrl.url) match {
      case Success(url) => ssoConnector.validateExternalDomain(url.getHost)
      case _ => Future.successful(false)
    }
  }

  def isAbsoluteUrlWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = {
    if (!hasInternalDomain(continueUrl)) hasExternalDomain(continueUrl)
    else Future.successful(true)
  }

  def hasInternalDomain(continueUrl: ContinueUrl): Boolean = {
    if (continueUrl.isAbsoluteUrl) {
      parseURL(continueUrl.url) match {
        case Success(url) => domainWhiteList.contains(url.getHost)
        case _ => false
      }
    } else false  }

  private def parseURL(url: String): Try[URL] = Try(new URL(url))
}
