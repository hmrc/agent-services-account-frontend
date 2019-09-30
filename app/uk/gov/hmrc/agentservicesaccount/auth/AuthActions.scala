/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc._
import play.api.mvc.Results._
import play.api.{Configuration, Environment, LoggerLike}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.AuthActions.AgentAuthAction
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, credentialRole}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

case class AgentInfo(arn: Arn, credentialRole: Option[CredentialRole]) {
  val isAdmin: Boolean = credentialRole match {
    case Some(Assistant) => false
    case Some(_) => true
    case _ => false
  }
}

case class AgentRequest[A](arn: Arn, request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class AuthActions @Inject()(logger: LoggerLike,
                            externalUrls: ExternalUrls,
                            override val authConnector: AuthConnector,
                            val env: Environment,
                            val config: Configuration) extends AuthorisedFunctions with AuthRedirects {

  def withAuthorisedAsAgent(body: AgentAuthAction)(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments and credentialRole) {
        case enrols ~ credRole =>
          getArn(enrols) match {
            case Some(arn) =>
              body(request)(AgentInfo(arn, credRole))
            case None =>
              logger.warn("No AgentReferenceNumber found in HMRC-AS-AGENT enrolment - this should not happen. Denying access.")
              Future successful Forbidden
          }
      }.recover(handleFailure)
  }

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession ⇒
      Redirect(externalUrls.agentSubscriptionUrl + encodeContinueUrl)

    case _: UnsupportedAuthProvider ⇒
      logger.warn(s"user logged in with unsupported auth provider")
      Forbidden

    case _: UnsupportedAffinityGroup ⇒
      logger.warn(s"user logged in with unsupported affinity group")
      Forbidden
  }

  private def encodeContinueUrl(implicit request: Request[_]): String = {
    import CallOps._
    request.session.get("otacTokenParam") match {
      case Some(p) =>
        val selfURL = routes.AgentServicesController.root().toURLWithParams("p" -> Some(p))
        "?continue=" + URLEncoder.encode(selfURL, "utf-8")
      case None => ""
    }
  }

  private def getArn(enrolments: Enrolments): Option[Arn] = {
    for {
      agentEnrol <- enrolments.getEnrolment("HMRC-AS-AGENT")
      enrolId <- agentEnrol.getIdentifier("AgentReferenceNumber")
    } yield {
      Arn(enrolId.value)
    }
  }
}

object AuthActions {
  type AgentAuthAction = Request[AnyContent] => AgentInfo => Future[Result]
}
