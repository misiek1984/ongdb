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
package org.neo4j.causalclustering.core.consensus.log.segmented;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReferenceCounterTest
{
    private ReferenceCounter refCount = new ReferenceCounter();

    @Test
    public void shouldHaveValidInitialBehaviour() throws Exception
    {
        assertEquals( 0, refCount.get() );
        assertTrue( refCount.tryDispose() );
    }

    @Test
    public void shouldNotBeAbleToDisposeWhenActive() throws Exception
    {
        // when
        refCount.increase();

        // then
        assertFalse( refCount.tryDispose() );
    }

    @Test
    public void shouldBeAbleToDisposeInactive() throws Exception
    {
        // given
        refCount.increase();
        refCount.increase();

        // when / then
        refCount.decrease();
        assertFalse( refCount.tryDispose() );

        // when / then
        refCount.decrease();
        assertTrue( refCount.tryDispose() );
    }

    @Test
    public void shouldNotGiveReferenceWhenDisposed() throws Exception
    {
        // given
        refCount.tryDispose();

        // then
        assertFalse( refCount.increase() );
    }

    @Test
    public void shouldAdjustCounterWithReferences() throws Exception
    {
        // when / then
        refCount.increase();
        assertEquals( 1, refCount.get() );

        // when / then
        refCount.increase();
        assertEquals( 2, refCount.get() );

        // when / then
        refCount.decrease();
        assertEquals( 1, refCount.get() );

        // when / then
        refCount.decrease();
        assertEquals( 0, refCount.get() );
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenDecreasingPastZero() throws Exception
    {
        // given
        refCount.increase();
        refCount.decrease();

        // when
        try
        {
            refCount.decrease();
            fail();
        }
        catch ( IllegalStateException e )
        {
            // expected
        }
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenDecreasingOnDisposed() throws Exception
    {
        // given
        refCount.tryDispose();

        // when
        try
        {
            refCount.decrease();
            fail();
        }
        catch ( IllegalStateException e )
        {
            // expected
        }
    }
}
