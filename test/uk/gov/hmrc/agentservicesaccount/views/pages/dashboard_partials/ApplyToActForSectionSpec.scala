/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.views.pages.dashboard_partials

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Lang
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.actions.AgentInfo
import uk.gov.hmrc.agentservicesaccount.actions.AuthRequestWithAgentInfo
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionInfo
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.dashboard_partials.apply_to_act_for_section
import uk.gov.hmrc.auth.core.Admin
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.AgentInformation

class ApplyToActForSectionSpec
extends ViewBaseSpec {

  private implicit val langs: Seq[Lang] = Seq(Lang("en"))
  private val view = app.injector.instanceOf[apply_to_act_for_section]

  private def fakeRequestWithAgentInfo: AuthRequestWithAgentInfo[AnyContent] = {
    val fakeRequest = FakeRequest("GET", "/")

    val testAgentInformation = AgentInformation(
      agentId = None,
      agentCode = Some("ABC123"),
      agentFriendlyName = None
    )

    val agentInfo = AgentInfo(
      arn = Arn("TARN0000001"),
      enrolments = Enrolments(Set.empty),
      credentialRole = Some(Admin),
      email = Some("test@test.com"),
      name = None,
      credentials = None,
      agentInformation = testAgentInformation
    )

    new AuthRequestWithAgentInfo(agentInfo, fakeRequest)
  }
  private def doc(
    subs: Seq[SubscriptionInfo],
    isAbroad: Boolean
  ): Document = Jsoup.parse(
    view(subs, isAbroad)(
      messages,
      fakeRequestWithAgentInfo,
      appConfig
    ).body
  )

  private def sub(
    regime: LegacyRegime,
    status: SubscriptionStatus
  ) = SubscriptionInfo(
    regime = regime,
    subscriptionStatus = status,
    None
  )

  "apply_to_act_for_section" should {

    "show all 3 services for UK agent when none subscribed" in {
      val document = doc(
        Seq(
          sub(LegacyRegime.PAYE, SubscriptionStatus.NotSubscribed),
          sub(LegacyRegime.CT, SubscriptionStatus.NotSubscribed),
          sub(LegacyRegime.SA, SubscriptionStatus.NotSubscribed)
        ),
        isAbroad = false
      )

      val links = document.select("a").eachText()

      links must contain("Pay as you earn (PAYE)/Construction Industry Scheme (CIS)")
      links must contain("Corporation Tax")
      links must contain("Self Assessment")

      document.select("h2").text() mustBe messages("asa.apply-to-act-for.multi-service.section")
    }

    "exclude PAYE for overseas agent" in {
      val document = doc(
        Seq(
          sub(LegacyRegime.PAYE, SubscriptionStatus.NotSubscribed),
          sub(LegacyRegime.CT, SubscriptionStatus.NotSubscribed),
          sub(LegacyRegime.SA, SubscriptionStatus.NotSubscribed)
        ),
        isAbroad = true
      )

      val links = document.select("a").eachText()

      links must not contain "Pay as you earn (PAYE)/Construction Industry Scheme (CIS)"
      links must contain("Corporation Tax")
      links must contain("Self Assessment")
    }

    "show only remaining unsubscribed services" in {
      val document = doc(
        Seq(
          sub(LegacyRegime.PAYE, SubscriptionStatus.Subscribed),
          sub(LegacyRegime.CT, SubscriptionStatus.NotSubscribed),
          sub(LegacyRegime.SA, SubscriptionStatus.NotSubscribed)
        ),
        isAbroad = false
      )

      val links = document.select("a").eachText()

      links must not contain "Pay as you earn (PAYE)/Construction Industry Scheme (CIS)"
      links must contain("Corporation Tax")
      links must contain("Self Assessment")
    }

    "show single-service heading when only one service available" in {
      val document = doc(
        Seq(
          sub(LegacyRegime.PAYE, SubscriptionStatus.Subscribed),
          sub(LegacyRegime.CT, SubscriptionStatus.Subscribed),
          sub(LegacyRegime.SA, SubscriptionStatus.NotSubscribed)
        ),
        isAbroad = false
      )

      document.select("h2").text() mustBe messages("asa.apply-to-act-for.single-service.section")

      val links = document.select("a").eachText()
      links must contain only "Self Assessment"
    }

    "not render section when all services subscribed" in {
      val document = doc(
        Seq(
          sub(LegacyRegime.PAYE, SubscriptionStatus.Subscribed),
          sub(LegacyRegime.CT, SubscriptionStatus.Subscribed),
          sub(LegacyRegime.SA, SubscriptionStatus.Subscribed)
        ),
        isAbroad = false
      )

      document.select("#tax-services-h2").isEmpty mustBe true
    }
  }

}
