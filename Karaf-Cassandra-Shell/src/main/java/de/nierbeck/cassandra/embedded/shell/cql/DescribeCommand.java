package de.nierbeck.cassandra.embedded.shell.cql;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Parsing;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.apache.karaf.shell.support.table.ShellTable;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import de.nierbeck.cassandra.embedded.shell.CqlExecuter;
import de.nierbeck.cassandra.embedded.shell.SessionParameter;

/**
 * Should support: 
 * 
 * DESCRIBE KEYSPACES
 * 		Output is a list of all keyspace names.
 * 
 * DESCRIBE KEYSPACE keyspace_name
 * 		Output is a list of CQL commands that could be used to recreate the given keyspace, and the tables in it. In some cases, as the CQL interface matures, there will be some metadata about a keyspace that is not representable with CQL. That metadata will not be shown.
 * 
 * 		The <keyspacename> argument can be omitted when using a non-system keyspace; in that case, the current keyspace is described.
 * 
 * DESCRIBE <FULL> SCHEMA
 * 		Output is a list of CQL commands that could be used to recreate the entire user-created schema. Works as though DESCRIBE KEYSPACE <k> was invoked for each keyspace k. Use DESCRIBE FULL SCHEMA to include the system keyspaces.
 * 
 * DESCRIBE TABLES
 * 		Output is a list of the names of all tables in the current keyspace, or in all keyspaces if there is no current keyspace.
 * 
 * DESCRIBE TABLE table_name
 * 		Output is a list of CQL commands that could be used to recreate the given table. In some cases, there might be table metadata that is not representable and it is not shown.
 * 
 * DESCRIBE TYPE type_name
 * 		Output is a list of components of type_name.
 * 
 * DESCRIBE TYPES
 * 		Output is a list of user-defined types.
 * 
 * ATTENTION, currently only TABLE, TABLES and KEYSPACES are supported. 
 * 
 */
@Command(scope = "cassandra:cqlsh", name = "DESCRIBE", description = "execute USE cql commands")
@Parsing(CqlParser.class)
@Service
public class DescribeCommand implements Action {

	@Reference
	protected org.apache.karaf.shell.api.console.Session session;

	@Argument(name = "describe", description = "DESCRIBE ?", required = true, multiValued = true)
	@Completion(caseSensitive = false, values = { "keyspaces", "TABLES" }, value = StringsCompleter.class)
	private List<String> describe;

	public Object execute() throws Exception {
		Session session = (Session) this.session.get(SessionParameter.CASSANDRA_SESSION);

		if (session == null) {
			System.err.println("No active session found--run the connect command first");
			return null;
		}

		String loggedKeyspace = session.getLoggedKeyspace();
		
		ShellTable table = new ShellTable();

		ResultSet execute = null;

		switch (describe.get(0)) {
			case "TABLES":
			case "tables":
				execute = session
						.execute("select columnfamily_name from system.schema_columnfamilies where keyspace_name = '"
								+ session.getLoggedKeyspace() + "';");
				break;
			case "table":
			case "TABLE":
				String keyspaceName = loggedKeyspace;
				String tableName = describe.get(1).trim();
				if (keyspaceName == null) {
					String[] split = tableName.split("\\.");
					keyspaceName = split[0].trim();
					tableName = split[1].trim();
				}
				String select = String
						.format("SELECT column_name, type FROM system.schema_columns WHERE keyspace_name = '%s' AND columnfamily_name = '%s'",
								keyspaceName, tableName);
				execute = session.execute(select);
				break;
			case "keyspaces":
			default:
				execute = session.execute("select * from  system.schema_keyspaces;");
				break;
		}

		if (execute != null) {
			CqlExecuter.cassandraRowFormater(table, execute);
			table.print(System.out);
		}

		return null;
	}

}
