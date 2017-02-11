package system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONObject;
import org.json.JSONTokener;

import middleware.user.GroundTruth;

public class Context {
	public Connection conn;
	public Statement stmt;
	public String truthPredicate;
	public GroundTruth truth;
	
	public void parseTruthPredicate(SystemConfig config, String configFileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(configFileName));
		JSONTokener tokener = new JSONTokener(br);
		JSONObject configJson = new JSONObject(tokener);
		truthPredicate = configJson.getString("truth");
		br.close();
		truth = new GroundTruth (config, truthPredicate, stmt);
	}
	
	public void close() throws SQLException {
		if(conn!=null) conn.close();
		if(stmt!=null) stmt.close();
	}
}
