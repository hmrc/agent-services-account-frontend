/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.i18n.Lang
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.play.language.LanguageController
import uk.gov.hmrc.play.language.LanguageUtils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentServicesLanguageController @Inject() (
  languageUtils: LanguageUtils,
  cc: MessagesControllerComponents,
  appConfig: AppConfig
)
extends LanguageController(languageUtils, cc) {

  override def languageMap: Map[String, Lang] = appConfig.languageMap

  override def fallbackURL: String = "https://www.tax.service.gov.uk/agent-services-account"

}
