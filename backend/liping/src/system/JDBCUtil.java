package system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.math3.util.Pair;

public class JDBCUtil {
	// JDBC driver name and database URL
	public static final String JDBC_DRIVER = "org.postgresql.Driver"; 

	public static Pair<Connection, Statement> connectDB(SystemConfig config) throws ClassNotFoundException, SQLException {		
		String DB_URL = "jdbc:postgresql://" + config.host + ":" + config.port + "/" + config.db_name;

	     //Register JDBC driver
	     Class.forName(JDBC_DRIVER);

	     //Open a connection
	     Connection conn = DriverManager.getConnection(DB_URL,config.user,config.pwd);
	     Statement stmt = conn.createStatement();
	     return new Pair<Connection, Statement>(conn, stmt);
	}
}
