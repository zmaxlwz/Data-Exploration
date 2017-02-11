package metrics;

import java.io.IOException;

import middleware.datastruc.AttributeValue;
import middleware.user.GroundTruth;
import SVMAlgos.libsvm.Kernel;
import SVMAlgos.libsvm.svm_model;

public class Accuracy {
	
	public static double evaluateF(GroundTruth truth, ThreeSetMetric tsm, svm_model svmModel) throws IOException {
		double tp=0, fp=0, fn = 0;
		for(int i=0;i<truth.allKeys.size();i++) {
			long key = truth.allKeys.get(i);
			AttributeValue [] x = truth.allTuples.get(i);
			if(tsm.estimateLabel(x, svmModel)) {
				if(truth.trueKeys.contains(key)) {
					tp ++;
				}
				else {
					fp ++;
				}
			}
			else {
				if(truth.trueKeys.contains(key)) {
					fn ++;
				}
			}
		}
			
	    double precision = tp/(tp+fp);
	    double recall = tp/(tp+fn);
	    System.out.println("tp=" + tp + "\tfp=" + fp + "\tfn=" + fn + "\tprecision=" + precision + "\trecall=" + recall);
	    return 2*precision*recall/(precision+recall);
	}
	
	public static double evaluateF(GroundTruth truth, svm_model svmModel) {
		double tp=0, fp=0, fn = 0;
		    	
    	for(int i=0;i<truth.allKeys.size();i++) {
			long key = truth.allKeys.get(i);
			AttributeValue [] x = truth.allTuples.get(i);
			
			double[] sv_coef = svmModel.sv_coef[0];
			double sum = 0;
			for(int j=0;j<svmModel.l;j++) {
				sum += sv_coef[j] * Kernel.k_function(x,svmModel.SV[j],svmModel.param);
			}
			sum -= svmModel.rho[0];
			
			if(sum>0) {
				if(truth.trueKeys.contains(key)) {
					tp ++;
				}
				else {
					fp ++;
				}
			}
			else {
				if(truth.trueKeys.contains(key)) {
					fn ++;
				}
			}
		}
			
	    double precision = tp/(tp+fp);
	    double recall = tp/(tp+fn);
	    System.out.println("tp=" + tp + "\tfp=" + fp + "\tfn=" + fn + "\tprecision=" + precision + "\trecall=" + recall);
	    return 2*precision*recall/(precision+recall);
		   
	}
}
