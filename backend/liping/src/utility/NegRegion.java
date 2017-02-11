package utility;

import java.util.Arrays;

/**
 * One negative sample and two postive samples determine a negative region in 2-dim space.
 * The negative region is the circular sector bounded by two rays "pos[0] to neg" and "pos[1] to neg"
 * @author lppeng
 *
 */
public class NegRegion {
	public double [] neg;
	public double [][] pos; 
	
	double [] norm;
	public double angle;
	
	public NegRegion(double [] neg, double [] pos0, double [] pos1) {
		this.neg = neg;
		this.pos = new double [2][];
		pos[0] = pos0;
		pos[1] = pos1;
		norm = new double [2];
		norm[0] = distanceToNeg(pos[0]);
		norm[1] = distanceToNeg(pos[1]);
		angle = computeAngle();
	}
	
	private double distanceToNeg(double [] point) {
		return Math.sqrt((point[0]-neg[0])*(point[0]-neg[0])
				+(point[1]-neg[1])*(point[1]-neg[1]));
	}
	
	public double computeNegativeAngle(double [] point, int index) {
		return Math.acos(((point[0]-neg[0])*(neg[0]-pos[index][0])+(point[1]-neg[1])*(neg[1]-pos[index][1]))
				/distanceToNeg(point)/norm[index]);
	}
	
	public double computePositiveAngle(double [] point, int index) {
		return Math.acos(((point[0]-neg[0])*(pos[index][0]-neg[0])+(point[1]-neg[1])*(pos[index][1]-neg[1]))
				/distanceToNeg(point)/norm[index]);
	}
	
	private double computeAngle() {
		return Math.acos(Math.min(1, ((pos[0][0]-neg[0])*(pos[1][0]-neg[0])+(pos[0][1]-neg[1])*(pos[1][1]-neg[1]))
				/norm[0]/norm[1]));
	}
	
	public void replacePos(int index, double [] point) {
		pos[index] = point;
		norm[index] = distanceToNeg(point);
		angle = computeAngle();
	}
	
	public boolean containsPoint(double [] point) {			
		double angle0 = computeNegativeAngle(point,0);
		double angle1 = computeNegativeAngle(point,1);
		if(angle0+angle1>Math.PI) {
			return false;
		}
		double max = Math.max(angle0, angle1);		
//		System.out.println(angle/Math.PI + " vs " + angle0/Math.PI  + " " + angle1/Math.PI );
		if(angle>=max) {
			return true;
		}
		else {
			return false;
		}
	}
	public String toString() {
		return "neg: " + Arrays.toString(neg) + "\tpos: " + Arrays.toString(pos[0]) + ", " + Arrays.toString(pos[1]);
	}
	
	public static void main(String[] args) {
		double [] neg = {764.217529, 996.467102};
		double [] pos1 = {744.129517, 969.001099};
		double [] pos2 = {755.286011, 1056.82996};
		NegRegion nr = new NegRegion(neg, pos1, pos2);
		
		double [] point = {614.4288577154308, 981.9639278557114};
		System.out.println(nr.containsPoint(point));
	}

}
