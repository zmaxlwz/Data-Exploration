package metrics.changerate;

import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.special.Gamma;

public class SurfaceArea{

	static boolean DEBUG = false; // (SystemConfig.mode==SystemConfig.Mode.DEBUG);
	
	public static double hyperSphere(int dim, double radius) {
		return 2*Math.pow(Math.PI, dim*0.5)/Gamma.gamma(dim*0.5)*Math.pow(radius, dim-1);
	}
	
	
	public static double hyperSphericalCap(int dim, double radius, double colatitude) {
		if(colatitude<=Math.PI/2) {
			return 0.5*hyperSphere(dim,radius)*Beta.regularizedBeta(Math.pow(Math.sin(colatitude), 2), (dim-1)*0.5, 0.5);
		}
		else {
			return hyperSphere(dim,radius)*(1-0.5*Beta.regularizedBeta(Math.pow(Math.sin(colatitude), 2), (dim-1)*0.5, 0.5));
		}
	}
	
/*	private static class IntegralFunction implements UnivariateFunction {
		final int dim;
		final double angle;
		public IntegralFunction (int dim, double angle, double radius) {
			this.dim = dim;
			this.angle = angle;
		}
		@Override
		public double value(double phi) {
			return Math.pow(Math.sin(phi), dim-2)*Beta.regularizedBeta(1-Math.pow(Math.tan(angle)/Math.tan(phi), 2), (dim-2)*0.5, 0.5);
		}
	}*/
	
	/**
	 * 
	 * @param dim
	 * @param radius
	 * @param lowerAngle
	 * @param upperAngle
	 * @return the surface area of a hyperspherical cap cut by a hyper plane
	 */
	private static double JFunction(final int dim, final double radius, final double lowerAngle, final double upperAngle) {
		if(lowerAngle<0 || lowerAngle>Math.PI/2 || upperAngle<0 || upperAngle>Math.PI/2) {
			System.err.println("Error in JFunction");
		}
		double dAngle = (upperAngle-lowerAngle)/10000;
		double sum = 0;
		for(double angle=lowerAngle+dAngle; angle<=upperAngle-dAngle; angle=angle+dAngle) {
			double I = Beta.regularizedBeta(1-Math.pow(Math.tan(lowerAngle)/Math.tan(angle), 2), (dim-2)*0.5, 0.5);
			double J = Math.pow(Math.sin(angle), dim-2)*I;
			sum += J*dAngle;
		}
		return sum*Math.pow(Math.PI, (dim-1)*0.5)*Math.pow(radius, dim-1)/Gamma.gamma((dim-1)*0.5);
	}
	
