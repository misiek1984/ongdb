/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.internal.cypher.acceptance

import java.util

import org.neo4j.kernel.api.proc.Neo4jTypes

class FunctionCallSupportAcceptanceTest extends ProcedureCallAcceptanceTest {

  test("should return correctly typed map result (even if converting to and from scala representation internally)") {
    val value = new util.HashMap[String, Any]()
    value.put("name", "Cypher")
    value.put("level", 9001)

    registerUserFunction(value)

    // Using graph execute to get a Java value
    graph.execute("RETURN my.first.value()").stream().toArray.toList should equal(List(
      java.util.Collections.singletonMap("my.first.value()", value)
    ))
  }

  test("should return correctly typed list result (even if converting to and from scala representation internally)") {
    val value = new util.ArrayList[Any]()
    value.add("Norris")
    value.add("Strange")

    registerUserFunction(value)

    // Using graph execute to get a Java value
    graph.execute("RETURN my.first.value() AS out").stream().toArray.toList should equal(List(
      java.util.Collections.singletonMap("out", value)
    ))
  }

  test("should return correctly typed stream result (even if converting to and from scala representation internally)") {
    val value = new util.ArrayList[Any]()
    value.add("Norris")
    value.add("Strange")
    val stream = value.stream()

    registerUserFunction(stream)

    // Using graph execute to get a Java value
    graph.execute("RETURN my.first.value() AS out").stream().toArray.toList should equal(List(
      java.util.Collections.singletonMap("out", stream)
    ))
  }

  test("should not copy lists unnecessarily") {
    val value = new util.ArrayList[Any]()
    value.add("Norris")
    value.add("Strange")

    registerUserFunction(value)

    // Using graph execute to get a Java value
    val returned = graph.execute("RETURN my.first.value() AS out").next().get("out")

    returned shouldBe an [util.ArrayList[_]]
    returned shouldBe value
  }

  test("should not copy unnecessarily with nested types") {
    val value = new util.ArrayList[Any]()
    val inner = new util.ArrayList[Any]()
    value.add("Norris")
    value.add(inner)

    registerUserFunction(value)

    // Using graph execute to get a Java value
    val returned = graph.execute("RETURN my.first.value() AS out").next().get("out")

    returned shouldBe an [util.ArrayList[_]]
    returned shouldBe value
  }

  test("should handle interacting with list") {
    val value = new util.ArrayList[Integer]()
    value.add(1)
    value.add(3)

    registerUserFunction(value, Neo4jTypes.NTList(Neo4jTypes.NTInteger))

    // Using graph execute to get a Java value
    val returned = graph.execute("WITH my.first.value() AS list RETURN list[0] + list[1] AS out")
      .next().get("out")

    returned should equal(4)
  }
}
