/*
 * Copyright 2018 HM Revenue & Customs
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

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import play.api.LoggerLike
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{affinityGroup, allEnrolments, credentialRole}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent.Future

case class AgentInfo(arn: Arn, credentialRole: Option[CredentialRole]) {
  val isAdmin: Boolean = credentialRole match {
    case Some(Admin) => true
    case _ => false
  }
}

case class AgentRequest[A](arn: Arn, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class AuthActions @Inject()(logger: LoggerLike, externalUrls: ExternalUrls, override val authConnector: AuthConnector) extends AuthorisedFunctions {

  def redirectToAgentSubscriptionGgSignIn[A](implicit request: Request[A]): Result =
    Redirect(externalUrls.agentSubscriptionUrl + encodeContinueUrl)

  def encodeContinueUrl[A](implicit request: Request[A]): String = {
    import CallOps._
    request.session.get("otacTokenParam") match {
      case Some(p) =>
        val selfURL = routes.AgentServicesController.root().toURLWithParams("p" -> Some(p))
        "?continue="+URLEncoder.encode(selfURL,"utf-8")
      case None => ""
    }

  }

  def authorisedWithAgent[A,R](body: (AgentInfo) => Future[R])(implicit headerCarrier: HeaderCarrier): Future[Option[R]] =
    authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments and affinityGroup and credentialRole) {
      case enrol ~ affinityG ~ credRole =>
        (enrol.getEnrolment("HMRC-AS-AGENT"), affinityG, credRole) match {
          case (Some(agentEnrolment), Some(AffinityGroup.Agent), _) if agentEnrolment.isActivated =>
            getArn(agentEnrolment).map { arn => body(AgentInfo(arn, credRole)).map(result => Some(result)) }
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
