package tuplereader;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import middleware.datastruc.DenseInstanceWithID;
import system.SystemConfig;
import weka.core.Attribute;
import weka.core.Instance;

public class DBTupleReader<PK> implements TupleReader<PK>{

	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "org.postgresql.Driver";  
	final String DB_URL;

	//  Database credentials
	final String USER;
	final String PASS;
	
	Connection conn = null;
	Statement stmt = null;
	ResultSet rs = null;
	ResultSetMetaData rsmd = null;
	
	PrintWriter pw = null;
	
	public DBTupleReader(SystemConfig config) throws IOException {
		DB_URL = "jdbc:postgresql://" + config.host + ":" + config.port + "/" + config.db_name;
		USER = config.user;
		PASS = config.pwd;
		
		if(config.debug) {
			pw = new PrintWriter(new FileWriter(config.tbl_name+".txt"));
		}
 
		   try{
		      //Register JDBC driver
		      Class.forName(JDBC_DRIVER);
	
		      //Open a connection
		      conn = DriverManager.getConnection(DB_URL,USER,PASS);
		     
		      //Execute a query
		      stmt = conn.createStatement();
		      StringBuilder sb = new StringBuilder();
		      sb.append("select ");
		      		      
	    	  sb.append(config.key_attr);				  			      
	    	  for(Attribute a:config.where_attrs) {
	    		  sb.append(',');
	    		  sb.append(a.name());
	    	  }		      
		      
		      sb.append(" from ");
		      sb.append(config.tbl_name);
		      String sql = sb.toString();
		      rs = stmt.executeQuery(sql);
		      rsmd = rs.getMetaData();
		     		      
		   }catch(SQLException se){
		      //Handle errors for JDBC
		      se.printStackTrace();
		   }catch(Exception e){
		      //Handle errors for Class.forName
		      e.printStackTrace();
		   }
	}
	
	public void close() {
		//Clean-up environment
	    try {
			rs.close();
			if(stmt!=null)
				stmt.close();
			if(conn!=null)
				conn.close();
			if(pw!=null)
				pw.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Instance getNext() {
		try {
			if(rs.next()) {
				int columnCount = rsmd.getColumnCount();
				Instance sample = new DenseInstanceWithID(columnCount-1,rs.getLong(1));
				if(pw!=null) {
					pw.print(rs.getString(1) + " \t");
				}
		    	for(int i=2; i<=columnCount; i++) { // column index starts with 1
		    		pw.print(rs.getString(i) + " \t");
		    		String columnType = rsmd.getColumnTypeName(i);
		    		if(columnType.startsWith("float")) {
		    			sample.setValue(i-2, rs.getDouble(i));
		    		}
		    		else if(columnType.startsWith("int")) {
		    			sample.setValue(i-2, rs.getLong(i));
		    		}
		    		else if(columnType.equals("string")){
		    			System.err.println(columnType);
		    		}
		    	}
		    	if(pw!=null) {
		    		pw.println();
		    	}
		    	return sample;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Instance getTuple(Object keyValue) {
		// TODO Auto-generated method stub
		return null;
	}
}
