/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Configuration
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

@Singleton
class AgentServicesLanguageController @Inject()(
  configuration: Configuration,
  languageUtils: LanguageUtils,
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  appConfig: AppConfig)
    extends LanguageController(configuration, languageUtils, cc) {

  override def languageMap: Map[String, Lang] = appConfig.languageMap

  override def fallbackURL: String = "https://www.tax.service.gov.uk/agent-services-account"

}
