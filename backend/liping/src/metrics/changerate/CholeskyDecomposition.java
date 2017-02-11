package metrics.changerate;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import middleware.datastruc.AttributeValue;

public class CholeskyDecomposition {
	public ArrayList<ArrayList<Double>> positiveDefiniteMatrix;
	public ArrayList<ArrayList<Double>> lowerTriangleMatrix;
	
	public CholeskyDecomposition() {
		positiveDefiniteMatrix = new ArrayList<ArrayList<Double>>();
		lowerTriangleMatrix = new ArrayList<ArrayList<Double>>();
	}
	
	public RealVector getVector(int index) {
		RealVector v = new ArrayRealVector();
		for(int i=0; i<lowerTriangleMatrix.get(index).size(); i++) {
			v = v.append(lowerTriangleMatrix.get(index).get(i));
		}
		return v;
	}

	public ArrayList<ArrayList<Double>> addRow(ArrayList<Double> row) throws Exception {
		positiveDefiniteMatrix.add(row);
		lowerTriangleMatrix.add(new ArrayList<Double>());
		int n = row.size();
		for(int i=0; i<n-1; i++) {
			lowerTriangleMatrix.get(i).add(new Double(0));
		}
		
		for(int j=0; j<n-1; j++) {
			double a = positiveDefiniteMatrix.get(n-1).get(j);
			for(int k=0; k<j; k++) {
				a = a - lowerTriangleMatrix.get(n-1).get(k)*lowerTriangleMatrix.get(j).get(k);
			}
			lowerTriangleMatrix.get(n-1).add(a/lowerTriangleMatrix.get(j).get(j));
		}
		
		double a = positiveDefiniteMatrix.get(n-1).get(n-1);
		for(int k=0; k<n-1; k++) {
			a = a - Math.pow(lowerTriangleMatrix.get(n-1).get(k),2);
		}
		if(a>=0) {
			lowerTriangleMatrix.get(n-1).add(Math.sqrt(a));
		}
		else {
			throw new Exception("square root of negative " + a);
		}
		
		return lowerTriangleMatrix;
	}
	
	public static double [][] decompose(AttributeValue[][] positiveDefiniteMatrix, boolean print) {
		if(positiveDefiniteMatrix==null) {
			return null;
		}
		int n = positiveDefiniteMatrix.length;
		double [][] lowerTriangleMatrix = new double [n][n];
		for(int i=0; i<n; i++) {
			for(int j=0; j<n; j++) {
				if(i==j) {
					double a = positiveDefiniteMatrix[i][j].value;
					for(int k=0; k<j; k++) {
						a = a - lowerTriangleMatrix[j][k]*lowerTriangleMatrix[j][k];
					}
					lowerTriangleMatrix[i][j] = Math.sqrt(a); 
				}
				else if(i>j) {
					double a = positiveDefiniteMatrix[i][j].value;
					for(int k=0; k<j; k++) {
						a = a - lowerTriangleMatrix[i][k]*lowerTriangleMatrix[j][k];
					}
					lowerTriangleMatrix[i][j] = a/lowerTriangleMatrix[j][j];
				}
			}
		}
		if(print) {
			for(int i=0; i<n; i++) {
				System.out.println(Arrays.toString(lowerTriangleMatrix[i]));
			}
		}
		return lowerTriangleMatrix;
	}
	/**
	 * @param matrix: positive definite matrix
	 * @return
	 */
	public static double [][] decompose(double [][] positiveDefiniteMatrix, boolean print) {
		if(positiveDefiniteMatrix==null) {
			return null;
		}
		int n = positiveDefiniteMatrix.length;
		double [][] lowerTriangleMatrix = new double [n][n];
		for(int i=0; i<n; i++) {
			for(int j=0; j<n; j++) {
				if(i==j) {
					double a = positiveDefiniteMatrix[i][j];
					for(int k=0; k<j; k++) {
						a = a - lowerTriangleMatrix[j][k]*lowerTriangleMatrix[j][k];
					}
					lowerTriangleMatrix[i][j] = Math.sqrt(a); 
				}
				else if(i>j) {
					double a = positiveDefiniteMatrix[i][j];
					for(int k=0; k<j; k++) {
						a = a - lowerTriangleMatrix[i][k]*lowerTriangleMatrix[j][k];
					}
					lowerTriangleMatrix[i][j] = a/lowerTriangleMatrix[j][j];
				}
			}
		}
		if(print) {
			for(int i=0; i<n; i++) {
				System.out.println(Arrays.toString(lowerTriangleMatrix[i]));
			}
		}
		return lowerTriangleMatrix;
	}
	
	public static void main(String[] args) throws Exception {
		CholeskyDecomposition cd = new CholeskyDecomposition();
		ArrayList<Double> row1 = new ArrayList<Double>();
		row1.add(new Double(4));
		System.out.println(cd.addRow(row1));
		ArrayList<Double> row2 = new ArrayList<Double>();
		row2.add(new Double(-16));
		row2.add(new Double(98));
		System.out.println(cd.addRow(row2));
		ArrayList<Double> row3 = new ArrayList<Double>();
		row3.add(new Double(12));
		row3.add(new Double(-43));
		row3.add(new Double(37));
		System.out.println(cd.addRow(row3));
		
//		double [][] matrix = {{4,12,-16},{12,37,-43},{-16,-43,98}};
//		CholeskyDecomposition.decompose(matrix, true);
	}
}
