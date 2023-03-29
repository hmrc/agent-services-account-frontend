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

package uk.gov.hmrc.agentservicesaccount.controllers


import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.support.Css.paragraphs

import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec}

import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}






class ManageAgentServicesAccountLandingSpec extends BaseISpec {


  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)



  val controller: ManageAgentServicesAccountLanding = app.injector.instanceOf[ManageAgentServicesAccountLanding]

  "showAccessGroupSummaryForASA" should  {

    val ASAAccountTitle = "Manage access in the agent services account - Agent services account - GOV.UK"

    "return Status: OK containing correct content" in {


      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showAccessGroupSummaryForASA(FakeRequest("GET", "/agent-services-access").withSession(SessionKeys.authToken -> "Bearer XYZ"))) //URL response created to mock webpage


      status(response) shouldBe OK // I except response to be OK(200)

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe ASAAccountTitle


    }

    "return Status: Unauthorized containing correct content" in {


      givenAuthorisedAsAgentWith(arn, false)
      val response = await(controller.showAccessGroupSummaryForASA(FakeRequest("GET", "/agent-services-access").withSession(SessionKeys.authToken -> "Bearer XYZ"))) //URL response created to mock webpage


      status(response) shouldBe 401 // I except response to be Unauthorized(401)

      val html = Jsoup.parse(contentAsString(response))

      html.title() shouldBe ""


    }

    "return page correct content" in {

     // This page is showing me when access groups are turned off

      givenAuthorisedAsAgentWith(arn)
      val response = await(controller.showAccessGroupSummaryForASA()(FakeRequest("GET", "/agent-services-access").withSession(SessionKeys.authToken -> "Bearer XYZ")))

      val html = Jsoup.parse(contentAsString(response))
      val p = html.select(paragraphs)

      p.get(0).text shouldBe "For this tax service, use access groups to control which team members can manage this client. This is done in your agent services account."
      p.get(1).text shouldBe "This tax service is managed through your agent services account. Access permissions for the agent services account work differently from other HMRC online services."
      p.get(2).text shouldBe "By default, all your team members have access to all your clients. You can restrict access to a clients taxes using access groups."
      p.get(3).text shouldBe "To find out more, select Turn on access groups on the Manage account page. Youll be shown more information. You can then choose to turn access groups on or leave them off."

    }

  }

}
