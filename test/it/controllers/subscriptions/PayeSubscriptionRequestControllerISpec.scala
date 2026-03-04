// TODO: THIS NEEDS TO BE REWRITTEN

///*
// * Copyright 2026 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package it.controllers.subscriptions
//
//import play.api.Application
//import play.api.inject.bind
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.test.Helpers._
//import stubs.AgentAssuranceStubs.givenAgentRecordFound
//import stubs.AgentServicesAccountStubs.givenSubscriptionInfoResponse
//import support.ComponentBaseISpec
//import uk.gov.hmrc.agentservicesaccount.connectors.PayeSubscriptionConnector
//import uk.gov.hmrc.agentservicesaccount.models.paye._
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class PayeSubscriptionRequestControllerISpec extends ComponentBaseISpec {
//
//  private val confirmPath   = "/paye-subscription/request/confirm"
//  private val submittedPath = "/paye-subscription/request/submitted"
//
//  private object TestState {
//    val cyaData: PayeCyaData =
//      PayeCyaData(
//        agentName = "Example Agent Ltd",
//        contactName = "Jane Agent",
//        telephoneNumber = Some("01632 960 001"),
//        emailAddress = Some("jane.agent@example.com"),
//        address = PayeAddress(
//          line1 = "1 High Street",
//          line2 = "Village",
//          line3 = Some("County"),
//          line4 = None,
//          postCode = "AA1 1AA"
//        )
//      )
//  }
//
//  private final class TestPayeSubscriptionConnector extends PayeSubscriptionConnector {
//    override def getCyaData()(implicit ec: ExecutionContext): Future[PayeCyaData] = Future.successful(TestState.cyaData)
//
//    override def submitRequest()(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())
//  }
//
//  override def extraConfig(): Map[String, String] = super.extraConfig()
//
//  override lazy val app: Application =
//    new GuiceApplicationBuilder()
//      .configure(config ++ extraConfig() ++ downstreamServices)
//      .overrides(
//        bind[PayeSubscriptionConnector].toInstance(new TestPayeSubscriptionConnector)
//      )
//      .build()
//
//  s"GET $confirmPath" should {
//
//    "return OK and render the confirmation screen when eligible" in {
//      givenAuthorisedAsAgentWith(arn.value)
//      givenAgentRecordFound(agentRecord)
//      givenSubscriptionInfoResponse()
//
//      val result = get(confirmPath)
//
//      result.status shouldBe OK
////      TODO: 10593 Better assertions in here
//      assertPageHasTitle("Check your details before requesting a PAYE subscription")(result)
//
//      result.body should include("Example Agent Ltd")
//      result.body should include("Jane Agent")
//      result.body should include("Return to your agent services account")
//    }
//  }
//
//  s"POST $confirmPath" should {
//
//    "redirect to submitted screen when eligible" in {
//      givenAuthorisedAsAgentWith(arn.value)
//      givenAgentRecordFound(agentRecord)
//      givenSubscriptionInfoResponse()
//
//      val result = post(confirmPath)(Map.empty)
//
//      result.status shouldBe SEE_OTHER
//      //      TODO: 10593 Better assertions in here
//      result.header("Location").value shouldBe "/agent-services-account/paye-subscription/request/submitted"
//    }
//  }
//
//  s"GET $submittedPath" should {
//
//    "return OK and render submitted screen" in {
//      givenAuthorisedAsAgentWith(arn.value)
//      givenAgentRecordFound(agentRecord)
//      givenSubscriptionInfoResponse()
//
//      val result = get(submittedPath)
//
//      result.status shouldBe OK
//      //      TODO: 10593 Better assertions in here
//      assertPageHasTitle("PAYE subscription request submitted")(result)
//      result.body should include("Return to your agent services account")
//    }
//  }
//}
