package middleware.sampleacquisition;

import java.util.ArrayList;

import weka.core.Attribute;

public class SampingSQL {
	
	public static String equiwidth(String tbl_name, String key_attr, ArrayList<String> select_attrs, ArrayList<Attribute> where_attrs,
			double [] lowerB, double [] upperB, int [] numBins, int numSamplesPerBin) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("select * from ( ");
		
			sb.append("select * , row_number() over ( partition by ");
			for(int i=0; i<where_attrs.size(); i++) {
				sb.append("grp_");
				sb.append(i+1);
				if(i!=where_attrs.size()-1) {
					sb.append(',');
				}
				else {
					sb.append(' ');
				}
			}
			sb.append("order by random() ) as rn from ( ");
			
				// start of innermost sub-query
				sb.append("select ");
				sb.append(key_attr);
				for(String s:select_attrs) {
					sb.append(',');
					sb.append(s);
				}
				for(Attribute a:where_attrs) {
					sb.append(',');
					sb.append(a.name());
				}
				for(int i=0; i<where_attrs.size(); i++) {
					sb.append(",width_bucket(");
					sb.append(where_attrs.get(i).name());
					sb.append(',');
					sb.append(lowerB[i]);
					sb.append(',');
					sb.append(upperB[i]);
					sb.append(',');
					sb.append(numBins[i]);
					sb.append(") as grp_");
					sb.append(i+1);
				}
				sb.append(" from ");
				sb.append(tbl_name);
				sb.append(" where ");
				for(int i=0; i<where_attrs.size(); i++) {
					sb.append(where_attrs.get(i).name());
					sb.append(">=");
					sb.append(lowerB[i]);
					sb.append(" and ");
					sb.append(where_attrs.get(i).name());
					sb.append('<');
					sb.append(upperB[i]);
					if(i!=where_attrs.size()-1) {
						sb.append(" and ");
					}
				}
				// end of innermost sub-query
			
			sb.append(" ) as sub1");
		sb.append(" ) as sub2 where rn <=");
		sb.append(numSamplesPerBin);
		sb.append(';');
		
		return sb.toString();
	}
	
	public static String equidepth(String tbl_name, String key_attr, ArrayList<String> select_attrs, ArrayList<Attribute> where_attrs, 
			double [] lowerB, double [] upperB, int [] numBins, int numSamplesPerBin) {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ( ");
		
		sb.append("select * , row_number() over ( partition by ");
		for(int i=0; i<where_attrs.size(); i++) {
			sb.append("grp_");
			sb.append(i+1);
			if(i!=where_attrs.size()-1) {
				sb.append(',');
			}
			else {
				sb.append(' ');
			}
		}
		sb.append("order by random() ) as rn from ( ");
		
			// start of innermost sub-query
			sb.append("select ");
			sb.append(key_attr);
			for(String s:select_attrs) {
				sb.append(',');
				sb.append(s);
			}
			for(Attribute a:where_attrs) {
				sb.append(',');
				sb.append(a.name());
			}
			for(int i=0; i<where_attrs.size(); i++) {
				sb.append(",ntile(");
				sb.append(numBins[i]);
				sb.append(") over (order by ");
				sb.append(where_attrs.get(i).name());
				sb.append(") as grp_");
				sb.append(i+1);
			}
			sb.append(" from ");
			sb.append(tbl_name);
			sb.append(" where ");
			for(int i=0; i<where_attrs.size(); i++) {
				sb.append(where_attrs.get(i).name());
				sb.append(">=");
				sb.append(lowerB[i]);
				sb.append(" and ");
				sb.append(where_attrs.get(i).name());
				sb.append('<');
				sb.append(upperB[i]);
				if(i!=where_attrs.size()-1) {
					sb.append(" and ");
				}
			}
			// end of innermost sub-query
		
		sb.append(" ) as sub1");
	sb.append(" ) as sub2 where rn <=");
	sb.append(numSamplesPerBin);
	sb.append(';');
		return sb.toString();
	}
	
	public static String allSamples(String tbl_name, String key_attr, ArrayList<Attribute> where_attrs) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("select ");
		sb.append(key_attr);
		for(int i=0; i<where_attrs.size()-1; i++) {
			sb.append(',');
			sb.append(where_attrs.get(i).name());
		}
		sb.append(" from ");
		sb.append(tbl_name);
		sb.append(';');
		
		return sb.toString();
	}
}
