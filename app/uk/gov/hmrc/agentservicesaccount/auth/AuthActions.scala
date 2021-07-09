/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.Assets.Redirect

import javax.inject.{Inject, Singleton}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, credentialRole}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier

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
class AuthActions @Inject()(appConfig: AppConfig,
                            override val authConnector: AuthConnector,
                            val env: Environment,
                            val config: Configuration) extends AuthorisedFunctions {

  val logger = Logger(AuthActions.getClass)

  def withAuthorisedAsAgent(body: AgentInfo => Future[Result])(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(allEnrolments and credentialRole) {
        case enrols ~ credRole =>
          getArn(enrols) match {
            case Some(arn) =>
              body(AgentInfo(arn, credRole))
            case None =>
              logger.warn("No HMRC-AS-AGENT enrolment found -- redirecting to /agent-subscription/start.")
              Future successful Redirect(appConfig.agentSubscriptionFrontendUrl)
          }
      }.recover(handleFailure)

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession ⇒
      Redirect(s"$signInUrl?continue_url=$continueUrl${request.uri}&origin=$appName")

    case _: UnsupportedAuthProvider ⇒
      logger.warn(s"user logged in with unsupported auth provider")
      Forbidden

    case _: UnsupportedAffinityGroup ⇒
      logger.warn(s"user logged in with unsupported affinity group")
      Forbidden
  }

  private def getArn(enrolments: Enrolments): Option[Arn] = {
    for {
      agentEnrol <- enrolments.getEnrolment("HMRC-AS-AGENT")
      enrolId <- agentEnrol.getIdentifier("AgentReferenceNumber")
    } yield {
      Arn(enrolId.value)
    }
  }

  private def getString(key: String): String = config.underlying.getString(key)

  private val signInUrl = getString("bas-gateway.url")
  private val continueUrl = getString("login.continue")
  private val appName = getString("appName")
}

object AuthActions {
  type AgentAuthAction = Request[AnyContent] => AgentInfo => Future[Result]
}
