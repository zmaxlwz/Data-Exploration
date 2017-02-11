package utility;

import java.io.IOException;

public class LinearAlgebra {
	public static double innerProduct (double [] v1, double [] v2) throws IOException {
		if(v1.length!=v2.length) {
			throw new IOException(v1.length + "\t" + v2.length);
		}
		double innerProduct = 0;
		for(int i=0; i<v1.length; i++) {
			innerProduct += v1[i]*v2[i];
		}
		return innerProduct;
	}
	
	public static double l2(double [] v) throws IOException {
		if(v==null) {
			throw new IOException("NULL");
		}
		double l2 = 0;
		for(int i=0; i<v.length; i++) {
			l2 += v[i]*v[i];
		}
		return Math.sqrt(l2);
	}
	
	public static double[][] invertMatrix(double [][] a) throws IOException {
		int n = a.length;
        double x[][] = new double[n][n];
        double b[][] = new double[n][n];
        int index[] = new int[n];
        for (int i=0; i<n; ++i) 
            b[i][i] = 1;
 
        // Transform the matrix into an upper triangle
        gaussian(a, index);
 
        // Update the matrix b[i][j] with the ratios stored
        for (int i=0; i<n-1; ++i)
            for (int j=i+1; j<n; ++j)
                for (int k=0; k<n; ++k)
                    b[index[j]][k]-= a[index[j]][i]*b[index[i]][k];
 
        // Perform backward substitutions
        for (int i=0; i<n; ++i) 
        {
        	if(a[index[n-1]][n-1]==0) {
        		throw new IOException("not invertible");
        	}
            x[n-1][i] = b[index[n-1]][i]/a[index[n-1]][n-1];
            for (int j=n-2; j>=0; --j) 
            {
                x[j][i] = b[index[j]][i];
                for (int k=j+1; k<n; ++k) 
                {
                    x[j][i] -= a[index[j]][k]*x[k][i];
                }
                if(a[index[j]][j]==0) {
                	throw new IOException("not invertible");
                }
                x[j][i] /= a[index[j]][j];
            }
        }
        return x;
	}
	
	private static void gaussian(double a[][], int index[]) {
        int n = index.length;
        double c[] = new double[n];
 
        // Initialize the index
        for (int i=0; i<n; ++i) 
            index[i] = i;
 
        // Find the rescaling factors, one from each row
        for (int i=0; i<n; ++i) 
        {
            double c1 = 0;
            for (int j=0; j<n; ++j) 
            {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }
 
        // Search the pivoting element from each column
        int k = 0;
        for (int j=0; j<n-1; ++j) 
        {
            double pi1 = 0;
            for (int i=j; i<n; ++i) 
            {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) 
                {
                    pi1 = pi0;
                    k = i;
                }
            }
 
            // Interchange rows according to the pivoting order
            int itmp = index[j];
            index[j] = index[k];
            index[k] = itmp;
            for (int i=j+1; i<n; ++i) 	
            {
                double pj = a[index[i]][j]/a[index[j]][j];
 
                // Record pivoting ratios below the diagonal
                a[index[i]][j] = pj;
 
                // Modify other elements accordingly
                for (int l=j+1; l<n; ++l)
                    a[index[i]][l] -= pj*a[index[j]][l];
            }
        }
    }
	
	public static double [] multiply(double [][] matrix, double [] vector) throws IOException {
		if(matrix[0].length!=vector.length) {
			throw new IOException("cannot multiply a " + matrix.length + "x" + matrix[0].length + " matrix with a " + vector.length + "x1 vector");
		}
		double [] res = new double [matrix.length];
		for(int i=0; i<matrix.length; i++) {
			res[i] = 0;
			for(int j=0; j<matrix[0].length; j++) {
				res[i] = res[i] + matrix[i][j]*vector[j];
			}
		}
		return res;
	}

	public static double determinant(double [][] matrix) throws IOException {
		if (matrix.length!=matrix[0].length)
	        throw new IOException("matrix need to be square.");
		int n = matrix.length;
	    if (n == 1) {
	    	return matrix[0][0];
	    }
	    if (n==2) {
	        return (matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]);
	    }
	    double sum = 0.0;
	    for (int i=0; i<n; i++) {
	        sum += ((i&1)==0?1:-1) * matrix[0][i] * determinant(createSubMatrix(matrix, 0, i));
	    }
	    return sum;
	}
	
	public static double[][] createSubMatrix(double [][] matrix, int excluding_row, int excluding_col) {
	    double[][] mat = new double [matrix.length-1][matrix[0].length-1];
	    int r = -1;
	    for (int i=0;i<matrix.length;i++) {
	        if (i==excluding_row)
	            continue;
	        r++;
	        int c = -1;
	        for (int j=0;j<matrix[0].length;j++) {
	            if (j==excluding_col)
	                continue;
	            mat[r][++c]=matrix[i][j];
	        }
	    }
	    return mat;
	} 
	/*
	private static double[][] cofactor(double[][] matrix) throws IOException {
		double[][] mat = new double[matrix.length][matrix[0].length];
	    for (int i=0;i<matrix.length;i++) {
	        for (int j=0; j<matrix[0].length;j++) {
	            mat[i][j] = (i%2==0?1:-1) * (j%2==0?1:-1) * determinant(createSubMatrix(matrix, i, j));
	        }
	    }	    
	    return mat;
	}*/
}
