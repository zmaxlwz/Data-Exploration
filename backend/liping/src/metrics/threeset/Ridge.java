package metrics.threeset;

import java.io.IOException;
import java.util.Arrays;

public class Ridge {
	private final Vertex [] vertices;
	/**
	 * Create a ridge based on the vertices, and check the number of vertices against the dimensionality of the space
	 * @param dim the dimensionality of the space
	 * @param vertices the vertices that define the ridge
	 * @throws IOException wrong number of vertices
	 */
	public Ridge (int dim, Vertex [] vertices) throws IOException {
		if (vertices.length != dim-1) {
			throw new IOException("Expected number of vertices = "+(dim-1)+", but get "+vertices.length+" vertices");
		}
		this.vertices = vertices;
		Arrays.sort(this.vertices);	
	}
	public Vertex [] getVertices() {
		return vertices;
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
        Ridge _a = (Ridge)a;
        for(int i=0; i<vertices.length; i++) {
        	if(vertices[i].compareTo(_a.vertices[i])!=0) {
        		return false;
        	}
        }
        return true;
	}
	public String toString() {
		return Arrays.toString(vertices);
	}
}
