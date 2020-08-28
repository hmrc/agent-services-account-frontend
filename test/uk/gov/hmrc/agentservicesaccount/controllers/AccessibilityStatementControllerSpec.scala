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

import play.api.i18n.{Lang, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec

class AccessibilityStatementControllerSpec extends BaseISpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")

  val controller: AccessibilityStatementController = app.injector.instanceOf[AccessibilityStatementController]

  "display the accessibility statement with a link to contact frontend to report problem" in {
    val result = controller.showAccessibilityStatement()(FakeRequest().withHeaders(HeaderNames.REFERER -> "foo"))
    val content = contentAsString(result)

    status(result) shouldBe 200
    content should include(messagesApi("accessibility.statement.h1"))
    content should include("http://localhost:9250/contact/accessibility?service=AOSS&userAction=foo")
  }
}
