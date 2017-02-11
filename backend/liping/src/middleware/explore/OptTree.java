package middleware.explore;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

import SVMAlgos.libsvm.svm_model;
import middleware.SVMMiddleWare;
import middleware.UserModel;
import middleware.datastruc.DenseInstanceWithID;
import middleware.sampleacquisition.Sampling;
import middleware.user.User;
import system.Context;
import system.SystemConfig;
import trees.DeltaBoundRTree;
import weka.core.Instances;

public class OptTree extends SVMMiddleWare {
	DeltaBoundRTree tree;
	HashSet<DenseInstanceWithID> preSamples;	
	double threshold;

	public OptTree(SystemConfig config, Context context, User user) throws Exception {
		super(config, context, user);
		
		preSamples = Sampling.randomSampling(config, context, stat, config.opt_tree_presample_count);
		
		tree = new DeltaBoundRTree(config); 
	}

	@Override
	public Instances explore(UserModel model) throws Exception {
		svm_model svmModel = (svm_model)model;
		
		long t1 = System.currentTimeMillis();
		
		double threshold = Double.MAX_VALUE;
		
		for(DenseInstanceWithID preSample:preSamples) {
			threshold = Math.min(threshold, Math.abs(f_x(svmModel, preSample)));
		}
		
		long t2 = System.currentTimeMillis();
		
		HashSet<DenseInstanceWithID> samples = tree.branch_and_bound(svmModel, user.getLabeledSamples(), threshold); 
		
		long t3 = System.currentTimeMillis();
		Date d = new Date(t3);
		
		if(pw_accuracy_profile!=null) {
			pw_accuracy_profile.println(d.toString() + "compute threshold " + threshold + " in " + (t2-t1) + " ms. number of index nodes loaded: " 
				+ tree.nodeCount + " number of tuples retrieved: " + tree.tupleCount + " queryTime: " + (t3-t2));
		}
		
		Instances labeledSamples = new Instances(null, config.where_attrs_with_class, 0);				
		labeledSamples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		HashSet<DenseInstanceWithID> filtered = new HashSet<DenseInstanceWithID>();
		
		for(DenseInstanceWithID sample:samples) {						
			if(tsm==null || tsm.isUsefulSample(sample)) {
				filtered.add(sample);
			}
			else {
				labeledSamples.add(sample);
				if(pw_sample!=null) {
					pw_sample.println(new Date(System.currentTimeMillis()).toString() + ": " + sample.toString());
				}
			}
			
		}
		filtered = user.label(filtered, svmModel);
		if(filtered.size()==0) {
			return explore(svmModel);
		}
		else {
			for(DenseInstanceWithID sample:filtered) {
				labeledSamples.add(sample);
				if(pw_sample!=null) {
					pw_sample.println(new Date(System.currentTimeMillis()).toString() + ": " + sample.toString());
				}
			}
			
			if(labeledSamples.size()==0) {
				throw new IOException("0 samples retrieved");
			}
			
			if(pw_sample!=null) {
				pw_sample.flush();
			}	
			return labeledSamples;
		}
	}
}
