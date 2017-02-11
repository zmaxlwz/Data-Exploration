package middleware.sampleacquisition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;

import middleware.TableAttrStat;
import middleware.datastruc.DenseInstanceWithID;
import system.Context;
import system.SystemConfig;
import weka.core.Attribute;

public class Sampling {
	public static HashSet<DenseInstanceWithID> loadFromFile(SystemConfig config, String fileName) throws IOException {
		HashSet<DenseInstanceWithID> samples = new HashSet<DenseInstanceWithID>();

		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String nextLine;
		while((nextLine=br.readLine())!=null) {
			if(nextLine.startsWith("#")) {
				continue;
			}
			StringTokenizer st = new StringTokenizer(nextLine, ",");
			int count = st.countTokens();
			long key = Long.valueOf(st.nextToken());
			double [] additionalAttributeValues;
			if(count>1+config.select_attrs.size()+config.where_attrs.size()) {
				additionalAttributeValues = new double [config.select_attrs.size()];
				for(int i=0; i<config.select_attrs.size(); i++) {
					additionalAttributeValues[i] = Double.valueOf(st.nextToken());
				}
			}
			else {
				additionalAttributeValues = null;
			}
			DenseInstanceWithID sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),key,additionalAttributeValues);
 			for(int i=0; i<config.where_attrs.size(); i++) {
				sample.setValue(i, Double.valueOf(st.nextToken()));
			}
 			samples.add(sample);
		}
		br.close();
		return samples;
	}
	
	public static HashSet<DenseInstanceWithID> randomSampling(SystemConfig config, Context context, TableAttrStat tas, int numSamples) throws Exception {
		Random r = new Random();
		HashSet<DenseInstanceWithID> samples = new HashSet<DenseInstanceWithID>();
		
		long query=0, construct=0;
		
		for(int j=0; j<numSamples;j++) {
			long random_row_id = (long)(r.nextDouble()*(tas.max_row_id-tas.min_row_id+1))+tas.min_row_id;
			
			StringBuilder sb = new StringBuilder();
			sb.append("select ");
			sb.append(config.key_attr);
			for(String s:config.select_attrs) {
				sb.append(',');
				sb.append(s);
			}
			for(Attribute a:config.where_attrs) {
				sb.append(',');
				sb.append(a.name());
			}
			sb.append(" from ");
			sb.append(config.tbl_name);
			sb.append(" where row_id=");
			sb.append(random_row_id);
			sb.append(';');
			
			long t1 = System.currentTimeMillis();
			ResultSet rs = context.stmt.executeQuery(sb.toString());
			long t2 = System.currentTimeMillis();
			
			query+=(t2-t1);
			
			ResultSetMetaData rsmd = rs.getMetaData();
			rs.next();
			
			long key = rs.getLong(1);
			double [] additionalAttributeValues = new double [config.select_attrs.size()];
	    	for(int i=0; i<config.select_attrs.size(); i++) { 
	    		String columnType = rsmd.getColumnTypeName(i+2);    		
	    		if(columnType.startsWith("float")) {
	    			additionalAttributeValues[i] = rs.getDouble(i+2);
	    		}
	    		else if(columnType.startsWith("int")) {
	    			additionalAttributeValues[i] = rs.getLong(i+2);
	    		}
	    		else {
	    			throw new Exception("columntype " + columnType);
	    		}
	    	}
	    	DenseInstanceWithID sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),key,additionalAttributeValues);
 			for(int i=0; i<config.where_attrs.size(); i++) {
 				String columnType = rsmd.getColumnTypeName(i+2+config.select_attrs.size());
 				if(columnType.startsWith("float")) {
 					sample.setValue(i, rs.getDouble(i+2+config.select_attrs.size()));
	    		}
	    		else if(columnType.startsWith("int")) {
	    			sample.setValue(i, rs.getLong(i+2+config.select_attrs.size()));
	    		}
	    		else {
	    			throw new Exception("columntype " + columnType);
	    		}
			}
	    	samples.add(sample);	
	    	long t3 = System.currentTimeMillis();
	    	
	    	construct+=(t3-t2);
	    	
	    	rs.close();
		}
		System.err.println(query + "\t" + construct);
		return samples;
	}
	
	

	public static HashSet<DenseInstanceWithID> loadFromDB(SystemConfig config, Context context, TableAttrStat tas) throws Exception {
		String sql = null;
	  	
	  	if(config.initialSamplingMethod==SystemConfig.InitialSamplingMethod.EQUIWIDTH) {
	  		sql = SampingSQL.equiwidth(config.tbl_name, config.key_attr, config.select_attrs, config.where_attrs, tas.lowerB, tas.upperB, config.numBins, config.numSamplesPerBin);
	  	}
	  	else if(config.initialSamplingMethod==SystemConfig.InitialSamplingMethod.EQUIDEPTH) {
	  		sql = SampingSQL.equidepth(config.tbl_name, config.key_attr, config.select_attrs, config.where_attrs, tas.lowerB, tas.upperB, config.numBins, config.numSamplesPerBin);
	  	}
	  	System.out.println(sql);
		
	  	HashSet<DenseInstanceWithID> samples = new HashSet<DenseInstanceWithID>();
		
		ResultSet rs = context.stmt.executeQuery(sql);
		ResultSetMetaData rsmd = rs.getMetaData();

	    while(rs.next()){
	    	long key = rs.getLong(1);
	    	double [] additionalAttributeValues = new double [config.select_attrs.size()];
	    	for(int i=0; i<config.select_attrs.size(); i++) { 
	    		String columnType = rsmd.getColumnTypeName(i+2);    		
	    		if(columnType.startsWith("float")) {
	    			additionalAttributeValues[i] = rs.getDouble(i+2);
	    		}
	    		else if(columnType.startsWith("int")) {
	    			additionalAttributeValues[i] = rs.getLong(i+2);
	    		}
	    		else {
	    			throw new Exception("columntype " + columnType);
	    		}
	    	}
	    	DenseInstanceWithID sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),key,additionalAttributeValues);
 			for(int i=0; i<config.where_attrs.size(); i++) {
 				String columnType = rsmd.getColumnTypeName(i+2+config.select_attrs.size());
 				if(columnType.startsWith("float")) {
 					sample.setValue(i, rs.getDouble(i+2+config.select_attrs.size()));
	    		}
	    		else if(columnType.startsWith("int")) {
	    			sample.setValue(i, rs.getLong(i+2+config.select_attrs.size()));
	    		}
	    		else {
	    			throw new Exception("columntype " + columnType);
	    		}
			}
	    	samples.add(sample);
	   	}
	    rs.close(); 
		return samples;
	}
}
