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

package uk.gov.hmrc.agentservicesaccount.controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.AgentRequest
import uk.gov.hmrc.agentservicesaccount.services.HostnameWhiteListService
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}


@Singleton
class ContinueUrlActions @Inject()(whiteListService: HostnameWhiteListService) {

  def isRelativeOrAbsoluteWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = {
    if (!continueUrl.isRelativeUrl) whiteListService.isAbsoluteUrlWhiteListed(continueUrl)
    else Future.successful(true)
  }

  case class RequestWithMaybeContinueUrl[A](continueUrlOpt: Option[ContinueUrl], request: AgentRequest[A])
    extends WrappedRequest[A](request) {
    def arn: Arn = request.arn
  }

  val WithMaybeContinueUrl = new ActionRefiner[AgentRequest, RequestWithMaybeContinueUrl] {
    override protected def refine[A](request: AgentRequest[A]): Future[Either[Result, RequestWithMaybeContinueUrl[A]]] = {
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)
      val continueUrl = request.getQueryString("continue").fold[Future[Option[ContinueUrl]]](
        Future.successful(None)
      ) { continueParam =>
        Try(ContinueUrl(continueParam)) match {
          case Success(url) =>
            isRelativeOrAbsoluteWhiteListed(url).
              map(if (_) Some(url) else None).
              recover {
                case NonFatal(e) =>
                  Logger.warn(s"Check for whitelisted hostname failed", e)
                  None
              }
          case Failure(_)   => Future.successful(None)
        }
      }
      continueUrl.map(urlOpt => Right(RequestWithMaybeContinueUrl(urlOpt, request)))
    }
  }
}




