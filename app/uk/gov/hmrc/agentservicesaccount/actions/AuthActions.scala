/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.actions

import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Environment, Logging}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
class AuthRequestWithAgentInfo[A](
                         val agentInfo: AgentInfo,
                         val request: Request[A]
                       ) extends WrappedRequest[A](request)

case class AgentInfo(arn: Arn,
                     credentialRole: Option[CredentialRole],
                     email: Option[String] = None,
                     name: Option[Name]= None,
                     credentials: Option[Credentials]= None)  {
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
                            val env: Environment)(implicit ec:ExecutionContext) extends AuthorisedFunctions with Logging {

  def authActionRefiner: ActionRefiner[Request, AuthRequestWithAgentInfo] =
    new ActionRefiner[Request, AuthRequestWithAgentInfo] {

      override protected def refine[A](request: Request[A]): Future[Either[Result, AuthRequestWithAgentInfo[A]]] = {
        implicit val r: Request[A] = request
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
          .retrieve(allEnrolments and credentials and email and name and credentialRole) {
            case enrols ~ creds ~ email ~ name ~ credRole =>
              getArn(enrols) match {
                case Some(arn) =>

                  Future.successful(Right(new  AuthRequestWithAgentInfo(
                    AgentInfo(arn = arn, credentialRole = credRole, email = email, name = name , credentials = creds),r)))
                case None =>
                  logger.warn("No HMRC-AS-AGENT enrolment found -- redirecting to /agent-subscription/start.")
                  Future successful Left(Redirect(appConfig.agentSubscriptionFrontendUrl))
              }
          }.recover(handleFailureRefiner)

      }

      override protected def executionContext: ExecutionContext = ec
    }
  def handleFailureRefiner[A](implicit request: Request[_]): PartialFunction[Throwable, Either[Result, AuthRequestWithAgentInfo[A]]] = {
    case _: NoActiveSession =>
      Left(Redirect(s"${appConfig.signInUrl}?continue_url=${appConfig.continueUrl}${request.uri}&origin=${appConfig.appName}"))

    case _: UnsupportedAuthProvider =>
      logger.warn(s"user logged in with unsupported auth provider")
      Left(Forbidden)

    case _: UnsupportedAffinityGroup =>
      logger.warn(s"user logged in with unsupported affinity group")
      Left(Forbidden)
  }

  def withFullUserDetails(body: AgentInfo => Future[Result])
                         (implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[AnyContent])
  : Future[Result] = {
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments and credentialRole and email and name and credentials) {
        case enrols ~ credRole ~ email ~ name ~ credentials =>
          getArn(enrols) match {
            case Some(arn) =>
              val full = AgentInfo(arn, credRole, email, name, credentials)
              body(full)
            case _ =>
              logger.warn("No HMRC-AS-AGENT enrolment found -- redirecting to /agent-subscription/start.")
              Future successful Redirect(appConfig.agentSubscriptionFrontendUrl)
          }
      }
  }.recover(handleFailure)

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(s"${appConfig.signInUrl}?continue_url=${appConfig.continueUrl}${request.uri}&origin=${appConfig.appName}")

    case _: UnsupportedAuthProvider =>
      logger.warn(s"user logged in with unsupported auth provider")
      Forbidden

    case _: UnsupportedAffinityGroup =>
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
}

object AuthActions {
  type AgentAuthAction = Request[AnyContent] => AgentInfo => Future[Result]
}
