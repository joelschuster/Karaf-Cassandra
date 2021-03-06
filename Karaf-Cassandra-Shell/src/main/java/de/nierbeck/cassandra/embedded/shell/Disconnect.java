package de.nierbeck.cassandra.embedded.shell;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

@Command(scope = "cassandra", name = "disconnect", description = "Disconnect from cassandra server")
@Service
public class Disconnect extends CassandraCommandSupport {

	@Override
	public Object doExecute() throws Exception {

		disconnect();

		return null;
	}

	public void disconnect() {
		this.session.put("org.apache.felix.gogo.expansion", true);
		Cluster cluster = (Cluster) this.session
				.get(SessionParameter.CASSANDRA_CLUSTER);
		Session session = (Session) this.session
				.get(SessionParameter.CASSANDRA_SESSION);

		String clusterName = "not available anymore";
		
		if (session == null) {
			System.err.println("No active session found!");
		} else {
			session.close();
			this.session.put(SessionParameter.CASSANDRA_SESSION, null);
		}

		if (cluster == null) {
			System.err.println("No active cluster connection found!");
		} else {
			clusterName = cluster.getMetadata().getClusterName();
			cluster.close();
			this.session.put(SessionParameter.CASSANDRA_CLUSTER, null);
		}

		System.out.println("disconnected from '"+clusterName+ "'");
	}
}
