package middleware.user;

import java.io.IOException;
import java.util.HashSet;

import SVMAlgos.libsvm.svm_model;
import metrics.EvaluatingPoints;
import middleware.datastruc.DenseInstanceWithID;
import weka.core.Instance;

public interface User {
	/**
	 * Get the label for a specific sample. Only used in exploration phase
	 * Also pass the current model to the front end
	 * @param sample
	 * @return
	 * @throws IOException
	 */
//	public void setClass(Instance sample, svm_model model) throws IOException;
	/**
	 * Get the labels for a set of samples. Only used in initial sampling
	 * @param samples
	 * @return labeled samples
	 * @throws IOException
	 */
	public HashSet<DenseInstanceWithID> label(HashSet<DenseInstanceWithID> samples, svm_model model) throws Exception;
	
	public HashSet<Long> getLabeledKeys();
	public HashSet<Instance> getLabeledSamples();
	public int getPositiveCount();
	public int getNegativeCount();
	public void increasePosCount();
	public void increaseNegCount();
	
	public boolean isAlive();
	public void setVisualPoints(EvaluatingPoints visualPoints);
}
