package middleware;

import java.sql.ResultSet;
import java.util.ArrayList;

import system.Context;
import system.SystemConfig;
import weka.core.Attribute;

/**
 * requires the table to have "row_id" attribute
 * @author lppeng
 *
 */
public class TableAttrStat {
	public final String tbl_name;
	public final ArrayList<Attribute> attrs;
	public final int dim;
	public final long min_row_id, max_row_id;
	public final double [] lowerB, upperB;
	
	public TableAttrStat (SystemConfig config, Context context) throws Exception {
		this.tbl_name = config.tbl_name;
		this.attrs = config.where_attrs;
		this.dim = config.where_attrs.size();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select min(row_id),max(row_id)");
		for(Attribute a:attrs) {
			sb.append(",min(");
			sb.append(a.name());
			sb.append("),max(");
			sb.append(a.name());
			sb.append(')');
		}
		sb.append(" from ");
		sb.append(config.tbl_name);
		sb.append(';');
		String sql = sb.toString();
		
		ResultSet rs = context.stmt.executeQuery(sql);
		rs.next();
		min_row_id = rs.getLong(1);
		max_row_id = rs.getLong(2);
		lowerB = new double [dim];
		upperB = new double [dim];
		for(int i=0; i<dim; i++) {
			lowerB[i] = rs.getDouble((i+1)*2+1);
			if(config.lowerBound!=null) {
				lowerB[i] = Math.max(lowerB[i], config.lowerBound[i]);
			}
			upperB[i] = rs.getDouble((i+1)*2+2);
			if(config.upperBound!=null) {
				upperB[i] = Math.min(upperB[i], config.upperBound[i]);
			}
			if(lowerB[i]>upperB[i]) {
				throw new Exception("The lower bound " + lowerB[i] + " is greater than the upper bound " + upperB[i]);
			}
		}
	}
}
