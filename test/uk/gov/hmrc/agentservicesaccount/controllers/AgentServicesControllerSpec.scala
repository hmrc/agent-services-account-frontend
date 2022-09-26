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


import org.apache.commons.lang3.RandomUtils
import org.jsoup.Jsoup
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Session
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, OptedInNotReady, OptedInReady, OptedInSingleUser, OptedOutEligible, OptedOutSingleUser, OptedOutWrongClientCount, SuspensionDetails, SuspensionDetailsNotFound, UserDetails}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{AccessGroupSummaries, AccessGroupSummary, AgencyDetails, BusinessAddress, GroupSummary}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentFiRelationshipStubs.{givenArnIsAllowlistedForIrv, givenArnIsNotAllowlistedForIrv}
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentUserClientDetailsStubs._
import uk.gov.hmrc.agentservicesaccount.support.Css._
import uk.gov.hmrc.http.SessionKeys


class AgentServicesControllerSpec extends BaseISpec {

  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: AgentServicesController = app.injector.instanceOf[AgentServicesController]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  private def fakeRequest(method: String = "GET", uri: String = "/") = FakeRequest(method, uri).withSession(SessionKeys.authToken -> "Bearer XYZ")

  "root" should {
    "redirect to agent services account when suspension is disabled" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      val controllerWithSuspensionDisabled =
        appBuilder(Map("features.enable-agent-suspension" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithSuspensionDisabled.root()(fakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to agent service account when suspension is enabled but user is not suspended" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.root()(fakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to suspended warning when suspension is enabled and user is suspended" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))

      val response = controller.root()(fakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
    }

    "redirect to suspended warning when suspension is enabled and user is suspended for AGSV" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      val response = controller.root()(fakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
      Helpers.session(response) shouldBe Session(Map("authToken" -> "Bearer XYZ", "suspendedServices" -> "CGT,ITSA,PIR,PPT,TRS,VATC", "isSuspendedForVat" -> "true"))
    }

    "redirect to suspended warning when suspension is enabled and user is suspended for ALL" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ALL"))))

      val response = controller.root()(fakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      Helpers.redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
      Helpers.session(response) shouldBe Session(Map("authToken" -> "Bearer XYZ", "suspendedServices" -> "CGT,ITSA,PIR,PPT,TRS,VATC", "isSuspendedForVat" -> "true"))
    }

    "throw an exception when suspension is enabled and suspension status returns NOT_FOUND for user" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenAgentRecordNotFound


      intercept[SuspensionDetailsNotFound]{
        await(controller.root()(fakeRequest("GET", "/")))
      }
    }
  }

