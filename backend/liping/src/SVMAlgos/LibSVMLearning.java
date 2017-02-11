package SVMAlgos;
import weka.core.Instance;
import weka.core.Instances;
import middleware.UserModel;
import middleware.datastruc.AttributeValue;
import SVMAlgos.libsvm.svm;
import SVMAlgos.libsvm.svm_parameter;
import SVMAlgos.libsvm.svm_problem;


public class LibSVMLearning implements SVMLearning{
	svm_parameter param;
	public LibSVMLearning (double gamma, double C) {
		param = new svm_parameter();
		param.gamma = gamma;
		param.C = C;
	}
	
	public svm_parameter getParam() {
		return param;
	}

	@Override
	public UserModel learn(Instances samples) throws Exception {
		// form the problem
		svm_problem prob = new svm_problem();
		prob.l = samples.numInstances();
		prob.x = new AttributeValue[prob.l][];
		prob.y = new double [prob.l];
		for(int i=0; i<prob.l; i++) {
			Instance sample = samples.instance(i);
			prob.x[i] = new AttributeValue [sample.numAttributes()-1];
			int index = 0;
			for(int j=0; j<sample.numAttributes(); j++) {
				if(j==sample.classIndex()) { // classIndex() is the index in the attrInfo
					continue;
				}
				prob.x[i][index] = new AttributeValue(sample.index(j), sample.value(j)); // TODO: check index(), value()
				index ++;
			}
			prob.y[i] = sample.value(sample.classIndex());
		}

		return svm.svm_train(prob,param);
	}
}
