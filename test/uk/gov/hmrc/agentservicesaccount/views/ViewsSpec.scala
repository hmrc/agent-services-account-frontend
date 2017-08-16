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

package uk.gov.hmrc.agentservicesaccount.views

import org.scalatestplus.play.MixedPlaySpec
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.views.html.error_template_Scope0.error_template
import uk.gov.hmrc.agentservicesaccount.views.html.main_template_Scope0.main_template

class ViewsSpec extends MixedPlaySpec {

  "error_template view" should {

    "render title, heading and message" in new App {
      val view = new error_template()
      val html = view.render(
        "My custom page title", "My custom heading", "My custom message",
        FakeRequest(), Messages.Implicits.applicationMessages)

      contentAsString(html) must {
        include("My custom page title") and
          include("My custom heading") and
          include("My custom message")
      }

      val hmtl2 = view.f("My custom page title", "My custom heading", "My custom message")(
        FakeRequest(), Messages.Implicits.applicationMessages
      )
      hmtl2 must be(html)
    }
  }

  "main_template view" should {

    "render title, header, sidebar and main content" in new App {
      val view = new main_template()
      val html = view.render(
        title = "My custom page title",
        navLinks = Some(Html("navLinks")),
        sidebarLinks = Some(Html("sidebarLinks")),
        contentHeader = Some(Html("contentHeader")),
        bodyClasses = Some("bodyClasses"),
        mainClass = Some("mainClass"),
        scriptElem = Some(Html("scriptElem")),
        mainContent = Html("mainContent"),
        request = FakeRequest(),
        messages = Messages.Implicits.applicationMessages
      )

      contentAsString(html) must {
        include("My custom page title") and
          include("sidebarLinks") and
          include("navLinks") and
          include("contentHeader") and
          include("scriptElem") and
          include("mainContent") and
          include("bodyClasses") and
          include("mainClass")
      }

      val hmtl2 = view.f(
        "My custom page title",
        Some(Html("navLinks")),
        Some(Html("sidebarLinks")),
        Some(Html("contentHeader")),
        Some("bodyClasses"),
        Some("mainClass"),
        Some(Html("scriptElem"))
      )(Html("mainContent"))(FakeRequest(), Messages.Implicits.applicationMessages)
      hmtl2 must be(html)
    }

  }

}