  "home" should {
    "return Status: OK and body containing correct content" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      val controllerWithSuspensionDisabled =
        appBuilder(Map("features.enable-agent-suspension" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controllerWithSuspensionDisabled.showAgentServicesAccount()(fakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      val html = Jsoup.parse(content)
      html.select("div.govuk-phase-banner").isEmpty() shouldBe false
      html.select("div.govuk-phase-banner a").text shouldBe "feedback"
      html.select("div.govuk-phase-banner a").attr("href") shouldBe "http://localhost:9250/contact/beta-feedback?service=AOSS"

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
      content should include(messagesApi("agent.accountNumber","TARN 000 0001"))
      content should include(messagesApi("agent.services.account.sectionITSA.h2"))
      content should include(messagesApi("agent.services.account.sectionITSA.col1.h3"))
      content should include(messagesApi("agent.services.account.sectionITSA.col1.p", appConfig.agentMappingUrl, appConfig.agentInvitationsFrontendClientTypeUrl))
      content should include(messagesApi("agent.services.account.sectionITSA.col1.link"))
      content should include(messagesApi("agent.services.account.sectionITSA.col2.h3"))
      content should include(appConfig.incomeTaxSubscriptionAgentFrontendUrl)
      content should include("https://www.gov.uk/guidance/follow-the-rules-for-making-tax-digital-for-income-tax#who-can-follow-the-rules")
      content should include(messagesApi("agent.services.account.sectionITSA.col2.link1.text"))
      content should include(messagesApi("agent.services.account.sectionITSA.col2.link2.text"))
      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.col1.h3"))
      content should include(messagesApi("agent.services.account.section1.col1.p", appConfig.agentMappingUrl, appConfig.agentInvitationsFrontendClientTypeUrl))
      content should include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
      content should include(htmlEscapedMessage("agent.services.account.section1.col2.h3"))
      content should include(htmlEscapedMessage("agent.services.account.section1.col2.link"))
      content should include(messagesApi("agent.services.account.section3.col1.h2"))
      content should include(messagesApi("agent.services.account.section3.col1.h3"))
      content should include(messagesApi("agent.services.account.section3.col1.text1"))
      content should include(messagesApi("agent.services.account.section3.col1.link1", messagesApi("agent.services.account.section3.col1.link1.href")))
      content should include(messagesApi("agent.services.account.section3.col1.text2"))
      content should include(messagesApi("agent.services.account.section3.col1.link2", messagesApi("agent.services.account.section3.col1.link2.href")))
      content should include(messagesApi("agent.services.account.section3.col2.h3"))
      content should include(messagesApi("agent.services.account.section3.col2.link1", messagesApi("agent.services.account.section3.col2.link1.href")))
      content should include(messagesApi("agent.services.account.section4.h2"))
      content should include(messagesApi("agent.services.account.section4.col1.h3"))
      content should include(messagesApi("agent.services.account.section4.col1.link"))
      content should include(appConfig.agentInvitationsFrontendUrl)
      content should include(appConfig.agentInvitationsFrontendClientTypeUrl)
      content should include(messagesApi("agent.services.account.section4.col2.link1"))
      content should include(messagesApi("agent.services.account.section4.col2.link2"))
      content should include(htmlEscapedMessage("agent.services.account.section4.col2.link3"))
      content should include(appConfig.agentInvitationsTrackUrl)
      content should include(appConfig.agentMappingUrl)
      content should include(appConfig.agentInvitationsCancelAuthUrl)
      content should include(messagesApi("agent.services.account.trusts-section.h2"))
      content should include(messagesApi("agent.services.account.trusts-section.col1.h3"))
      content should include(messagesApi("agent.services.account.trusts-section.col1.register-trust.p", appConfig.agentInvitationsFrontendClientTypeUrl))
      content should include(messagesApi("agent.services.account.trusts-section.col1.register-estate.p"))
      content should include(messagesApi("agent.services.account.trusts-section.col2.h3"))
      content should include(messagesApi("agent.services.account.trusts-section.col2.register-trust-link.text"))
      content should include(messagesApi("agent.services.account.welcome"))
      content should include(messagesApi("agent.services.account.client-authorisations.p"))
      content should include(messagesApi("agent.services.account.tax-services"))
      content should include(messagesApi("asa.other.heading"))
      content should include(messagesApi("asa.other.p1"))
      content should include(messagesApi("nav.help"))
    }

    "return Status: OK and body containing correct content when suspension details are in the session and agent is suspended for VATC" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("VATC"))))
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showAgentServicesAccount()(fakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.suspended.h3"))
      content should include(messagesApi("agent.services.account.section1.suspended.p1"))
      content should include(messagesApi("agent.services.account.section1.suspended.p2"))

      content should not include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
    }

    "return Status: OK and body containing correct content when agent suspension is not enabled" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      val controllerWithSuspensionDisabled =
        appBuilder(Map("features.enable-agent-suspension" -> false))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithSuspensionDisabled.showAgentServicesAccount()(fakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
    }

