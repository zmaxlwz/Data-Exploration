package middleware.explore;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

import SVMAlgos.libsvm.svm_model;
import middleware.SVMMiddleWare;
import middleware.UserModel;
import middleware.datastruc.DenseInstanceWithID;
import middleware.datastruc.ObjectWithDistance;
import middleware.sampleacquisition.Sampling;
import middleware.user.User;
import system.Context;
import system.SystemConfig;
import weka.core.Instances;

public class RandomSampling extends SVMMiddleWare {

	Random r;
	PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> topK;
	
	public RandomSampling(SystemConfig config, Context context, User user) throws Exception {
		super(config, context, user);
		r = new Random();
		
		topK = new PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> (1,new Comparator<ObjectWithDistance<DenseInstanceWithID>>() {
		    public int compare(ObjectWithDistance<DenseInstanceWithID> n1, ObjectWithDistance<DenseInstanceWithID> n2) {
		        double d1 = n1.getDistance();
		        double d2 = n2.getDistance();
		        if(d1<d2) {
		        	return 1;
		        }
		        else if(d1==d2) {
		        	return 0;
		        }
		        else {
		        	return -1;
		        }
		    }
		});
	}

	@Override
	public Instances explore(UserModel model) throws Exception {
		topK.clear();
		svm_model svmModel = (svm_model)model;
		
		long t1 = System.currentTimeMillis();
		
		HashSet<DenseInstanceWithID> samples = Sampling.randomSampling(config, context, stat, config.random_sample_count_per_iteration);
		
		long t2 = System.currentTimeMillis();
		
		for(DenseInstanceWithID sample:samples) {
	    	topK.add(new ObjectWithDistance<DenseInstanceWithID>(Math.abs(f_x(svmModel, (DenseInstanceWithID)sample)),(DenseInstanceWithID)sample));
	    	
	    	if(topK.size()>config.samples_per_round) {
	    		topK.remove();
	    	}			
		}
		
		long t3 = System.currentTimeMillis();
		
		if(pw_accuracy_profile!=null) {
	    	pw_accuracy_profile.println("number of tuples loaded: " + config.random_sample_count_per_iteration + " queryTime: " + (t2-t1) + " evalTime: " + (t3-t2));
	    	pw_accuracy_profile.flush();
	    }
		
		if(config.debug) {
			System.out.println("number of tuples loaded: " + config.random_sample_count_per_iteration + " queryTime: " + (t2-t1) + " evalTime: " + (t3-t2));
		}
		
		Instances labeledSamples = new Instances(null, config.where_attrs_with_class, 0);				
		labeledSamples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		HashSet<DenseInstanceWithID> filtered = new HashSet<DenseInstanceWithID>();
		
		Iterator<ObjectWithDistance<DenseInstanceWithID>> itr_topK = topK.iterator();
		while(itr_topK.hasNext()) {
			DenseInstanceWithID sample = itr_topK.next().getObject();						
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
			
			if(pw_sample!=null) {
				pw_sample.flush();
			}		
			return labeledSamples;
		}
	}

}
