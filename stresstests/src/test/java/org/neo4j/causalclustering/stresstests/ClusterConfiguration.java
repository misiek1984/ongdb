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
package org.neo4j.causalclustering.stresstests;

import java.util.Map;
import java.util.function.IntFunction;

import org.neo4j.backup.OnlineBackupSettings;
import org.neo4j.causalclustering.core.CausalClusteringSettings;
import org.neo4j.helpers.SocketAddress;
import org.neo4j.kernel.configuration.Settings;

import static org.neo4j.kernel.configuration.Settings.TRUE;

class ClusterConfiguration
{
    private ClusterConfiguration()
    {
        // no instances
    }

    static Map<String,String> enableRaftMessageLogging( Map<String,String> settings )
    {
        settings.put( CausalClusteringSettings.raft_messages_log_enable.name(), Settings.TRUE );
        return settings;
    }

    static Map<String,String> configureRaftLogRotationAndPruning( Map<String,String> settings )
    {
        settings.put( CausalClusteringSettings.raft_log_rotation_size.name(), "1K" );
        settings.put( CausalClusteringSettings.raft_log_pruning_frequency.name(), "250ms" );
        settings.put( CausalClusteringSettings.raft_log_pruning_strategy.name(), "keep_none" );
        return settings;
    }

    static Map<String,IntFunction<String>> configureBackup( Map<String,IntFunction<String>> settings,
            IntFunction<SocketAddress> address )
    {
        settings.put( OnlineBackupSettings.online_backup_enabled.name(), id -> TRUE );
        settings.put( OnlineBackupSettings.online_backup_server.name(), id -> address.apply( id ).toString() );
        return settings;
    }
}
