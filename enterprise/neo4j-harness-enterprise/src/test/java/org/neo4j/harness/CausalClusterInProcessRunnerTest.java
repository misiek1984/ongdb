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
package org.neo4j.harness;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.neo4j.harness.CausalClusterInProcessRunner.CausalCluster;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.test.rule.TestDirectory;

public class CausalClusterInProcessRunnerTest
{
    @ClassRule
    public static final TestDirectory testDirectory = TestDirectory.testDirectory();

    @Test
    public void shouldBootAndShutdownCluster() throws Exception
    {
        CausalCluster cluster = new CausalCluster( 3, 3, testDirectory.absolutePath().toPath(), NullLogProvider.getInstance() );

        cluster.boot();
        cluster.shutdown();
    }
}
