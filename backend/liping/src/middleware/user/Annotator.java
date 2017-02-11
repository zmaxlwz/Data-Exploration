package middleware.user;

import java.util.ArrayList;
import java.util.HashSet;

import SVMAlgos.libsvm.svm_model;
import metrics.EvaluatingPoints;
import middleware.datastruc.DenseInstanceWithID;
import weka.core.Attribute;
import weka.core.Instance;

public class Annotator implements User {
	final ArrayList<Attribute> attributesWithClass;
	final GroundTruth truth;
	final HashSet<Long> labeledKeys;
	final HashSet<Instance> labeledSamples;
	int trueCount = 0, falseCount = 0;
	
	public Annotator(ArrayList<Attribute> attributesWithClass, GroundTruth truth) {
		this.attributesWithClass = attributesWithClass;
		this.truth = truth;
		labeledKeys = new HashSet<Long>();
		labeledSamples = new HashSet<Instance>();
	}
	
/*	@Override
	public void setClass(Instance sample, svm_model model) {	// TODO: svm model
		int label = 0;
		if(truth.trueKeys.contains(((DenseInstanceWithID) sample).getID())) {
			label = 1;
			trueCount ++;
		}
		else {
			label = -1;
			falseCount ++;
		}
    	sample.setValue(sample.numAttributes()-1, label); 
    	labeledKeys.add(((DenseInstanceWithID)sample).getID());
    	labeledSamples.add(sample);
	}*/
	
	public void reset() {
		labeledKeys.clear();
		labeledSamples.clear();
		trueCount = 0;
		falseCount = 0;
	}

	@Override
	public HashSet<Long> getLabeledKeys() {
		return labeledKeys;
	}

	@Override
	public int getPositiveCount() {
		return trueCount;
	}

	@Override
	public int getNegativeCount() {
		return falseCount;
	}

	@Override
	public void increasePosCount() {
		trueCount++;
	}

	@Override
	public void increaseNegCount() {
		falseCount++;
	}

/*	@Override
	public Instances label(HashMap<Long, double[]> samples) throws IOException {
		Instances labeledSamples = new Instances(null, attributesWithClass, 0);
		labeledSamples.setClass(attributesWithClass.get(attributesWithClass.size()-1));
		Iterator<Long> itr = samples.keySet().iterator();
		while(itr.hasNext()) {
			long key = itr.next();
			double [] value = samples.get(key);
			
			Instance sample = new DenseInstanceWithID(attributesWithClass.size(),key);
			for(int j=0; j<value.length; j++) {
				sample.setValue(j, value[j]);
			}
			if(truth.trueKeys.contains(key)) {
				trueCount ++;
				sample.setValue(value.length, 1);
			}
			else {
				falseCount ++;
				sample.setValue(value.length, -1);
			}
			labeledKeys.add(key);
			this.labeledSamples.add(sample);
			labeledSamples.add(sample);
		}
		return labeledSamples;
	}*/

	@Override
	public HashSet<Instance> getLabeledSamples() {
		return labeledSamples;
	}

	@Override
	public boolean isAlive() {
		return true;
	}

	@Override
	public void setVisualPoints(EvaluatingPoints visualPoints) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HashSet<DenseInstanceWithID> label(HashSet<DenseInstanceWithID> samples, svm_model model) throws Exception {
		for(DenseInstanceWithID sample:samples) {	
			if(truth.trueKeys.contains(sample.getID())) {
				trueCount ++;
				sample.setValue(attributesWithClass.size()-1, 1);
			}
			else {
				falseCount ++;
				sample.setValue(attributesWithClass.size()-1, -1);
			}
			labeledKeys.add(sample.getID());
			labeledSamples.add(sample);
		}
		return samples;
	}
}
