package metrics.threeset;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.util.Pair;

import utility.LinearAlgebra;

public class Facet {
	private final Vertex [] vertices;
	private final double [] coef;
	private double offset;
	/**
	 * Create a facet based on the vertices, and check the number of vertices against the dimensionality of the space
	 * @param dim the dimensionality of the space
	 * @param vertices the vertices that define the facet
	 * @throws IOException wrong number of vertices
	 */
	public Facet (int dim, Vertex [] vertices, Vertex ref, boolean isInside) throws IOException {
		if (vertices.length != dim) {
			throw new IOException("Expected number of vertices = "+dim+", but get "+vertices.length+" vertices");
		}
		this.vertices = vertices;
		Arrays.sort(this.vertices);			
		
		double [][] matrix = new double [dim][dim];
		Vertex v0 = vertices[0]; // the first vertex
		for(int j=0; j<dim; j++) {
			matrix[0][j] = - v0.getValues()[j];
		} 
		for(int i=1; i<dim; i++) {
			for(int j=0; j<dim; j++) {
				matrix[i][j] = vertices[i].getValues()[j] - v0.getValues()[j];
			} 
		}
		
		coef = new double [dim];
		double _offset = 0;
		for(int i=0; i<dim; i++) {
			coef[i] = ((i&1)==0?1:-1)*LinearAlgebra.determinant(LinearAlgebra.createSubMatrix(matrix, 0, i));
			_offset += matrix[0][i]*coef[i];
		}
		offset = _offset;
		
		if((isVisible(ref.getValues()) && isInside) || (!isVisible(ref.getValues()) && !isInside)) {
			for(int i=0;i<coef.length;i++) {
				coef[i] = -coef[i];
			}
			offset = -offset;
		}
	}
	
	public Facet (Ridge r, Vertex v, Vertex ref, boolean isInside) throws IOException {
		vertices = new Vertex[r.getVertices().length+1];
		for(int i=0; i<r.getVertices().length; i++) {
			vertices[i] = r.getVertices()[i];
		}
		vertices[r.getVertices().length] = v;
		Arrays.sort(this.vertices);			
		
		double [][] matrix = new double [vertices.length][vertices.length];
		Vertex v0 = vertices[0]; // the first vertex
		for(int j=0; j<vertices.length; j++) {
			matrix[0][j] = - v0.getValues()[j];
		} 
		for(int i=1; i<vertices.length; i++) {
			for(int j=0; j<vertices.length; j++) {
				matrix[i][j] = vertices[i].getValues()[j] - v0.getValues()[j];
			} 
		}
		coef = new double [vertices.length];
		double _offset = 0;
		for(int i=0; i<vertices.length; i++) {
			coef[i] = ((i&1)==0?1:-1)*LinearAlgebra.determinant(LinearAlgebra.createSubMatrix(matrix, 0, i));
			_offset += matrix[0][i]*coef[i];
		}
		offset = _offset;
		
		if((isVisible(ref.getValues()) && isInside) || (!isVisible(ref.getValues()) && !isInside)) {
			for(int i=0;i<coef.length;i++) {
				coef[i] = -coef[i];
			}
			offset = -offset;
		}
	}
	
	public Vertex [] getVertices() {
		return vertices;
	}
	
	public Ridge [] getRidge() throws IOException {
		Ridge [] ridges = new Ridge [vertices.length];			
		for(int i=0; i<vertices.length; i++) { // for each ridge, do not include vertex i
			Vertex [] ridge = new Vertex [vertices.length-1];
			int index = -1;
			for(int j=0; j<vertices.length; j++) {
				if(j==i) {
					continue;
				}
				ridge[++index] = vertices[j];
			}			
			ridges[i] = new Ridge(vertices.length, ridge);
		}
		return ridges;
	}
	
	@SuppressWarnings("unchecked")
	public Pair<Ridge,Vertex> [] getRidge(Vertex v) throws IOException {
		Pair<Ridge,Vertex> [] ridges = new Pair [vertices.length-1];
		int pos = -1;
		for(int i=0; i<vertices.length; i++) {
			Vertex vertex = vertices[i];
			if(vertex.equals(v)) {
				pos = i;
				break;
			}
		}
		if(pos==-1) {
			throw new IOException(v.toString() + " is not a vertex of facet: " + this.toString());
		}
		
		int ridgeCount = 0;
		for(int i=0; i<vertices.length; i++) {// for each ridge, do not include vertex i
			if(i!=pos) {
				Vertex [] ridge = new Vertex [vertices.length-1];
				int index = -1;
				for(int j=0; j<vertices.length; j++) {
					if(j==i) {
						continue;
					}
					ridge[++index] = vertices[j];
				}			
				ridges[ridgeCount] = new Pair<Ridge, Vertex>(new Ridge(vertices.length, ridge), vertices[i]);
				ridgeCount ++;
			}
		}		
		return ridges;
	}
	
	public boolean isVisible(double [] point) throws IOException {
		if(point.length!=coef.length) {
			throw new IOException("point dim = "+point.length+", facet dim ="+coef.length);
		}
		return LinearAlgebra.innerProduct(point, coef)+offset>0;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Vertex v:vertices) {
			sb.append(v.toString());
			sb.append("->");
		}
		sb.append(vertices[0]);
		sb.append("\n");
		sb.append(Arrays.toString(coef));
		sb.append("\t");
		sb.append(offset);
		return sb.toString();
	}
	
	public int hashCode() {
		int result = 0;
		for(int i=0; i<vertices.length; i++) {
			result = result*31+vertices[i].hashCode();
		}
		return result;
	}
	
	public boolean equals(Object a) {
		if(this==a) {
            return true;
        }
        if(a==null || this.getClass()!=a.getClass()) {
            return false;
        }
        Facet _a = (Facet)a;
        for(int i=0; i<vertices.length; i++) {
        	if(vertices[i].compareTo(_a.vertices[i])!=0) {
        		return false;
        	}
        }
        return true;
	}
}
