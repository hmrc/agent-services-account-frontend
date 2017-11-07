/*
 * Copyright 2017 HM Revenue & Customs
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

import java.net.URL
import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.agentservicesaccount.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.SsoConnector
import uk.gov.hmrc.play.binders.ContinueUrl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class HostnameWhiteListService @Inject()(appConfig: AppConfig, ssoConnector: SsoConnector) {

  val domainWhiteList: Set[String] = appConfig.domainWhiteList

  def hasExternalDomain(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    ssoConnector.validateExternalDomain(getHost(continueUrl))


  def isAbsoluteUrlWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    if (!hasInternalDomain(continueUrl)) hasExternalDomain(continueUrl)
    else Future.successful(true)

  def hasInternalDomain(continueUrl: ContinueUrl): Boolean = domainWhiteList.contains(getHost(continueUrl))

  private def getHost(continueUrl: ContinueUrl): String = Try(new URL(continueUrl.url).getHost).getOrElse("invalid")
}
