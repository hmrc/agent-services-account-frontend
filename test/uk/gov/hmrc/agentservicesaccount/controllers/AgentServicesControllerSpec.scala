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


import org.jsoup.Jsoup
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Session
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, OptedInNotReady, OptedInReady, OptedInSingleUser, OptedOutEligible, OptedOutSingleUser, OptedOutWrongClientCount, SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentFiRelationshipStubs.{givenArnIsAllowlistedForIrv, givenArnIsNotAllowlistedForIrv}
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs._
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
      content should include(messagesApi("serviceinfo.help"))
    }

    "not show Help and Guidance link when toggled off" in {
      givenArnIsAllowlistedForIrv(Arn(arn))
      val controllerWithHelpToggledOff =
        appBuilder(Map("features.enable-help-and-guidance" -> false)).build().injector.instanceOf[AgentServicesController]
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controllerWithHelpToggledOff.showAgentServicesAccount()(fakeRequest("GET", "/home"))
      Helpers.contentAsString(response) should not include messagesApi("serviceinfo.help")
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
    }
  }

  "manage-account" should {

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
      content should include(messagesApi("manage.account.heading"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return Status: OK and body containing existing manage account content when gran perms FF is on but there was an error getting optin-status" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusFailedForArn(Arn(arn))
      val response = controller.manageAccount().apply(fakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("manage.account.heading"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))
      content should include(messagesApi("manage.account.account-details"))
    }

    "return status: OK and body containing content for status Opted-In_READY" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInReady)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val h1 = html.select(Css.H1)
      val h2 = html.select(Css.H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      h1.get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access permissions"
      h3.get(0).text shouldBe "Status OPTED-IN"
      paragraphs.get(0).text shouldBe "You are opted in and can now start creating access groups."
      h3.get(1).text shouldBe "Access groups"
      li.get(0).child(0).text shouldBe "Create new access group"
      li.get(0).child(0).attr("href") shouldBe "http://localhost:9452/agent-permissions/opt-in"
      li.get(1).child(0).text shouldBe "Manage access groups"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:9452/agent-permissions/manage-access-groups/start"
      h3.get(2).text shouldBe "Settings"
      li.get(2).child(0).text shouldBe "Opt out of using access groups"
      li.get(2).child(0).attr("href") shouldBe "http://localhost:9452/agent-permissions/opt-out"
      h2.get(1).text shouldBe "Make changes to your account"
      li.get(3).child(0).text shouldBe "Add or remove a team member"
      li.get(3).child(0).attr("href") shouldBe "http://localhost:hmmm/user-profile-redirect-frontend/group-profile-management"
      li.get(4).child(0).text shouldBe "Manage who can view your client list (opens in a new tab)"
      li.get(4).child(0).attr("href") shouldBe "http://localhost:hmmm/tax-and-scheme-management/users?origin=Agent"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(1).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(1).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-In_NOT_READY" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInNotReady)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val h1 = html.select(Css.H1)
      val h2 = html.select(Css.H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      h1.get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access permissions"
      h3.get(0).text shouldBe "Status OPTED-IN"
      paragraphs.get(0).text shouldBe "You have added new clients but need to wait until your client names are ready to use with access groups. You will receive a confirmation email after which you can start using access groups."
      h3.get(1).text shouldBe "Settings"
      li.get(0).child(0).text shouldBe "Opt out of using access groups"
      li.get(0).child(0).attr("href") shouldBe "http://localhost:9452/agent-permissions/opt-out"
      h2.get(1).text shouldBe "Make changes to your account"
      li.get(1).child(0).text shouldBe "Add or remove a team member"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:hmmm/user-profile-redirect-frontend/group-profile-management"
      li.get(2).child(0).text shouldBe "Manage who can view your client list (opens in a new tab)"
      li.get(2).child(0).attr("href") shouldBe "http://localhost:hmmm/tax-and-scheme-management/users?origin=Agent"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(1).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(1).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-In_SINGLE_USER" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedInSingleUser)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val h1 = html.select(Css.H1)
      val h2 = html.select(Css.H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      h1.get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access permissions"
      h3.get(0).text shouldBe "Status OPTED-IN"
      paragraphs.get(0).text shouldBe "To use access groups you need to add more team members to your agent services account."
      h2.get(1).text shouldBe "Make changes to your account"
      li.get(0).child(0).text shouldBe "Add or remove a team member"
      li.get(0).child(0).attr("href") shouldBe "http://localhost:hmmm/user-profile-redirect-frontend/group-profile-management"
      li.get(1).child(0).text shouldBe "Manage who can view your client list (opens in a new tab)"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:hmmm/tax-and-scheme-management/users?origin=Agent"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(1).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(1).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-Out_WRONG_CLIENT_COUNT" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutWrongClientCount)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val h1 = html.select(Css.H1)
      val h2 = html.select(Css.H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      h1.get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access permissions"
      h3.get(0).text shouldBe "Status OPTED-OUT"
      paragraphs.get(0).text shouldBe "To use access groups you need to have more than one client and fewer than 50 clients in your agent services account."
      h2.get(1).text shouldBe "Make changes to your account"
      li.get(0).child(0).text shouldBe "Add or remove a team member"
      li.get(0).child(0).attr("href") shouldBe "http://localhost:hmmm/user-profile-redirect-frontend/group-profile-management"
      li.get(1).child(0).text shouldBe "Manage who can view your client list (opens in a new tab)"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:hmmm/tax-and-scheme-management/users?origin=Agent"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(1).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(1).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-Out_SINGLE_USER" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutSingleUser)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val h1 = html.select(Css.H1)
      val h2 = html.select(Css.H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      h1.get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access permissions"
      h3.get(0).text shouldBe "Status OPTED-OUT"
      paragraphs.get(0).text shouldBe "To use access groups you need to add more team members to your agent services account."
      h2.get(1).text shouldBe "Make changes to your account"
      li.get(0).child(0).text shouldBe "Add or remove a team member"
      li.get(0).child(0).attr("href") shouldBe "http://localhost:hmmm/user-profile-redirect-frontend/group-profile-management"
      li.get(1).child(0).text shouldBe "Manage who can view your client list (opens in a new tab)"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:hmmm/tax-and-scheme-management/users?origin=Agent"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(1).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(1).child(0).attr("href") shouldBe "/agent-services-account/account-details"

    }

    "return status: OK and body containing content for status Opted-Out_ELIGIBLE" in {

      givenArnIsAllowlistedForIrv(Arn(arn))
      givenAuthorisedAsAgentWith(arn)
      givenOptinStatusSuccessReturnsForArn(Arn(arn), OptedOutEligible)
      val response = await(controller.manageAccount()(fakeRequest("GET", "/manage-account")))

      status(response) shouldBe 200

      val html = Jsoup.parse(contentAsString(response))
      val h1 = html.select(Css.H1)
      val h2 = html.select(Css.H2)
      val h3 = html.select(Css.H3)
      val paragraphs = html.select(Css.paragraphs)
      val li = html.select(Css.LI)

      h1.get(0).text shouldBe "Manage account"
      h2.get(0).text shouldBe "Manage access permissions"
      h3.get(0).text shouldBe "Status OPTED-OUT"
      h3.get(1).text shouldBe "Access groups"
      li.get(0).child(0).text shouldBe "Opt in to use access groups"
      li.get(0).child(0).attr("href") shouldBe "http://localhost:9452/agent-permissions/opt-in"
      h2.get(1).text shouldBe "Make changes to your account"
      li.get(1).child(0).text shouldBe "Add or remove a team member"
      li.get(1).child(0).attr("href") shouldBe "http://localhost:hmmm/user-profile-redirect-frontend/group-profile-management"
      li.get(2).child(0).text shouldBe "Manage who can view your client list (opens in a new tab)"
      li.get(2).child(0).attr("href") shouldBe "http://localhost:hmmm/tax-and-scheme-management/users?origin=Agent"
      h2.get(2).text shouldBe "Contact details"
      paragraphs.get(0).child(0).text shouldBe "View the contact details we have for your business"
      paragraphs.get(0).child(0).attr("href") shouldBe "/agent-services-account/account-details"

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

  "help" should {

    "return Status: OK and body containing correct content" in {
      givenAuthorisedAsAgentWith(arn)
      val response = controller.showHelp().apply(fakeRequest("GET", "/help"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("help.title"))
      content should include(messagesApi("help.heading"))
      content should include(messagesApi("help.p1"))
      content should include(messagesApi("help.authorised.h2"))
      content should include(messagesApi("help.authorised.link"))
      content should include(messagesApi("help.mtd.h2"))
      content should include(messagesApi("help.mtd.link1"))
      content should include(messagesApi("help.mtd.link2"))
      content should include(messagesApi("help.mtd.link3"))
      content should include(messagesApi("help.mtd.link4"))
      content should include(messagesApi("help.mtd.link5"))
      content should include(messagesApi("help.trusts.h2"))
      content should include(messagesApi("help.trusts.link1"))
      content should include(messagesApi("help.trusts.link2"))
      content should include(messagesApi("help.cgt.h2"))
      content should include(messagesApi("help.cgt.link"))
      content should include(messagesApi("help.cannot.h2"))
      content should include(messagesApi("help.cannot.link1"))
      content should include(messagesApi("help.cannot.link2"))
      content should include(messagesApi("help.cannot.link3"))
      content should include(messagesApi("help.cannot.link4"))
      content should include(messagesApi("help.cannot.link5"))
      content should include(messagesApi("help.cannot.link6"))
    }
  }
}
