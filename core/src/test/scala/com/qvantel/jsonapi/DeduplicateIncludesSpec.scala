/*
Copyright (c) 2017, Qvantel
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
 * Neither the name of the Qvantel nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Qvantel BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qvantel.jsonapi

import org.specs2.mutable.Specification
import spray.json._
import Models._
import com.qvantel.jsonapi.Deduplicator.includedEntities
import com.qvantel.jsonapi.Helpers.{readStrFromFile, sortAllArraysById}


class DeduplicateIncludesSpec extends Specification {
  val sofia = Person("Sofia")
  val eva = Person("Eva")
  val roman = Person("Roman-younger")

  val evaAndRoman: ToMany[Person] = ToMany.loaded(Seq(eva, roman))

  val maria = Person("Maria")
  val mariaWithChildren = maria.copy(children = evaAndRoman)

  val iryna = Person("Iryna")
  val irynaWithChild = iryna.copy(children = ToMany.loaded(Seq(sofia)))

  val bogdan = Person("Bogdan")
  val bogdanWithChild = bogdan.copy(children = ToMany.loaded(Seq(sofia)))

  val yuriy = Person("Yuriy")
  val yuriyWithTasks = yuriy.copy(tasks = ToMany.loaded(Seq(Task("bring Eva to Kindergarten"))))
  val yuriyWithChildren = yuriy.copy(children = evaAndRoman)

  val irynaAndYuriy = ToMany.loaded(Seq(iryna, yuriyWithTasks))

  val wira  = Person("Wira", irynaAndYuriy)
  val romanOlder = Person("Roman-older", irynaAndYuriy)

  val families = try {
    Seq(
      Family("Pigovsky-older", ToOne.loaded(romanOlder), ToOne.loaded(wira)),
      Family("Polischtschuk", ToOne.loaded(bogdanWithChild), ToOne.loaded(irynaWithChild)),
      Family("Pigovsky-younger", ToOne.loaded(yuriyWithChildren), ToOne.loaded(mariaWithChildren)),
    )
  } catch {
    case x =>
      println(x)
      x.printStackTrace()
      throw x
  }

  val json   = rawCollection(families)

  "shall not duplicate people in `included`" >> {
    val actual = sortAllArraysById(
      Deduplicator.deduplicateIncludes(json)
    )

    val idEntitiesSeq = includedEntities(actual)

    val entitiesGroupedById = idEntitiesSeq.groupBy(_._1)

    val duplicates = entitiesGroupedById.filter(_._2.length > 1)

    duplicates.map {
      case (id, entities) =>
        entities(0)._2.prettyPrint must be equalTo
          entities(1)._2.prettyPrint
    }

    duplicates must beEmpty

    val expected = sortAllArraysById(readStrFromFile(
      "families.json"
    ).parseJson).prettyPrint

    actual.prettyPrint must be equalTo expected
  }
}
