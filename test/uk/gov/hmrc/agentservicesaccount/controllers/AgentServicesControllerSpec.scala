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

      userGroupsPanel.select("a").get(0).text shouldBe "Other clients"
      userGroupsPanel.select("a").get(0).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/other-clients"

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
      grps.get(0).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/group-clients/grpId1"
      grps.get(1).text() shouldBe "Carrots"
      grps.get(1).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/group-clients/grpId2"
      userGroupsPanel.select("a").get(2).text shouldBe "Other clients"
      userGroupsPanel.select("a").get(2).attr("href") shouldBe s"$wireMockBaseUrlAsString/agent-permissions/your-account/other-clients"

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

  "GET help" should {

    "return Status: OK" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      status(response) shouldBe OK
    }

    "contain matching heading in page title" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      val html = Jsoup.parse(contentAsString(response))
      html.title() shouldBe "Help and guidance - Agent services account - GOV.UK"
      html.select(H1).get(0).text shouldBe "Help and guidance"
    }

    "contain body with correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showHelp().apply(fakeRequest("GET", "/help")))
      val html = Jsoup.parse(contentAsString(response))
      val h2 = html.select(H2)
      val h3 = html.select(H3)
      val p = html.select(paragraphs)
      val li = html.select(LI)
      val a = html.select(link)

      p.get(0).text shouldBe "This guidance is for tax agents and advisors. It describes how to use your agent services account."

      // Accordion tab1
      h2.get(0).text shouldBe "About this guidance"
      p.get(1).text shouldBe "This help and guidance is for tax agents and advisors. It covers how to use your agent services account to:"
      li.get(0).text shouldBe "copy VAT and Income Tax for Self Assessment authorisations from other agent accounts"
      li.get(1).text shouldBe "request and manage authorisations from clients"
      li.get(2).text shouldBe "manage tax services launched since 2019"
      li.get(3).text shouldBe "manage the tax affairs of your assigned clients"
      li.get(4).text shouldBe "view the details we hold about your organisation"
      p.get(2).text shouldBe "If you have administrator access, you can also:"
      li.get(5).text shouldBe "control which staff members can view and manage a client’s tax"
      li.get(6).text shouldBe "manage client references"

      // Accordion tab2
      h2.get(1).text shouldBe "About your agent services account"
      p.get(3).text shouldBe "You can only have one agent services account, but you can add several team members to this account. They will need to Government Gateway user IDs for you to add them."
      p.get(4).text shouldBe "The ‘Account home’ screen includes links for managing client authorisations and certain tax services. For other tax services, you will need to log into your online services for agents account."
      p.get(5).text shouldBe "The ‘Manage account’ screen will vary, depending on whether you have administrator or standard user access. Administrators can choose to manage access permissions using the new feature ‘access groups’. When access groups are turned on, standard users can only manage the clients they have been assigned to."

      // Accordion tab3
      h2.get(2).text shouldBe "Account home: client authorisations"
      p.get(6).text shouldBe "This section allows you to create, view and manage authorisation requests"
      p.get(7).text shouldBe "You can copy across authorisations for VAT and Income Tax for Self Assessment from your online services for agents accounts."
      p.get(8).text shouldBe "You can also create an authorisation request for a client, and view or manage your recent authorisation requests."
      p.get(9).text shouldBe "When asking a client for authorisation, you can send them the link to this guidance:"
      a.get(0).text shouldBe "https://www.gov.uk/guidance/authorise-an-agent-to-deal-with-certain-tax-services-for-you"
      a.get(0).attr("href") shouldBe "https://www.gov.uk/guidance/authorise-an-agent-to-deal-with-certain-tax-services-for-you"

      // Accordion tab4 - these links are in list items 7-20
      h2.get(3).text shouldBe "Account home: tax services"

      h3.get(0).text shouldBe "VAT"
      a.get(1).text shouldBe "Making Tax Digital for VAT as an agent: step by step"
      a.get(1).attr("href") shouldBe "https://www.gov.uk/guidance/making-tax-digital-for-vat-as-an-agent-step-by-step"
      a.get(2).text shouldBe "How to keep digital records and file returns for Making Tax Digital for VAT"
      a.get(2).attr("href") shouldBe "https://www.gov.uk/government/publications/vat-notice-70022-making-tax-digital-for-vat/vat-notice-70022-making-tax-digital-for-vat"
      a.get(3).text shouldBe "How to sign clients up for Making Tax Digital for VAT"
      a.get(3).attr("href") shouldBe "https://www.gov.uk/guidance/sign-up-your-client-for-making-tax-digital-for-vat"
      a.get(4).text shouldBe "Help and support for Making Tax Digital (videos and webinars)"
      a.get(4).attr("href") shouldBe "https://www.gov.uk/guidance/help-and-support-for-making-tax-digital#creating-an-agent-services-account"

      h3.get(1).text shouldBe "Trusts and estates"
      a.get(5).text shouldBe "How to register your client’s estate"
      a.get(5).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-estate"
      a.get(6).text shouldBe "How to register your client’s trust"
      a.get(6).attr("href") shouldBe "https://www.gov.uk/guidance/register-your-clients-trust"

      h3.get(2).text shouldBe "Capital Gains Tax on UK property"
      a.get(7).text shouldBe "How to ask for authorisation, manage your client’s account and send returns as an agent"
      a.get(7).attr("href") shouldBe "https://www.gov.uk/guidance/managing-your-clients-capital-gains-tax-on-uk-property-account"

      h3.get(3).text shouldBe "Plastic Packaging Tax"
      a.get(8).text shouldBe "Check if your client needs to register for Plastic Packaging Tax"
      a.get(8).attr("href") shouldBe "https://www.gov.uk/guidance/check-if-you-need-to-register-for-plastic-packaging-tax"
      a.get(9).text shouldBe "Register for the next live webinar"
      a.get(9).attr("href") shouldBe "https://www.gov.uk/guidance/help-and-support-for-agents#plastic-packaging-tax"

      h3.get(4).text shouldBe "Tax services you cannot manage in this account"
      a.get(10).text shouldBe "Self Assessment"
      a.get(10).attr("href") shouldBe "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"
      a.get(11).text shouldBe "VAT (not including Making Tax Digital for VAT)"
      a.get(11).attr("href") shouldBe "https://www.gov.uk/guidance/vat-online-services-for-agents"
      a.get(12).text shouldBe "Corporation Tax"
      a.get(12).attr("href") shouldBe "https://www.gov.uk/guidance/corporation-tax-for-agents-online-service"
      a.get(13).text shouldBe "Authorising an agent for other tax services"
      a.get(13).attr("href") shouldBe "https://www.gov.uk/guidance/client-authorisation-an-overview"
      a.get(14).text shouldBe "PAYE for Agents"
      a.get(14).attr("href") shouldBe "https://www.gov.uk/guidance/payecis-for-agents-online-service"
      a.get(15).text shouldBe "How to change or cancel authorisations as an agent"
      a.get(15).attr("href") shouldBe "https://www.gov.uk/guidance/client-authorisation-an-overview#how-to-change-or-cancel-authorisations-as-an-agent"

      // Accordion tab5
      h2.get(4).text shouldBe "Manage account: standard users"
      p.get(10).text shouldBe "Standard users cannot make any changes to access groups. If access groups are turned off, they can manage the tax of all their organisation's clients. When access groups are turned on, they can only:"
      li.get(21).text shouldBe "view the access groups they are assigned to"
      li.get(22).text shouldBe "view and manage clients they are assigned to through these access groups"
      p.get(11).text shouldBe "Whether access groups are on or off, they can also:"
      li.get(23).text shouldBe "view details of the people in the company with administrator access"
      li.get(24).text shouldBe "view the details we hold about their company"
      p.get(12).text shouldBe "If you are a standard user and you need to access more information, contact someone in your company with administrator access."

      // Accordion tab6
      h2.get(5).text shouldBe "Manage account: administrators"
      p.get(13).text shouldBe "Team members with administrator access can:"
      li.get(25).text shouldBe "see all client, team member and access group details"
      li.get(26).text shouldBe "turn access groups on and off"
      li.get(27).text shouldBe "create, edit and delete access groups"
      li.get(28).text shouldBe "assign clients and team members to access groups"
      li.get(29).text shouldBe "create or change client references within this service"
      li.get(30).text shouldBe "view the details we hold about their organisation"
      h3.get(5).text shouldBe "Manage access permissions with access groups new"
      p.get(14).text shouldBe "Access groups allow you to manage access permissions for your team members within your agent services account."
      p.get(15).text shouldBe "By default, all your team members can manage all your clients’ tax affairs. You may want to limit who can manage a specific client’s tax. If so, turn on access groups."
      p.get(16).text shouldBe "Access groups include team members and clients. If a client in access groups, only the team members in those groups manage their tax. You can change the clients and team members in a group at any time"
      p.get(17).text shouldBe "You do not need to assign all your clients to groups. If you do not add a client to any access groups, any staff member can manage their tax."
      p.get(18).text shouldBe "To use access groups your agent services account needs to include:"
      li.get(31).text shouldBe "more than one team member"
      li.get(32).text shouldBe "between 2 and 1,000 clients (inclusive)"
      p.get(19).text shouldBe "HMRC are looking into making access groups available to larger agent firms."
      p.get(20).text shouldBe "When you turn access groups on, it may take some time for our service to gather all your client details. If this happens then you will receive an email when the processing is done."
      p.get(21).text shouldBe "If you turn access groups off then all your team members will be able to manage all your clients’ tax again. The service will remember your groups, so you can restore them by turning access groups on again."
      p.get(22).text shouldBe "Access groups do not work with Income Record Viewer at present. HMRC is looking into this."
      h3.get(6).text shouldBe "Manage team members"
      p.get(23).text shouldBe "You cannot add team members to your account within this service. If you select 'Add or remove team members' then the required service will open in a new tab."
      h3.get(7).text shouldBe "Manage clients"
      p.get(24).text shouldBe "You can manage the client’s reference within access groups. This will not affect their details in other Government Gateway services."

    }

  }
}
