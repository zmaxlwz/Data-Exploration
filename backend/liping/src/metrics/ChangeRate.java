package metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math3.linear.RealVector;

import SVMAlgos.libsvm.Kernel;
import SVMAlgos.libsvm.svm_model;
import SVMAlgos.libsvm.svm_parameter;
import metrics.changerate.CholeskyDecomposition;
import metrics.changerate.SurfaceArea;
import middleware.SVMMiddleWare;
import middleware.datastruc.AttributeValue;
import weka.core.Instance;
import weka.core.Instances;

public class ChangeRate {
	final EvaluatingPoints ep;
	final svm_parameter param;
	
	CholeskyDecomposition cd;
	
	HashSet<Long> preTrueGrids;
	HashSet<Long> curTrueGrids;		
	
	RealVector preW, curW;
	double preB, curB;
	
	AttributeValue [] temp1, temp2;
	
	public double changeRate_stat, changeRate_closedForm;
	
	public ChangeRate(EvaluatingPoints ep, svm_parameter param, int dim) {
		this.ep = ep;
		this.param = param;
		
		cd = new CholeskyDecomposition();
		
		preTrueGrids = new HashSet<Long>();
		curTrueGrids = new HashSet<Long>();
		
		// initialize temp variable
		temp1 = new AttributeValue[dim]; 
		temp2 = new AttributeValue[dim];
		for(int i=0; i<dim; i++) {
			temp1[i] = new AttributeValue();
			temp2[i] = new AttributeValue();
		}
		
	}
	
	public void update(Instances oldSamples, Instances samples) throws Exception {
		for(int i=0; i<samples.size(); i++) {
			Instance sample1 = samples.get(i);
			for(int k=0; k<temp1.length; k++) {
				temp1[k].set(sample1.index(k), sample1.value(k)); 
			}
			ArrayList<Double> row = new ArrayList<Double>();
			for(int j=0; oldSamples!=null && j<oldSamples.size(); j++) {
				Instance sample2 = oldSamples.get(j);
				for(int k=0; k<temp2.length; k++) {
					temp2[k].set(sample2.index(k), sample2.value(k)); 
				}
				row.add(Kernel.k_function(temp1, temp2, param));
			}
			for(int j=0; j<i; j++) {
				Instance sample2 = samples.get(j);
				for(int k=0; k<temp2.length; k++) {
					temp2[k].set(sample2.index(k), sample2.value(k)); 
				}
				row.add(Kernel.k_function(temp1, temp2, param));
			}
			row.add(new Double(1));
			cd.addRow(row);
		}
	}
	
	public void updateChangeRate(svm_model model) throws Exception {
		// step 1: update curW and curB
		RealVector rv = cd.getVector(model.sv_indices[0]-1);
//		System.out.println("rv " + rv.toString());
		curW = rv.mapMultiply(model.sv_coef[0][0]);
		for(int i=1; i<model.l; i++) {
			rv = cd.getVector(model.sv_indices[i]-1);
//			System.out.println("rv " + rv.toString());
			curW = curW.add(rv.mapMultiply(model.sv_coef[0][i]));
		}
		curB = model.rho[0];
		
		// step 2: update change rate based on statistics
		curTrueGrids.clear();
		Iterator<Long> itr = ep.points.keySet().iterator();
		while(itr.hasNext()) {
			long key = itr.next();
			double [] value = ep.points.get(key);
			if(SVMMiddleWare.f_x(model, value)>0) {
				curTrueGrids.add(key);
			}
		}		
		
		Set<Long> union = new HashSet<Long>(preTrueGrids);
		union.addAll(curTrueGrids);
		Set<Long> intersection = new HashSet<Long>(preTrueGrids);
		intersection.retainAll(curTrueGrids);
		
		HashSet<Long> temp = preTrueGrids;
		preTrueGrids = curTrueGrids;
		curTrueGrids = temp;
		
		changeRate_stat = (double)(union.size()-intersection.size())/ep.points.size();
		
		// step 3: update change rate based on closed form
		if(preW!=null) {
			for(int i=0; i<curW.getDimension()-preW.getDimension(); i++) {
				preW = preW.append(0);
			}
			changeRate_closedForm = SurfaceArea.differenceTwoHyperPlanesOnSphere(1, preW, preB, curW, curB);
		}
		preW = curW;
		preB = curB;
	}
}
