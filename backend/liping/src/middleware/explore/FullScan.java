package middleware.explore;

import java.util.Iterator;

import middleware.SVMMiddleWare;
import middleware.UserModel;
import middleware.user.User;
import SVMAlgos.libsvm.svm_model;
import system.Context;
import system.SystemConfig;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class FullScan extends SVMMiddleWare {

	public FullScan(SystemConfig config, Context context, User user) throws Exception {
		super(config, context, user);
	}

	@Override
	public Instances explore(UserModel model) throws Exception {
		svm_model svmModel = (svm_model)model;
		
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
		
		boolean notfirst = false;
		/*Iterator<Long> itr = user.getLabeledKeys().iterator();
		while(itr.hasNext()) {
			if(notfirst) {
				sb.append(" and ");
				notfirst = true;
			}
			sb.append("row_id");
			sb.append("<>");
			sb.append(itr.next());
		}*/
		Iterator<Instance> itr = user.getLabeledSamples().iterator();
		while(itr.hasNext()) {
			if(notfirst) {
				sb.append(" and ");
				notfirst = true;
			}
			sb.append("not (");
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
					
		return getSamples(svmModel, sb.toString());
	}

}
