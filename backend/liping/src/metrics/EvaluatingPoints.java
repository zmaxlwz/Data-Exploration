package metrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import middleware.TableAttrStat;
import weka.core.Attribute;

/**
 * 
 * @author lppeng
 *
 */
public class EvaluatingPoints {
	public HashMap<Long, double []> points;
	/**
	 * grid points
	 * @param taStat
	 * @param numPoints
	 * @throws Exception
	 */
	public EvaluatingPoints(TableAttrStat taStat, int numPoints) throws Exception {
		if(taStat==null) {
			throw new Exception("INSTRUCTION: obtain TableAttrStat first before initializing points to evaluate");
		}
		
		points = new HashMap<Long, double []>();
			
		int granularity = (int)Math.pow(numPoints, 1.0/taStat.dim);
		int _numPoints = (int)Math.pow(granularity,taStat.dim);
		for(long i=0; i<_numPoints; i++) {
			long _i = i;
			double [] grid = new double [taStat.dim];
			for(int j=0; j<taStat.dim; j++) {
				grid[j] = taStat.lowerB[j]+(_i%granularity)*(taStat.upperB[j]-taStat.lowerB[j])/(granularity-1);
				_i = _i/granularity;
			}
			points.put(i, grid);
		}
	}
	/**
	 * random tuples from database
	 * @param taStat
	 * @param stmt
	 * @param numPoints
	 * @throws Exception
	 */
	public EvaluatingPoints(TableAttrStat taStat, Statement stmt, int numPoints, String keyAttr) throws Exception {
		if(taStat==null) {
			throw new Exception("INSTRUCTION: obtain TableAttrStat first before initializing points to evaluate");
		}
		
		points = new HashMap<Long, double []>();
		
		Random r = new Random();
		for(int i=0; i<numPoints;) {
			long random_row_id = (long)(r.nextDouble()*(taStat.max_row_id-taStat.min_row_id+1))+taStat.min_row_id;
			if(!points.containsKey(random_row_id)) {
				StringBuilder sb = new StringBuilder();
				sb.append("select ");
				sb.append(keyAttr);
				for(Attribute a:taStat.attrs) {
					sb.append(',');
					sb.append(a.name());
				}
				sb.append(" from ");
				sb.append(taStat.tbl_name);
				sb.append(" where row_id=");
				sb.append(random_row_id);
				sb.append(';');
				try {
					
					ResultSet rs = stmt.executeQuery(sb.toString());
					
					ResultSetMetaData rsmd = rs.getMetaData();
					int columnCount = rsmd.getColumnCount();
					rs.next();
					
					long key = rs.getLong(1);
					double [] grid = new double [taStat.dim];
					for(int j=2; j<=columnCount; j++) { 
			    		String columnType = rsmd.getColumnTypeName(j);    		
			    		if(columnType.startsWith("float")) {
			    			grid[j-2] = rs.getDouble(j);
			    		}
			    		else if(columnType.startsWith("int")) {
			    			grid[j-2] = rs.getLong(j);
			    		}
			    		else {
			    			throw new Exception(columnType);
			    		}
			    	}
					points.put(key, grid);
			    	rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				i++;
			}
		}
	}
	/**
	 * reading from a file
	 * @param taStat
	 * @param fileName
	 * @throws Exception
	 */
	public EvaluatingPoints(TableAttrStat taStat, String fileName) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		points = new HashMap<Long, double []>();
		String nextLine;
		while((nextLine=br.readLine())!=null) {
			if(nextLine.startsWith("#")) {
				continue;
			}
			StringTokenizer st = new StringTokenizer(nextLine, ", \t");
			if(st.countTokens()!=taStat.dim+1) {
				br.close();
				throw new Exception("dimensionalities do not match: " + st.countTokens() + " vs " + (taStat.dim+1));
			}
			long key = Long.valueOf(st.nextToken());
			double [] value = new double [taStat.dim];
			for(int i=0; i<taStat.dim; i++) {
				value[i] = Double.valueOf(st.nextToken());
			}
			points.put(key, value);
		}
		br.close();
	}
}