    "do not fail without continue url parameter" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.showAgentServicesAccount().apply(fakeRequest("GET", "/home"))
      status(response) shouldBe OK
      Helpers.contentAsString(response) should {
        not include "<a href=\"/\" class=\"btn button\" id=\"continue\">"
      }
    }

    "include the Income Record Viewer section when the IRV allowlist is enabled and the ARN is allowed " in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      val controller =
        appBuilder(Map("features.enable-agent-suspension" -> false, "features.enable-irv-allowlist" -> true))
          .build()
          .injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)

      val result = controller.showAgentServicesAccount(fakeRequest())
      status(result) shouldBe OK

      val content = Helpers.contentAsString(result)
      content should include (messagesApi("agent.services.account.paye-section.h2"))
    }

    "not include the Income Record Viewer section when the IRV allowlist is enabled and the ARN is not allowed" in {
      givenArnIsNotAllowlistedForIrv(Arn(arn))
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))
      givenAuthorisedAsAgentWith(arn)

      val controller = appBuilder().build().injector.instanceOf[AgentServicesController]

      val result = controller.showAgentServicesAccount(fakeRequest())
      status(result) shouldBe OK

      val content = Helpers.contentAsString(result)
      content should not include messagesApi("agent.services.account.paye-section.h2")
    }

  }

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showSuspendedWarning()(fakeRequest("GET", "/home").withSession("suspendedServices" -> "HMRC-MTD-IT,HMRC-MTD-VAT"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("suspension-warning.header"))
      content should include(messagesApi("suspension-warning.p1"))
      content should include(messagesApi("suspension-warning.p2.multi"))
      content should include(messagesApi("suspension-warning.multi.HMRC-MTD-IT"))
      content should include(messagesApi("suspension-warning.multi.HMRC-MTD-VAT"))
      content should include(messagesApi("suspension-warning.p3"))
      content should include(htmlEscapedMessage("suspension-warning.p4"))
      content should include(messagesApi("suspension-warning.button"))
      val getHelpLink = Jsoup.parse(content).select(Css.getHelpWithThisPageLink)
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2Fhome"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
    }
  }

  "manage-account" should {

    val manageAccountTitle = "Manage account - Agent services account - GOV.UK"

    "return Status: OK and body containing correct content when gran perms FF disabled" in {

          val controllerWithGranPermsDisabled =
            appBuilder(Map("features.enable-gran-perms" -> false))
              .build()
              .injector.instanceOf[AgentServicesController]

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      val response = controllerWithGranPermsDisabled.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but there was an error getting optin-status" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdFailure(Arn(arn))
      givenOptinStatusFailedForArn(Arn(arn))
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but ARN is not on allowed list" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedNotOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.h1"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return status: OK and body containing content for status Opted-In_READY (no access groups created yet)" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty)) // no access groups yet
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val li = html.select(Css.LI)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      h3.get(0).text shouldBe "Status Turned on"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")
      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).hasClass("govuk-button") shouldBe true
      li.get(0).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/group/create-access-group"
      li.get(1).child(0).text shouldBe "Manage access groups"
      li.get(1).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-access-groups"
      li.get(2).child(0).text shouldBe "Turn off access groups"
      li.get(2).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/turn-off-guide"
      h2.get(1).text shouldBe "Clients"
      li.get(3).child(0).text shouldBe "Manage clients"
      li.get(4).child(0).text shouldBe "Unassigned clients"
      h2.get(2).text shouldBe "Team members"
      li.get(5).child(0).text shouldBe "Manage team members"
      li.get(5).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-team-members"
      li.get(6).child(0).text shouldBe "Add or remove team members (opens in a new tab)"
      li.get(6).child(0).attr("href") shouldBe "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"
      h2.get(3).text shouldBe "Contact details"
      paragraphs.get(3).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(3).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-In_READY (access groups already created)" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq(AccessGroupSummary("myAccessGroupId")))) // there is already an access group
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val li = html.select(Css.LI)
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      h3.get(0).text shouldBe "Status Turned on"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")

      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).hasClass("govuk-button") shouldBe false

      h2.get(1).text shouldBe "Clients"

      paragraphs.get(1).text shouldBe "View client details, update client reference and see what groups a client is in."
      li.get(3).child(0).text shouldBe "Manage clients"
      li.get(3).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-clients"
      li.get(4).child(0).text shouldBe "Unassigned clients"
      li.get(4).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/unassigned-clients"

      // TODO - should move these into method checks eg. pageWithTeamMembersSectionContent(html)
      h2.get(2).text shouldBe "Team members"
      paragraphs.get(2).text shouldBe "View team member details and see what groups a team member is in."
      li.get(5).child(0).text shouldBe "Manage team members"
      li.get(5).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-team-members"
      li.get(6).child(0).text shouldBe "Add or remove team members (opens in a new tab)"
      li.get(6).child(0).attr("href") shouldBe "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"

    }

    "return status: OK and body containing content for status Opted-In_NOT_READY" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInNotReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val li = html.select(Css.LI)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")
      html.select(Css.insetText).get(0).text
        .shouldBe("You have added new clients but need to wait until your client details are ready to use with access groups. You will receive a confirmation email after which you can start using access groups.")

      h3.get(0).text shouldBe "Status Turned on"
      html.select("p#config-link a").text shouldBe "Turn off access groups"
      html.select("p#config-link a").attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/turn-off-guide"

      h2.get(1).text shouldBe "Team members"
      paragraphs.get(2).text shouldBe "View team member details and see what groups a team member is in."
      li.get(0).child(0).text shouldBe "Manage team members"
      li.get(0).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-team-members"
      li.get(1).child(0).text shouldBe "Add or remove team members (opens in a new tab)"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"

      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(3).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(3).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-In_SINGLE_USER" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInSingleUser)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      h3.get(0).text shouldBe "Status Turned on"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")
      html.select(Css.insetText).get(0).text
        .shouldBe("To use access groups you need to add more team members to your agent services account under ‘Manage team members’ below.")
      h2.get(1).text shouldBe "Team members"
      paragraphs.get(1).text shouldBe "View team member details and see what groups a team member is in."
      li.get(0).child(0).text shouldBe "Manage team members"
      li.get(0).child(0).attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/manage-team-members"
      li.get(1).child(0).text shouldBe "Add or remove team members (opens in a new tab)"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(2).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(2).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-Out_WRONG_CLIENT_COUNT" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutWrongClientCount)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")
      html.select(Css.insetText).get(0).text
        .shouldBe("To use access groups you need to have more than 1 client in your agent services account.")

      h3.get(0).text shouldBe "Status Turned off"

      h2.get(1).text shouldBe "Team members"
      html.select("p#manage-team-members a").text() shouldBe "Add or remove team members (opens in a new tab)"
      html.select("p#manage-team-members a").attr("href") shouldBe "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"

      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(2).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(2).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-Out_SINGLE_USER" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutSingleUser)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      h3.get(0).text shouldBe "Status Turned off"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")
      html.select(Css.insetText).get(0).text
        .shouldBe("To use access groups you need to add more team members to your agent services account under ‘Manage team members’ below.")

      h2.get(1).text shouldBe "Team members"
      html.select("p#manage-team-members a").text() shouldBe "Add or remove team members (opens in a new tab)"
      html.select("p#manage-team-members a").attr("href") shouldBe "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"

      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(2).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(2).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-Out_ELIGIBLE" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutEligible)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty))
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)

      html.title() shouldBe manageAccountTitle
      html.select(H1).get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access groups"
      paragraphs.get(0).text
        .shouldBe("Access groups allow you to control which team members can view and manage each client’s tax affairs.")

      h3.get(0).text shouldBe "Status Turned off"
      html.select("p#config-link a").text shouldBe "Turn on access groups"
      html.select("p#config-link a").attr("href") shouldBe s"http://localhost:$wireMockPort/agent-permissions/turn-on-guide"

      h2.get(1).text shouldBe "Team members"
      html.select("p#manage-team-members a").text shouldBe "Add or remove team members (opens in a new tab)"
      html.select("p#manage-team-members a").attr("href") shouldBe
        "http://localhost:1111/user-profile-redirect-frontend/group-profile-management"

      val contactDetails = html.select("div#contact-details")
      contactDetails.select("h2").text shouldBe "Contact details"
      contactDetails.select("p a").text shouldBe "View the contact details we have for your business"
      contactDetails.select("p a").attr("href") shouldBe "/agent-services-account/account-details"

    }
  }

  "account-details" should {

    "return Status: OK and body containing correct content" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenAgentDetailsFound(
        AgencyDetails(
          Some("My Agency"),
          Some("abc@abc.com"),
            Some(BusinessAddress("25 Any Street", Some("Any Town"), None, None, Some("TF3 4TR"), "GB"))))

      val response = controller.accountDetails().apply(fakeRequest("GET", "/account-details"))

      Helpers.status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("account-details.title"))
      content should include(messagesApi("account-details.summary-list.header"))
      content should include(messagesApi("account-details.summary-list.email"))
      content should include(messagesApi("account-details.summary-list.name"))
      content should include(messagesApi("account-details.summary-list.address"))
      content should include("My Agency")
      content should include("abc@abc.com")
      content should include("25 Any Street")
      content should include("Any Town")
      content should include("TF3 4TR")
      content should include("GB")
      content should include(" To change these details you will need to write to us. <a class=\"govuk-link\" href=https://www.gov.uk/guidance/change-or-remove-your-authorisations-as-a-tax-agent#changes-you-can-make-in-writing target=\"_blank\" rel=\"noreferrer noopener\">Find out more by reading the guidance (opens in new tab)</a>. You can only change your details if you are a director, company secretary, sole trader, proprietor or partner.")

    }

    "return Status: OK and body containing None in place of missing agency details" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenAgentDetailsNoContent()

      val response = controller.accountDetails().apply(fakeRequest("GET", "/account-details"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("account-details.title"))
      content should include(messagesApi("account-details.summary-list.header"))
      content should include(messagesApi("account-details.summary-list.email"))
      content should include(messagesApi("account-details.summary-list.name"))
      content should include(messagesApi("account-details.summary-list.address"))
      content should include(messagesApi("account-details.summary-list.none"))
      content should include(" To change these details you will need to write to us. <a class=\"govuk-link\" href=https://www.gov.uk/guidance/change-or-remove-your-authorisations-as-a-tax-agent#changes-you-can-make-in-writing target=\"_blank\" rel=\"noreferrer noopener\">Find out more by reading the guidance (opens in new tab)</a>. You can only change your details if you are a director, company secretary, sole trader, proprietor or partner.")

      }

    "return Forbidden if the agent is not Admin" in {
      givenAuthorisedAsAgentWith(arn, isAdmin = false)

      val response = controller.accountDetails().apply(fakeRequest("GET", "/account-details"))
      status(response) shouldBe 403
    }
  }

  val yourAccountUrl: String = routes.AgentServicesController.yourAccount.url

  s"GET on Your Account at url: $yourAccountUrl" should {

    "render correctly for Standard User who's Opted-In_READY without Access Groups" in {
      val providerId = RandomUtils.nextLong().toString
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenFullAuthorisedAsAgentWith(arn, providerId)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq.empty)) // no access groups yet
      givenAccessGroupsForTeamMember(Arn(arn), providerId, Seq.empty)
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      
      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"

      //LEFT PANEL
      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select("h3").get(0).text() shouldBe "Name"
      userDetailsPanel.select("p").get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select("h3").get(1).text() shouldBe "Email address"
      userDetailsPanel.select("p").get(1).text() shouldBe "bob@builder.com"
      userDetailsPanel.select("h3").get(2).text() shouldBe "Role"
      userDetailsPanel.select("p").get(2)
        .text() shouldBe "Standard - As a user with standard permissions you can view your assigned access groups and clients. Please contact your administrator for more information."

      //RIGHT PANEL
      val userGroupsPanel = html.select("div#user-groups")
      val grps = userGroupsPanel.select("ul li a")
      grps.isEmpty shouldBe true
      userGroupsPanel.select("p").get(0).text shouldBe "You are not currently assigned to any groups"

      //BOTTOM PANEL
      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your company’s administrators"
      bottomPanel.select("a").get(0).text shouldBe "View administrators"
      bottomPanel.select("a").get(0).attr("href") shouldBe routes.AgentServicesController.administrators.url
      bottomPanel.select("h2").get(1).text shouldBe "Contact details"
      bottomPanel.select("a").get(1).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.accountDetails.url

    }

    "return status: OK and body containing content for status Opted-In_READY (access groups already created)" in {
      val providerId = RandomUtils.nextLong().toString
      val groupSummaries: Seq[GroupSummary] = Seq(
        GroupSummary("grpId1", "Potatoes", 1, 1),
        GroupSummary("grpId2", "Carrots", 1, 1),
      )
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenFullAuthorisedAsAgentWith(arn, providerId)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      givenAccessGroupsForArn(Arn(arn), AccessGroupSummaries(Seq(AccessGroupSummary("myAccessGroupId")))) // there is already an access group
      givenAccessGroupsForTeamMember(Arn(arn), providerId, groupSummaries)
      val response = await(controller.yourAccount()(fakeRequest("GET", yourAccountUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Your account - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Your account"
      //LEFT PANEL
      val userDetailsPanel = html.select("div#user-details")
      userDetailsPanel.select("h3").get(0).text() shouldBe "Name"
      userDetailsPanel.select("p").get(0).text() shouldBe "Bob The Builder"
      userDetailsPanel.select("h3").get(1).text() shouldBe "Email address"
      userDetailsPanel.select("p").get(1).text() shouldBe "bob@builder.com"
      userDetailsPanel.select("h3").get(2).text() shouldBe "Role"
      userDetailsPanel.select("p").get(2)
        .text() shouldBe "Standard - As a user with standard permissions you can view your assigned access groups and clients. Please contact your administrator for more information."

      //RIGHT PANEL
      val userGroupsPanel = html.select("div#user-groups")
      val grps = userGroupsPanel.select("ul li a")
      grps.get(0).text() shouldBe "Potatoes"
      grps.get(0).attr("href") shouldBe "/agent-permissions/manage-clients/grpId1"
      grps.get(1).text() shouldBe "Carrots"
      grps.get(1).attr("href") shouldBe "/agent-permissions/manage-clients/grpId2"

      //BOTTOM PANEL
      val bottomPanel = html.select("div#bottom-panel")
      bottomPanel.select("h2").get(0).text shouldBe "Your company’s administrators"
      bottomPanel.select("a").get(0).text shouldBe "View administrators"
      bottomPanel.select("a").get(0).attr("href") shouldBe routes.AgentServicesController.administrators.url
      bottomPanel.select("h2").get(1).text shouldBe "Contact details"
      bottomPanel.select("a").get(1).text shouldBe "View the contact details we have for your business"
      bottomPanel.select("a").get(1).attr("href") shouldBe routes.AgentServicesController.accountDetails.url


    }
  }

  val adminUrl: String = routes.AgentServicesController.administrators.url

  s"GET on Administrators of your account at url: $adminUrl" should {

    "render static data and list of Admin Users for ARN" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenArnAllowedOk()
      givenSyncEacdSuccess(Arn(arn))
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      val teamMembers = Seq(
        UserDetails(credentialRole = Some("User"), name = Some("Robert Builder"), email = Some("bob@builder.com")),
        UserDetails(credentialRole = Some("User"), name = Some("Steve Smith"), email = Some("steve@builder.com")),
        //Assistant will be filtered out from the results we get back
        UserDetails(credentialRole = Some("Assistant"), name = Some("irrelevant")),
      )
      stubGetTeamMembersForArn(Arn(arn), teamMembers)
      val response = await(controller.administrators()(fakeRequest("GET", adminUrl)))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Administrators - Agent services account - GOV.UK"
      html.select(Css.H1).get(0).text shouldBe "Administrators"

      html.select(paragraphs).get(0).text shouldBe "Administrators can:"
      html.select(LI).get(0).text shouldBe "turn access groups on or off"
      html.select(LI).get(1).text shouldBe "view information about all clients and access groups"
      html.select(LI).get(2).text shouldBe "create, rename and delete access groups"
      html.select(LI).get(3).text shouldBe "assign clients and team members to access groups"
      val adminNames = html.select(summaryListKeys)
      val adminEmails = html.select(summaryListValues)
      adminNames.size() shouldBe 2
      adminNames.get(0).text shouldBe "Robert Builder"
      adminEmails.get(0).text shouldBe "bob@builder.com"
      adminNames.get(1).text shouldBe "Steve Smith"
      adminEmails.get(1).text shouldBe "steve@builder.com"
    }

  }

  "help" should {

    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))

      status(response) shouldBe OK

      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Help and guidance - Agent services account - GOV.UK"
      html.select(Css.H1).get(0).text shouldBe "Help and guidance"

      val h2 = html.select(H2)
      val h3 = html.select(Css.H3)
      val p = html.select(Css.paragraphs)

      p.get(0).text shouldBe "This guidance is for tax agents and advisors. It describes how to use your agent services account."

      // Accordion content - TODO fill out
      h2.get(0).text shouldBe "About this guidance"

      h2.get(1).text shouldBe "About your agent services account"

      h2.get(2).text shouldBe "Account home: client authorisations"

      h2.get(3).text shouldBe "Account home: tax services"
      h3.get(0).text shouldBe "VAT"
      h3.get(1).text shouldBe "Trusts and estates"

      h2.get(4).text shouldBe "Manage account: standard users"

      h2.get(5).text shouldBe "Manage account: administrators"


    }

  }
}
