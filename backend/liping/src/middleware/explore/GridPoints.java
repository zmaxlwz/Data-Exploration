package middleware.explore;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;

import SVMAlgos.libsvm.svm_model;
import metrics.EvaluatingPoints;
import middleware.SVMMiddleWare;
import middleware.UserModel;
import middleware.user.User;
import system.Context;
import system.SystemConfig;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class GridPoints extends SVMMiddleWare{

	final PrintWriter queries;
	final EvaluatingPoints grids;
	public GridPoints(SystemConfig config, Context context, User user) throws Exception {
		super(config, context, user);
		if(config.evalMode!=SystemConfig.EvalMode.GRIDS) {
			throw new Exception("cannot use this exploration method");
		}
		if(context.truthPredicate!=null) {
			queries = new PrintWriter(new FileWriter("queries_"+context.truthPredicate));
		}
		else {
			queries = new PrintWriter(new FileWriter("queries_"+System.currentTimeMillis()));
		}
		grids = new EvaluatingPoints(stat,config.dec_tree_grid_count);
	}

	@Override
	public Instances explore(UserModel model) throws Exception {
		svm_model svmModel = (svm_model)model;
		
		double minDist = Double.MAX_VALUE;
		long minID = 0;
		Iterator<Long> itr1 = grids.points.keySet().iterator();
		while(itr1.hasNext()) {
			long key = itr1.next();
			double dist = Math.exp(-f_x(svmModel, grids.points.get(key)));
			if(dist<minDist) {
				minDist = dist;
				minID = key;
			}			
		}		
		
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
		sb.append(" where ");
		
		int granularity = (int)Math.pow(grids.points.size(), 1.0/stat.dim);
		for(int i=0; i<config.where_attrs.size(); i++) {
			sb.append(config.where_attrs.get(i).name());
			sb.append('>');
			sb.append(Math.max(stat.lowerB[i], grids.points.get(minID)[i] - (stat.upperB[i]-stat.lowerB[i])/(granularity-1)));
			sb.append(" and ");
			sb.append(config.where_attrs.get(i).name());
			sb.append('<');
			sb.append(Math.min(stat.upperB[i], grids.points.get(minID)[i] + (stat.upperB[i]-stat.lowerB[i])/(granularity-1)));			
		}
		
		/*itr = user.getLabeledKeys().iterator();
		while(itr.hasNext()) {
			sb.append(" and row_id");
			sb.append("<>");
			sb.append(itr.next());
		}*/
		Iterator<Instance> itr = user.getLabeledSamples().iterator();
		while(itr.hasNext()) {
			sb.append(" and not (");
			Instance sample = itr.next();
			for(int i=0; i<config.where_attrs.size(); i++) {
				sb.append(config.where_attrs.get(i).name());
				sb.append('=');
				sb.append(sample.value(i));
				if(i!=config.where_attrs.size()-1) {
					sb.append(" and ");
				}
			}
			sb.append(')');
		}
		sb.append(';');
		sb.append(';');
		
		if(queries!=null) {
			queries.println(sb);
			queries.flush();
		}
		return getSamples(svmModel, sb.toString());
	}

}
