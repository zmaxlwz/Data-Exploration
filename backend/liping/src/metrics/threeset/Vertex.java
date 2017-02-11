package metrics.threeset;

import java.io.IOException;
import java.util.Arrays;

public class Vertex implements Comparable<Vertex>{
	private final double [] values;
	/**
	 * Create a vertex based on the values on each dimension, and check the number of values against the dimensionality of the space
	 * @param dim the dimensionality of the space
	 * @param values the values on every dimension
	 * @throws IOException wrong number of values
	 */
	public Vertex (int dim, double [] values) throws IOException {
		if (values.length != dim) {
			throw new IOException("Expected dim = "+dim+", but get value dim = "+values.length);
		}
		this.values = values;
	}
	public double [] getValues () {
		return values;
	}
	
	@Override
	public int compareTo(Vertex o) {
		for(int i=0; i<values.length; i++) {
			if(this.values[i]>o.values[i]) {
				return 1;
			}
			else if(this.values[i]<o.values[i]) {
				return -1;
			}
		}
		return 0;
	}
	
	public int hashCode() {
		int result = 0;
		for(int i=0; i<values.length; i++) {
			result = result*31+Double.valueOf(values[i]).hashCode();
		}
		return result;
	}
	
	public String toString() {
		return Arrays.toString(values);
	}
}
