/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.unsafe.impl.batchimport.staging;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.unsafe.impl.batchimport.Configuration;
import org.neo4j.unsafe.impl.batchimport.stats.StepStats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ForkedProcessorStepTest
{
    @Test
    public void shouldProcessAllSingleThreaded() throws Exception
    {
        // GIVEN
        StageControl control = mock( StageControl.class );
        int processors = 10;

        int batches = 10;
        BatchProcessor step = new BatchProcessor( control, processors );
        TrackingStep downstream = new TrackingStep();
        step.setDownstream( downstream );
        step.processors( processors - step.processors( 0 ) );

        // WHEN
        step.start( 0 );
        for ( int i = 1; i <= batches; i++ )
        {
            step.receive( i, new Batch( processors ) );
        }
        step.endOfUpstream();
        while ( !step.isCompleted() )
        {
            Thread.sleep( 10 );
        }
        step.close();

        // THEN
        assertEquals( batches, downstream.received.get() );
    }

    @Test
    public void shouldProcessAllMultiThreadedAndWithChangingProcessorCount() throws Exception
    {
        // GIVEN
        StageControl control = mock( StageControl.class );
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        BatchProcessor step = new BatchProcessor( control, availableProcessors );
        TrackingStep downstream = new TrackingStep();
        step.setDownstream( downstream );

        // WHEN
        step.start( 0 );
        AtomicLong nextTicket = new AtomicLong();
        Thread[] submitters = new Thread[3];
        AtomicBoolean end = new AtomicBoolean();
        for ( int i = 0; i < submitters.length; i++ )
        {
            submitters[i] = new Thread()
            {
                @Override
                public void run()
                {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    while ( !end.get() )
                    {
                        // synchronized block simulating that batches are received in order,
                        // which is enforced in real environment of a stage.
                        synchronized ( nextTicket )
                        {
                            // The processor count is changed here in this block simply because otherwise
                            // it's very hard to know how many processors we expect to see have processed
                            // a particular batch.
                            if ( random.nextFloat() < 0.1 )
                            {
                                step.processors( random.nextInt( -2, 4 ) );
                            }

                            long ticket = nextTicket.incrementAndGet();
                            Batch batch = new Batch( step.processors( 0 ) );
                            step.receive( ticket, batch );
                        }
                    }
                }
            };
            submitters[i].start();
        }

        while ( downstream.received.get() < 200 )
        {
            Thread.sleep( 10 );
        }
        end.set( true );
        for ( Thread submitter : submitters )
        {
            submitter.join();
        }
        step.endOfUpstream();
        step.close();
    }

    @Test
    public void shouldKeepForkedOrderIntactWhenChangingProcessorCount() throws Exception
    {
        int length = 100;
        AtomicIntegerArray reference = new AtomicIntegerArray( length );

        // GIVEN
        StageControl control = mock( StageControl.class );
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ForkedProcessorStep<int[]> step = new ForkedProcessorStep<int[]>( control, "Processor",
                config( availableProcessors ) )
        {
            @Override
            protected void forkedProcess( int id, int processors, int[] batch ) throws InterruptedException
            {
                int ticket = batch[0];
                Thread.sleep( ThreadLocalRandom.current().nextInt( 10 ) );
                for ( int i = 1; i < batch.length; i++ )
                {
                    if ( batch[i] % processors == id )
                    {
                        assertTrue( "Was expecting " + ticket + " for " + batch[i],
                                reference.compareAndSet( batch[i], ticket, ticket + 1 ) );
                    }
                }
            }
        };
        DeadEndStep downstream = new DeadEndStep( control );
        step.setDownstream( downstream );

        // WHEN
        step.start( 0 );
        downstream.start( 0 );
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for ( int ticket = 0; ticket < 200; ticket++ )
        {
            // The processor count is changed here in this block simply because otherwise
            // it's very hard to know how many processors we expect to see have processed
            // a particular batch.
            if ( random.nextFloat() < 0.1 )
            {
                int p = step.processors( random.nextInt( -2, 4 ) );
            }

            int[] batch = new int[length];
            batch[0] = ticket;
            for ( int j = 1; j < batch.length; j++ )
            {
                batch[j] = j - 1;
            }
            step.receive( ticket, batch );
        }
        step.endOfUpstream();
        while ( !step.isCompleted() )
        {
            Thread.sleep( 10 );
        }
        step.close();
    }

    @Test
    public void shouldPanicOnFailure() throws Exception
    {
        // GIVEN
        SimpleStageControl control = new SimpleStageControl();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        Exception testPanic = new RuntimeException();
        ForkedProcessorStep<Void> step = new ForkedProcessorStep<Void>( control, "Processor",
                config( availableProcessors ) )
        {
            @Override
            protected void forkedProcess( int id, int processors, Void batch ) throws Throwable
            {
                throw testPanic;
            }
        };

        // WHEN
        step.start( 0 );
        step.receive( 1, null );
        control.steps( step );

        // THEN
        while ( !step.isCompleted() )
        {
            Thread.sleep( 10 );
        }
        try
        {
            control.assertHealthy();
        }
        catch ( Exception e )
        {
            assertSame( testPanic, e );
        }
    }

    private static class BatchProcessor extends ForkedProcessorStep<Batch>
    {
        protected BatchProcessor( StageControl control, int processors )
        {
            super( control, "PROCESSOR", config( processors ) );
        }

        @Override
        protected void forkedProcess( int id, int processors, Batch batch )
        {
            batch.processedBy( id );
        }
    }

    private static Configuration config( int processors )
    {
        return new Configuration()
        {
            @Override
            public int maxNumberOfProcessors()
            {
                return processors;
            }
        };
    }

    private static class Batch
    {
        private final boolean[] processed;

        Batch( int processors )
        {
            this.processed = new boolean[processors];
        }

        void processedBy( int id )
        {
            assertFalse( processed[id] );
            processed[id] = true;
        }
    }

    private static class TrackingStep implements Step<Batch>
    {
        private final AtomicLong received = new AtomicLong();

        @Override
        public void receivePanic( Throwable cause )
        {
        }

        @Override
        public void start( int orderingGuarantees )
        {
        }

        @Override
        public String name()
        {
            return "END";
        }

        @Override
        public long receive( long ticket, Batch batch )
        {
            int count = 0;
            for ( int i = 0; i < batch.processed.length; i++ )
            {
                if ( batch.processed[i] )
                {
                    count++;
                }
            }
            assertEquals( batch.processed.length, count );
            if ( !received.compareAndSet( ticket - 1, ticket ) )
            {
                fail( "Hmm " + ticket + " " + received.get() );
            }
            return 0;
        }

        @Override
        public StepStats stats()
        {
            return null;
        }

        @Override
        public void endOfUpstream()
        {
        }

        @Override
        public boolean isCompleted()
        {
            return false;
        }

        @Override
        public void setDownstream( Step<?> downstreamStep )
        {
        }

        @Override
        public void close() throws Exception
        {
        }
    }
}
