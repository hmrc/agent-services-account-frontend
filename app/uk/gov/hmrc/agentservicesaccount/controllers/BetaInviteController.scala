/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.auth.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentPermissionsConnector

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BetaInviteController @Inject()
(
  authActions: AuthActions,
  agentPermissionsConnector: AgentPermissionsConnector
)(implicit val appConfig: AppConfig,
                  val cc: MessagesControllerComponents,
                  ec: ExecutionContext,
                  messagesApi: MessagesApi)
  extends AgentServicesBaseController with Logging {

  import authActions._

  implicit class ToFuture[T](t: T) {
    def toFuture: Future[T] = Future successful t
  }

  val hideInvite: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      agentPermissionsConnector.declinePrivateBetaInvite().map(_ =>
        Redirect(routes.AgentServicesController.showAgentServicesAccount())
      )
    }
  }

  val showInvite: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Ok(s"not implemented APB-6581").toFuture
    }
  }

  val showInviteDetails: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Ok(s"not implemented APB-6581").toFuture
    }
  }

  val showInviteCheckYourAnswers: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Ok(s"not implemented APB-6589").toFuture
    }
  }



}
