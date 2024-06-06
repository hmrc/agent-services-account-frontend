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

package uk.gov.hmrc.agentservicesaccount.stubs


import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CONFLICT, CREATED, NOT_FOUND, NO_CONTENT}
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.GroupSummary
import uk.gov.hmrc.agents.accessgroups.optin.OptinStatus
import uk.gov.hmrc.agentservicesaccount.models.AccessGroupSummaries

object AgentPermissionsStubs {

  def givenSyncEacdSuccess(arn: Arn): StubMapping =
    stubFor(
      post(urlEqualTo(s"/agent-permissions/arn/${arn.value}/sync?fullSync=true"))
        .willReturn(aResponse()
          .withStatus(200)))

  def givenSyncEacdFailure(arn: Arn): StubMapping =
    stubFor(
      patch(urlEqualTo(s"/agent-permissions/arn/${arn.value}/sync"))
        .willReturn(serverError()))

  def givenArnAllowedOk(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn-allowed"))
        .willReturn(aResponse()
          .withStatus(200)))

  def givenArnAllowedNotOk(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn-allowed"))
        .willReturn(aResponse()
          .withStatus(403)))

  def givenHidePrivateBetaInvite(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/private-beta-invite"))
        .willReturn(aResponse()
          .withStatus(200)))

  def givenHidePrivateBetaInviteNotFound(): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/private-beta-invite"))
        .willReturn(aResponse()
          .withStatus(404)))

  def givenOptinStatusSuccessReturnsForArn( arn: Arn, optinStatus: OptinStatus): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn/${arn.value}/optin-status"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(s""" "${optinStatus.value}" """)))

  def givenOptinRecordExistsForArn(arn: Arn, exists: Boolean): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn/${arn.value}/optin-record-exists"))
        .willReturn(
          aResponse()
            .withStatus(if(exists) NO_CONTENT else NOT_FOUND))
    )

  def givenOptinStatusFailedForArn(arn: Arn): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn/${arn.value}/optin-status"))
        .willReturn(aResponse()
        .withStatus(500))
    )

  def givenAccessGroupsForArn(arn: Arn, accessGroupSummaries: AccessGroupSummaries): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn/${arn.value}/groups"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.toJson(accessGroupSummaries).toString)))

  def givenAccessGroupsForTeamMember(arn: Arn, credentialsProviderId: String, groupSummaries: Seq[GroupSummary]): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-permissions/arn/${arn.value}/team-member/$credentialsProviderId/groups"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(Json.toJson(groupSummaries).toString)))

  def givenHideBetaInviteResponse(conflict: Boolean = false): StubMapping =
    stubFor(
      post(urlEqualTo(s"/agent-permissions/private-beta-invite/decline"))
        .willReturn(
          aResponse()
            .withStatus(if(conflict) CONFLICT else CREATED))
    )

}
