/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.internal.kernel.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings( "Duplicates" )
public abstract class RelationshipTransactionStateTestBase<G extends KernelAPIWriteTestSupport> extends KernelAPIWriteTestBase<G>
{
    @Test
    public void shouldSeeSingleRelationshipInTransaction() throws Exception
    {
        int label;
        long n1, n2;
        try ( Transaction tx = session.beginTransaction() )
        {
            n1 = tx.dataWrite().nodeCreate();
            n2 = tx.dataWrite().nodeCreate();
            long decoyNode = tx.dataWrite().nodeCreate();
            label = tx.tokenWrite().relationshipTypeGetOrCreateForName( "R" ); // to have >1 relationship in the db
            tx.dataWrite().relationshipCreate( n2, label, decoyNode );
            tx.success();
        }

        try ( Transaction tx = session.beginTransaction() )
        {
            label = tx.tokenWrite().relationshipTypeGetOrCreateForName( "R" );
            long r = tx.dataWrite().relationshipCreate( n1, label, n2 );
            try ( RelationshipScanCursor relationship = cursors.allocateRelationshipScanCursor() )
            {
                tx.dataRead().singleRelationship( r, relationship );
                assertTrue( "should find relationship", relationship.next() );

                assertEquals( label, relationship.label() );
                assertEquals( n1, relationship.sourceNodeReference() );
                assertEquals( n2, relationship.targetNodeReference() );
                assertEquals( r, relationship.relationshipReference() );

                assertFalse( "should only find one relationship", relationship.next() );
            }
            tx.success();
        }
    }

    @Test
    public void shouldNotSeeSingleRelationshipWhichWasDeletedInTransaction() throws Exception
    {
        int label;
        long n1, n2, r;
        try ( Transaction tx = session.beginTransaction() )
        {
            n1 = tx.dataWrite().nodeCreate();
            n2 = tx.dataWrite().nodeCreate();
            label = tx.tokenWrite().relationshipTypeGetOrCreateForName( "R" );

            long decoyNode = tx.dataWrite().nodeCreate();
            tx.dataWrite().relationshipCreate( n2, label, decoyNode ); // to have >1 relationship in the db

            r = tx.dataWrite().relationshipCreate( n1, label, n2 );
            tx.success();
        }

        try ( Transaction tx = session.beginTransaction() )
        {
            assertTrue( "should delete relationship", tx.dataWrite().relationshipDelete( r ) );
            try ( RelationshipScanCursor relationship = cursors.allocateRelationshipScanCursor() )
            {
                tx.dataRead().singleRelationship( r, relationship );
                assertFalse( "should not find relationship", relationship.next() );
            }
            tx.success();
        }
    }
}
