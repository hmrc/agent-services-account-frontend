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

package uk.gov.hmrc.agentservicesaccount.auth

import javax.inject.{Inject, Singleton}

import play.api.LoggerLike
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent.Future

case class AgentInfo(arn: Arn)

case class AgentRequest[A](arn: Arn, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class AuthActions @Inject() (logger: LoggerLike, externalUrls: ExternalUrls, override val authConnector: PlayAuthConnector) extends AuthorisedFunctions {

  def AuthorisedWithAgentAsync = new ActionBuilder[AgentRequest] {
    override def invokeBlock[A](request: Request[A], block: (AgentRequest[A]) => Future[Result]): Future[Result] = {
      implicit val hc =  HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
      authorisedWithAgent[Result] { agentInfo =>
        block(AgentRequest(agentInfo.arn, request))
      } map { maybeResult =>
        maybeResult.getOrElse(redirectToGgSignIn)
      } recover {
        case _: NoActiveSession =>
          redirectToGgSignIn
      }
    }
  }

  private def redirectToGgSignIn: Result = Redirect(externalUrls.signInUrl)

  def authorisedWithAgent[R](body: (AgentInfo) => Future[R])(implicit hc: HeaderCarrier): Future[Option[R]] =
    authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments and affinityGroup) {
      case enrol ~ affinityG =>
        (enrol.getEnrolment("HMRC-AS-AGENT"), affinityG) match {
          case (Some(agentEnrolment), Some(AffinityGroup.Agent)) if agentEnrolment.isActivated =>
            getArn(agentEnrolment).map { arn => body(AgentInfo(arn)).map(result => Some(result)) }
              .getOrElse {
                logger.warn("No AgentReferenceNumber found in HMRC-AS-AGENT enrolment - this should not happen. Denying access.")
                Future successful None
              }

          case _ =>
            Future successful None
        }
    }

  private def getArn(enrolment: Enrolment): Option[Arn] = {
    enrolment.getIdentifier("AgentReferenceNumber").map(enrolmentIdentifier => Arn(enrolmentIdentifier.value))
  }

}
