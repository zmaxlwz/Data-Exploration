package middleware.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;

import middleware.datastruc.AttributeValue;
import system.SystemConfig;

public class GroundTruth {
	public HashSet<Long> trueKeys = null;
	public ArrayList<Long> allKeys;
	public ArrayList<AttributeValue[]> allTuples;
	
	public GroundTruth(SystemConfig config, String truthPredicate, Statement stmt) {
		trueKeys = new HashSet<Long>();

		if(config.debug) {
			System.out.println("loading ground truth....");
		}
		
		String sql = "select " + config.key_attr + " from " + config.tbl_name + " where " + truthPredicate + ";"; 
		try {
			ResultSet rs = stmt.executeQuery(sql);
		    while(rs.next()){
		    	trueKeys.add(rs.getLong(1));
		   	}
		    rs.close();
		      
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if(config.debug) {
			System.out.println(sql);
			System.out.println(trueKeys.size() + " results");
		}
		
		if(config.mode==SystemConfig.Mode.ACCURACY) {
			allKeys = new ArrayList<Long>();
			allTuples = new ArrayList<AttributeValue[]>();
			
			StringBuilder sb = new StringBuilder();
			sb.append("select ");
			sb.append(config.key_attr);
			for(int i=0; i<config.where_attrs.size(); i++) {
				sb.append(',');
				sb.append(config.where_attrs.get(i).name());
			}
			sb.append(" from ");
			sb.append(config.tbl_name);
			sb.append(';');
			try {
				ResultSet rs = stmt.executeQuery(sb.toString());
				while(rs.next()){
					allKeys.add(rs.getLong(1));
					AttributeValue [] x = new AttributeValue [config.where_attrs.size()];
					for(int j=0; j<config.where_attrs.size(); j++) {
						x[j] = new AttributeValue(j, rs.getDouble(j+2)); 
					}
					allTuples.add(x);
			   	}
			    rs.close();
			}catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