	/**
	 * w1*x-b1=0 and w2*x-b2=0
	 * @param radius
	 * @param w1
	 * @param b1
	 * @param w2
	 * @param b2
	 * @return the surface area of the differences of two hyperspherical caps 
	 * @throws Exception 
	 */
	public static double differenceTwoHyperPlanesOnSphere(double radius, RealVector w1, double b1, RealVector w2, double b2) throws Exception {
		if(w1.getDimension()!=w2.getDimension()) {
			throw new Exception("dimensionalities do not match: "+ w1.getDimension() + " vs " + w2.getDimension());
		}
		int dim = w1.getDimension();
		
		double dist1 = b1/w1.getNorm();
		double colatitude1 = Math.acos(dist1/radius);		
		double dist2 = b2/w2.getNorm();
		double colatitude2 = Math.acos(dist2/radius);
		
		double area1 = hyperSphericalCap(dim, radius, colatitude1);
		double area2 = hyperSphericalCap(dim, radius, colatitude2);
		double difference = area1 + area2 - 2*intersectionTwoHyperSphericalCaps(dim, radius, w1, colatitude1, w2, colatitude2);
		double whole = hyperSphere(dim,radius);
//		System.err.println("dim=" + dim + " w1=" + w1.toString() + " b1=" + b1 + " w2=" + w2.toString() + " b2=" + b2+
//				" theta1=" + colatitude1 + " theta2=" + colatitude2 + 
//				" dist1=" + dist1 + " dist2=" + dist2 +
//				": " + area1 + " " + area2 + " " + difference + " " + whole);
		
		return (difference)/whole;
	}
	/**
	 * Based on paper "Concise Formulas for the Surface Area of the Intersection of Two HyperSpherical Caps"
	 * By Yongjae Lee and Woo Chang Kim
	 * KAIST technical report
	 * 
	 * @param dim (cases when dim=2 are processed differently with cases when dim>2)
	 * @param radius
	 * @param axis1
	 * @param colatitude1 in [0,PI]
	 * @param axis2
	 * @param colatitude2 in [0,PI]
	 * @return
	 */
	public static double intersectionTwoHyperSphericalCaps(int dim, double radius, RealVector axis1, double colatitude1, RealVector axis2, double colatitude2) {
		// the angle between the two axes
		double angle = Math.acos(axis1.dotProduct(axis2)/axis1.getNorm()/axis2.getNorm());
		
		if(dim==2) {
			if(angle>=colatitude1+colatitude2) {
				return 0;
			}
			else {
				if(colatitude1>=Math.min(colatitude2+angle, Math.PI)) {
					return 2*colatitude2*radius;
				}
				else if(colatitude2>=Math.min(colatitude1+angle, Math.PI)) {
					return 2*colatitude1*radius;
				}
				else if(colatitude1+colatitude2>2*Math.PI-angle) {
					return 2*(colatitude1+colatitude2-Math.PI)*radius;
				}
				else {
					return (colatitude1+colatitude2-angle)*radius;
				}
			}
		}
		else {
			if(angle>=colatitude1+colatitude2) {
				if(DEBUG) {
					System.out.println("case 1");
				}
				return 0;
			}
			else {
				if(colatitude1>=Math.min(colatitude2+angle, Math.PI)) {
					if(DEBUG) {
						System.out.println("case 2");
					}
					return hyperSphericalCap(dim, radius, colatitude2);
				}
				else if(colatitude2>=Math.min(colatitude1+angle, Math.PI)) {
					if(DEBUG) {
						System.out.println("case 3");
					}
					return hyperSphericalCap(dim, radius, colatitude1);
				}
				else if(colatitude1+colatitude2>2*Math.PI-angle) {
					if(colatitude1<=Math.PI/2) {
						if(DEBUG) {
							System.out.println("case 4");
						}
						return hyperSphericalCap(dim, radius, colatitude1) - hyperSphericalCap(dim, radius, Math.PI-colatitude2);
					}
					else { //colatitude1>Math.PI/2
						if(DEBUG) {
							System.out.println("case 5");
						}
						return hyperSphericalCap(dim, radius, colatitude2) - hyperSphericalCap(dim, radius, Math.PI-colatitude1);
					}
				}
				else { //colatitude1+colatitude2<=2*Math.PI-angle
					if(colatitude1<Math.PI/2) {
						if(colatitude2<Math.PI/2) {
							if(colatitude2>angle && Math.cos(colatitude1)*Math.cos(angle)>=Math.cos(colatitude2)) {
								if(DEBUG) {
									System.out.println("case 6");
								}
								double theta_min = Math.atan(1/Math.tan(angle)-Math.cos(colatitude2)/(Math.cos(colatitude1)*Math.sin(angle)));
								return hyperSphericalCap(dim, radius, colatitude1) 
										- JFunction(dim, radius, theta_min, colatitude1)
										+ JFunction(dim, radius, angle+theta_min, colatitude2);
							}
							else if(colatitude1>angle && Math.cos(colatitude2)*Math.cos(angle)>=Math.cos(colatitude1)) {
								if(DEBUG) {
									System.out.println("case 7");
								}
								double theta_min = Math.atan(1/Math.tan(angle)-Math.cos(colatitude1)/(Math.cos(colatitude2)*Math.sin(angle)));
								return hyperSphericalCap(dim, radius, colatitude2) 
										- JFunction(dim, radius, theta_min, colatitude2)
										+ JFunction(dim, radius, angle+theta_min, colatitude1);
							}
							else {
								if(DEBUG) {
									System.out.println("case 8");
								}
								double theta_min = Math.atan(Math.cos(colatitude1)/(Math.cos(colatitude2)*Math.sin(angle))-1/Math.tan(angle));
								return JFunction(dim, radius, angle-theta_min, colatitude1)
										+ JFunction(dim, radius, theta_min, colatitude2);
							}
						}
						else if(colatitude2==Math.PI/2) {
							if(angle<=Math.PI/2) {
								if(DEBUG) {
									System.out.println("case 9");
								}
								return hyperSphericalCap(dim, radius, colatitude1) 
										- JFunction(dim, radius, colatitude2-angle, colatitude1);
							}
							else { //angle>Math.PI/2
								if(DEBUG) {
									System.out.println("case 10");
								}
								return JFunction(dim, radius, angle-colatitude2, colatitude1);
							}
						}
						else { //colatitude2>Math.PI/2
							double _angle = Math.PI - angle;
							double _colatitude2 = Math.PI - colatitude2;
							if(_colatitude2>_angle && Math.cos(colatitude1)*Math.cos(_angle)>=Math.cos(_colatitude2)) {
								if(DEBUG) {
									System.out.println("case 11");
								}
								double theta_min = Math.atan(1/Math.tan(_angle)-Math.cos(_colatitude2)/(Math.cos(colatitude1)*Math.sin(_angle)));
								return JFunction(dim, radius, theta_min, colatitude1)
										- JFunction(dim, radius, _angle+theta_min, _colatitude2);
							}
							else if(colatitude1>_angle && Math.cos(_colatitude2)*Math.cos(_angle)>=Math.cos(colatitude1)) {
								if(DEBUG) {
									System.out.println("case 12");
								}
								double theta_min = Math.atan(1/Math.tan(_angle)-Math.cos(colatitude1)/(Math.cos(_colatitude2)*Math.sin(_angle)));
								return hyperSphericalCap(dim, radius, colatitude1)
										- hyperSphericalCap(dim, radius, _colatitude2)
										- JFunction(dim, radius, _angle+theta_min, colatitude1)
										+ JFunction(dim, radius, theta_min, _colatitude2);
							}
							else {
								if(DEBUG) {
									System.out.println("case 13");
								}
								double theta_min = Math.atan(Math.cos(colatitude1)/(Math.cos(_colatitude2)*Math.sin(_angle))-1/Math.tan(_angle));
								return hyperSphericalCap(dim, radius, colatitude1)
										- JFunction(dim, radius, theta_min, _colatitude2)
										+ JFunction(dim, radius, _angle-theta_min, colatitude1);
							}
						}
					}
					else if(colatitude1==Math.PI/2) {
						if(colatitude2<=Math.PI/2) {
							if(angle<=Math.PI/2) {
								if(DEBUG) {
									System.out.println("case 14");
								}
								return hyperSphericalCap(dim, radius, colatitude2)
										- JFunction(dim, radius, colatitude1-angle, colatitude2);
							}
							else { //angle>Math.PI/2
								if(DEBUG) {
									System.out.println("case 15");
								}
								return JFunction(dim, radius, angle-colatitude1, colatitude2);
							}
						}
						else { //colatitude2>Math.PI/2
							double _angle = Math.PI - angle;
							double _colatitude2 = Math.PI - colatitude2;
							if(_angle<=Math.PI/2) {
								if(DEBUG) {
									System.out.println("case 16");
								}
								return hyperSphericalCap(dim, radius, colatitude1)
										- hyperSphericalCap(dim, radius, _colatitude2)
										+ JFunction(dim, radius, colatitude1-_angle, _colatitude2);
							}
							else { //_angle>Math.PI/2
								if(DEBUG) {
									System.out.println("case 17");
								}
								return hyperSphericalCap(dim, radius, colatitude1)
										- JFunction(dim, radius, _angle-colatitude1, _colatitude2);
							}
						}
					}
					else { //colatitude1>Math.PI/2
						if(colatitude2<Math.PI/2) {
							double _angle = Math.PI - angle;
							double _colatitude1 = Math.PI - colatitude1;
							if(colatitude2>_angle && Math.cos(_colatitude1)*Math.cos(_angle)>=Math.cos(colatitude2)) {
								if(DEBUG) {
									System.out.println("case 18");
								}
								double theta_min = Math.atan(1/Math.tan(_angle)-Math.cos(colatitude2)/(Math.cos(_colatitude1)*Math.sin(_angle)));
								return hyperSphericalCap(dim, radius, colatitude2)
										- hyperSphericalCap(dim, radius, _colatitude1)
										+ JFunction(dim, radius, theta_min, _colatitude1)
										- JFunction(dim, radius, _angle+theta_min, colatitude2);
							}
							else if(_colatitude1>_angle && Math.cos(colatitude2)*Math.cos(_angle)>=Math.cos(_colatitude1)) {
								if(DEBUG) {
									System.out.println("case 19");
								}
								double theta_min = Math.atan(1/Math.tan(_angle)-Math.cos(_colatitude1)/(Math.cos(colatitude2)*Math.sin(_angle)));
								return JFunction(dim, radius, theta_min, colatitude2)
										- JFunction(dim, radius, _angle+theta_min, _colatitude1);
							}
							else {
								if(DEBUG) {
									System.out.println("case 20");
								}
								double theta_min = Math.atan(Math.cos(_colatitude1)/(Math.cos(colatitude2)*Math.sin(_angle))-1/Math.tan(_angle));
								return hyperSphericalCap(dim, radius, colatitude2)
										- JFunction(dim, radius, _angle-theta_min, _colatitude1)
										- JFunction(dim, radius, theta_min, colatitude2);
							}
						}
						else if(colatitude2==Math.PI/2) {
							double _angle = Math.PI - angle;
							double _colatitude1 = Math.PI - colatitude1;
							if(_angle<=Math.PI/2) {
								if(DEBUG) {
									System.out.println("case 21");
								}
								return hyperSphericalCap(dim, radius, colatitude2)
										- hyperSphericalCap(dim, radius, _colatitude1)
										+ JFunction(dim, radius, colatitude2-_angle, _colatitude1);
							}
							else { //_angle>Math.PI/2
								if(DEBUG) {
									System.out.println("case 22");
								}
								return hyperSphericalCap(dim, radius, colatitude2)
										- JFunction(dim, radius, _angle-colatitude2, _colatitude1);
							}
						}
						else { // colatitude2>Math.PI/2
							double _colatitude1 = Math.PI - colatitude1;
							double _colatitude2 = Math.PI - colatitude2;
							if(_colatitude2>angle && Math.cos(_colatitude1)*Math.cos(angle)>=Math.cos(_colatitude2)) {
								if(DEBUG) {
									System.out.println("case 23");
								}
								double theta_min = Math.atan(1/Math.tan(angle)-Math.cos(_colatitude2)/(Math.cos(_colatitude1)*Math.sin(angle)));
								return hyperSphere(dim, radius)
										- hyperSphericalCap(dim, radius, _colatitude2)
										+ JFunction(dim, radius, angle+theta_min, _colatitude2)
										- JFunction(dim, radius, theta_min, _colatitude1);
							}
							else if(_colatitude1>angle && Math.cos(_colatitude2)*Math.cos(angle)>=Math.cos(_colatitude1)) {
								if(DEBUG) {
									System.out.println("case 24");
								}
								double theta_min = Math.atan(1/Math.tan(angle)-Math.cos(_colatitude1)/(Math.cos(_colatitude2)*Math.sin(angle)));
								return hyperSphere(dim, radius)
										- hyperSphericalCap(dim, radius, _colatitude1)
										+ JFunction(dim, radius, angle+theta_min, _colatitude1)
										- JFunction(dim, radius, theta_min, _colatitude2);
							}
							else {
								if(DEBUG) {
									System.out.println("case 25");
								}
								double theta_min = Math.atan(Math.cos(_colatitude1)/(Math.cos(_colatitude2)*Math.sin(angle))-1/Math.tan(angle));
								return hyperSphere(dim, radius)
										- hyperSphericalCap(dim, radius, _colatitude1)
										- hyperSphericalCap(dim, radius, _colatitude2)
										+ JFunction(dim, radius, angle-theta_min, _colatitude1)
										+ JFunction(dim, radius, theta_min, _colatitude2);
							}
						}
					}
				}
			}
		}
	}
}
