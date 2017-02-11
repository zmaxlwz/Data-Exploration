package middleware.datastruc;

import weka.core.DenseInstance;

public class DenseInstanceWithID extends DenseInstance{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final long id;
	final double [] additionalAttributeValues;
	public DenseInstanceWithID(int numAttrs, long id) {
		super(numAttrs);
		this.id = id;
		additionalAttributeValues = null;
	}
	public DenseInstanceWithID(int numAttrs, long id, double [] additionalAttributeValues) {
		super(numAttrs);
		this.id = id;
		this.additionalAttributeValues = additionalAttributeValues;
	}
	public DenseInstanceWithID(DenseInstanceWithID old) {
		super(old);
		this.id = old.getID();
		this.additionalAttributeValues = old.getAdditionalAttributeValues();
	}
	
	public long getID() {
		return id;
	}
	
	public double [] getAdditionalAttributeValues() {
		return additionalAttributeValues;
	}
	
	public String toString() {
		return id + "," + super.toString();
	}
	
	public Object copy() {
		DenseInstanceWithID result = new DenseInstanceWithID(this);
		result.m_Dataset = m_Dataset;
		return result;
	}
	
	public boolean equals(Object a) {
        if(this==a) {
            return true;
        }
        if(a==null || this.getClass()!=a.getClass()) {
            return false;
        }
        DenseInstanceWithID _a = (DenseInstanceWithID)a;
        if(numAttributes()!=_a.numAttributes()) {
        	return false;
        }
        for(int i=0; i<numAttributes()-1; i++) {
            if(value(i)!=_a.value(i)) {
                return false;
            }
        }
        return true;
    }
    public int hashCode() {
        int result = 0;
        for(int i=0; i<numAttributes()-1; i++) {
            result = 31*result + Double.valueOf(value(i)).hashCode();
        }
        return result;
    }
}
