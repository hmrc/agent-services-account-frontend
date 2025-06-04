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

package uk.gov.hmrc.agentservicesaccount.support

import java.net.ServerSocket
import java.net.URL

import scala.annotation.tailrec
import scala.util.Random

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import play.Logger

case class WireMockBaseUrl(value: URL)

object WireMockSupport {
  // We have to make the wireMockPort constant per-JVM instead of constant
  // per-WireMockSupport-instance because config values containing it are
  // cached in the GGConfig object
  private lazy val wireMockPort: Int = Port.randomAvailable
}

trait WireMockSupport
extends BeforeAndAfterAll
with BeforeAndAfterEach {
  me: Suite =>

  val wireMockPort: Int = WireMockSupport.wireMockPort
  val wireMockHost: String = "localhost"
  val wireMockBaseUrlAsString: String = s"http://$wireMockHost:$wireMockPort"
  val wireMockBaseUrl: URL = new URL(wireMockBaseUrlAsString)
  protected implicit val implicitWireMockBaseUrl: WireMockBaseUrl = WireMockBaseUrl(wireMockBaseUrl)

  protected def basicWireMockConfig(): WireMockConfiguration = wireMockConfig()

  private val wireMockServer: WireMockServer = new WireMockServer(basicWireMockConfig().port(wireMockPort))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    Thread.sleep(100)
    WireMock.reset()
  }

  protected def stopWireMockServer(): Unit = wireMockServer.stop()

  protected def startWireMockServer(): Unit = wireMockServer.start()

}

// This class was copy-pasted from the hmrctest project, which is now deprecated.
object Port {

  val rnd: Random = new scala.util.Random
  val range: Seq[Int] = 8000 to 39999
  val usedPorts: Seq[Int] = List[Int]()

  @tailrec
  def randomAvailable: Int =
    range(rnd.nextInt(range.length)) match {
      case 8080 => randomAvailable
      case 8090 => randomAvailable
      case p: Int => {
        available(p) match {
          case false => {
            Logger.of("WireMockSupport").debug(s"Port $p is in use, trying another")
            randomAvailable
          }
          case true => {
            Logger.of("WireMockSupport").debug("Taking port : " + p)
            usedPorts :+ p
            p
          }
        }
      }
    }

  private def available(p: Int): Boolean = {
    var socket: ServerSocket = null
    try {
      if (!usedPorts.contains(p)) {
        socket = new ServerSocket(p)
        socket.setReuseAddress(true)
        true
      }
      else {
        false
      }
    }
    catch {
      case t: Throwable => false
    }
    finally {
      if (socket != null)
        socket.close()
    }
  }

}
