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

package uk.gov.hmrc.agentservicesaccount.services

import org.apache.pekko.Done
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanSuccess
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.RetentionPeriod
import uk.gov.hmrc.objectstore.client.Sha256Checksum
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.http.StringContextOps

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ObjectStoreService @Inject() (
  playObjectStoreClient: PlayObjectStoreClient,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
extends Logging {

  private def rootDirectory(reference: String) = Path.Directory(s"/$reference")

  def deleteObject(oldReference: String)(implicit request: RequestHeader): Future[Unit] =
    (for {
      objects <- playObjectStoreClient
        .listObjects(
          path = rootDirectory(oldReference),
          owner = appConfig.objectStoreOwner
        )
      _ <- Future.sequence(objects.objectSummaries.map(objectListing =>
        playObjectStoreClient.deleteObject(
          objectListing.location.copy(directory = Path.Directory(objectListing.location.directory.value.replace(appConfig.objectStoreOwner, ""))),
          owner = appConfig.objectStoreOwner
        )
      ))
    } yield ()).recover {
      case e => logger.error(s"[ObjectStoreService] Failed to delete object $oldReference", e)
    }

  /** Transfers the file from Upscan to Object Store if the upload was successful. Returns the Object Store file path if the transfer was successful, None
    * otherwise.
    */
  def transferFileToObjectStore(
    uplodDetails: UpscanSuccess
  )(implicit request: RequestHeader): Future[Path.File] = {
    val fileLocation: Path.File = rootDirectory(uplodDetails.reference.value).file(s"${uplodDetails.fileName}")
    val contentSha256 = Sha256Checksum.fromHex(uplodDetails.checksum)
    playObjectStoreClient.uploadFromUrl(
      from = url"${uplodDetails.downloadUrl}",
      to = fileLocation,
      retentionPeriod = RetentionPeriod.SixMonths, // TODO: how long do we need to keep these files?
      contentType = Some(uplodDetails.mimeType),
      contentSha256 = Some(contentSha256),
      owner = appConfig.objectStoreOwner
    )
      .map(_.location)
  }

}
