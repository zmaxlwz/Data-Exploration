package tuplereader;

import weka.core.Instance;

public interface TupleReader<PK> {
	public Instance getNext();
	public void reset();
	public Instance getTuple(PK keyValue);
}
