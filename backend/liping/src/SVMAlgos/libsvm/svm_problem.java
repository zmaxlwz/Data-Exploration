package SVMAlgos.libsvm;

import middleware.datastruc.AttributeValue;

//import middleware.AttributeValue;

public class svm_problem implements java.io.Serializable
{
	public int l;
	public double[] y;
	public AttributeValue[][] x;
}
