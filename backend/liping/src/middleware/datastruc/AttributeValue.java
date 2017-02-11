package middleware.datastruc;

public class AttributeValue implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int index;
	public double value;
	public AttributeValue() {
		
	}
	public AttributeValue(int index, double value) {
		this.index = index;
		this.value = value;
	}
	public String toString() {
		return index+":"+value;
	}
	
	public void set(int index, double value) {
		this.index = index;
		this.value = value;
	}
}
