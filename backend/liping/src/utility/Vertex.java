package utility;

import java.util.Arrays;


public class Vertex {
	
	public double [] vertex;
	public Vertex next;
	public Vertex(double [] point) {
		vertex = point;
		next = null;
	}
	public String toString() {
		return Arrays.toString(vertex);
	}

}
