package SVMAlgos;

import java.io.IOException;

//import middleware.AttributeValue;
//import middleware.Tuple;

public class Kernel {
	public static double GaussianKernel(double [] x, double [] y, double gamma) throws IOException {
		if(x.length!=y.length)
			throw new IOException("GaussianKernel: dimensionality does not match");
		double r = 0;
		for(int i=0; i<x.length; i++)
			r += (x[i] - y[i]) * (x[i] - y[i]);
		r *= gamma;
		return Math.exp(r);
	}
	
//	public static double GaussianKernel(double [] x, AttributeValue [] y, double gamma) throws IOException {
//		if(x.length!=y.length)
//			throw new IOException("GaussianKernel: dimensionality does not match");
//		double r = 0;
//		for(int i=0; i<x.length; i++)
//			r += (x[i] - y[i].value) * (x[i] - y[i].value);
//		r *= gamma;
//		return Math.exp(r);
//	}
	
//	public static double GaussianKernel(Tuple t1, Tuple t2, double gamma) throws IOException {
//		return GaussianKernel(t1.getValues(), t2.getValues(), gamma);
//	}
}
