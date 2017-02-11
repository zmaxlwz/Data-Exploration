package SVMAlgos.libsvm;

public abstract class QMatrix {
	abstract float[] get_Q(int column, int len);
	abstract double[] get_QD();
	abstract void swap_index(int i, int j);
};
