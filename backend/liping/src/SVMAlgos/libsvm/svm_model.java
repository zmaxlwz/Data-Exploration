//
// svm_model
//
package SVMAlgos.libsvm;

import java.util.Arrays;
import java.util.StringTokenizer;

import middleware.UserModel;
import middleware.datastruc.AttributeValue;

public class svm_model implements java.io.Serializable, UserModel
{
	public svm_parameter param;	// parameter
	public int nr_class;		// number of classes, = 2 in regression/one class svm
	public int l;			// total #SV
	public int [] attrID;
	public AttributeValue[][] SV;	// SVs (SV[l])
	public double[][] sv_coef;	// coefficients for SVs in decision functions (sv_coef[k-1][l])
	public double[] rho;		// constants in decision functions (rho[k*(k-1)/2])
	public double[] probA;         // pairwise probability information
	public double[] probB;
	public int[] sv_indices;       // sv_indices[0,...,nSV-1] are values in [1,...,num_traning_data] to indicate SVs in the training set

	// for classification only

	public int[] label;		// label of each class (label[k])
	public int[] nSV;		// number of SVs for each class (nSV[k])
				// nSV[0] + nSV[1] + ... + nSV[k-1] = l
	@Override
	public String translateToSQL() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(param.toString());
		sb.append("\t");
		sb.append(l);
		sb.append("\t");
		sb.append(rho[0]);
		sb.append("\t");
		for(int i=0; i<l; i++) {
			sb.append(sv_coef[0][i]);
			sb.append(" ");
			sb.append(Arrays.toString(SV[i]));
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public svm_model() {
		
	}
	public svm_model(String str) {
		int dim = 2; // TODO: hardcode
		StringTokenizer st = new StringTokenizer(str, "\t:[], ");
		param = new svm_parameter(Integer.valueOf(st.nextToken()), Double.valueOf(st.nextToken()), 
				Double.valueOf(st.nextToken()), Double.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));
		System.out.println(param.toString());
		l = Integer.valueOf(st.nextToken());
		rho = new double [1];
		rho[0] = Double.valueOf(st.nextToken());
		sv_coef = new double [1][l];
		SV = new AttributeValue[l][];
		for(int i=0; i<l; i++) {
			sv_coef[0][i] = Double.valueOf(st.nextToken());
			SV[i] = new AttributeValue[dim];
			for(int j=0; j<dim; j++) {
				SV[i][j] = new AttributeValue(Integer.valueOf(st.nextToken()), Double.valueOf(st.nextToken()));
			}
		}
	}
};
