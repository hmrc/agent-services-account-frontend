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

package uk.gov.hmrc.agentservicesaccount.views

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, OptionValues}
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.support.Css.{H1, H2, H3, H4, paragraphs}

trait ViewBaseSpec extends AnyWordSpecLike with Matchers with OptionValues with ScalaFutures {

  def asDocument(html: Html): Document = Jsoup.parse(html.toString())

  def getElements(doc: Document, cssSelector: String): Elements = {
    val elements = doc.select(cssSelector)
    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")
    else elements
  }

  def assertElementNotOnPage(doc: Document, cssSelector: String): Assertion = {
    val elements = doc.select(cssSelector)
    elements.isEmpty shouldBe true
  }

  def assertEqualsValue(doc: Document, cssSelector: String, expectedValue: String): Assertion = {
    val elements = getElements(doc, cssSelector)
    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(elements.first().html().replace("\n", "") == expectedValue)
  }

  def assertElementContainsText(doc: Document, cssSelector: String, expectedText: String): Assertion = {
    val element = getElements(doc, cssSelector)
    element.text shouldBe expectedText
  }

  def assertElementInPositionContainsText(doc: Document,
                                          cssSelector: String,
                                          index: Int = 0,
                                          expectedText: String
                                          ): Assertion = {
    val elements = getElements(doc, cssSelector)
    elements.get(index).text shouldBe expectedText
  }

  def expectTextForElement(element: Element, expectedText: String): Assertion = {
    element.text shouldBe expectedText
  }

  def assertAttributeValueForElement(element: Element, attribute: String = "href", attributeValue: String): Assertion = {
    element.attr(attribute) shouldBe attributeValue
  }

  def assertRenderedById(doc: Document, id: String): Assertion = {
    assert(doc.getElementById(id) != null, "\n\nElement " + id + " was not rendered on the page.\n")
  }

  def assertNotRenderedById(doc: Document, id: String): Assertion = {
    assert(doc.getElementById(id) == null, "\n\nElement " + id + " was rendered on the page.\n")
  }

  // less specific, try to avoid
  def assertPageContainsText(doc: Document, text: String): Assertion = assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

  def assertDoesNotContainText(doc: Document, text: String): Assertion =
    assert(!doc.toString.contains(text), "\n\ntext " + text + " was rendered on the page.\n")

  def expectedTitle(doc: Document, expected: String): Assertion = {
    doc.title() shouldBe expected
  }

  def expectedH1(doc: Document, expected: String): Assertion = {
    doc.select(H1).get(0).text shouldBe expected
  }

  def expectedH2(doc: Document, expected: String, index: Int = 0): Assertion = {
    doc.select(H2).get(index).text shouldBe expected
  }

  def expectedH3(doc: Document, expected: String, index: Int = 0): Assertion = {
    doc.select(H3).get(index).text shouldBe expected
  }

  def expectedH4(doc: Document, expected: String, index: Int = 0): Assertion = {
    doc.select(H4).get(index).text shouldBe expected
  }


}
