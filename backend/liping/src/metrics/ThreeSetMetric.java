package metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import SVMAlgos.libsvm.Kernel;
import SVMAlgos.libsvm.svm_model;
import metrics.threeset.ConvexHull;
import metrics.threeset.PointWiseComplementConvexHull;
import middleware.datastruc.AttributeValue;
import system.SystemConfig;
import weka.core.Instance;
import weka.core.Instances;

public class ThreeSetMetric {	
//	// 2 dim
//	private boolean start = false;
//	ConvexPolygon posRegion;
//	ArrayList<NegRegion> negRegions;
	
	// multi-dimensional
	final int dim;
	ConvexHull positiveRegion;
	final ArrayList<PointWiseComplementConvexHull> negativeRegions;
	
	// for initialization
	double [][] pos;
	int posCount;
	ArrayList<double []> neg;
	boolean posInitialized = false;
	boolean negInitialized = false;
	
	// for evaluation	
	final EvaluatingPoints ep;
	final HashSet<Long> positiveSamples;
	final HashSet<Long> negativeSamples;
	final HashSet<Long> uncertainSamples;	
	public double value; 
	
	// for temporary uses
	double [] temp;
	
	public ThreeSetMetric(SystemConfig config, EvaluatingPoints ep) {
		dim = config.where_attrs.size();
		
		this.ep = ep;
		positiveSamples = new HashSet<Long>();
		negativeSamples = new HashSet<Long>();
		uncertainSamples = new HashSet<Long>(ep.points.keySet());	
		
		temp = new double [dim];
		
		pos = new double [dim+1][];
		posCount = 0;
		neg = new ArrayList<double []>();
		negativeRegions = new ArrayList<PointWiseComplementConvexHull>();
	}
	
	public void updateRatio(Instances labeledSamples) throws IOException {
		for(int s=0; s<labeledSamples.size(); s++) {
			Instance sample = labeledSamples.get(s);
			double [] point = new double [dim];
			for(int i=0; i<dim; i++) {
				point[i] = sample.value(i);
			}
			if(sample.classValue()>0) {
				if(!posInitialized) {
					pos[posCount++] = point;	
					// if this point is the (dim)-th positive sample and there already are some negative samples
					if(posCount==dim && !neg.isEmpty()) {
						// initialize the negative regions
						for(double [] negPoint:neg) {
							negativeRegions.add(new PointWiseComplementConvexHull(dim, negPoint, pos));
						}
						negInitialized = true;
						neg.clear();
					}					
					else if(posCount==dim+1){
						positiveRegion = new ConvexHull(dim,pos);
						posInitialized = true;
						if(negInitialized) {
							for(PointWiseComplementConvexHull nr:negativeRegions) {
								nr.addVertex(point);
							}
						}
					}
				}
				else {
					positiveRegion.addVertex(point);
					for(PointWiseComplementConvexHull nr:negativeRegions) {
						nr.addVertex(point);
					}
				}
			}
			else {
				if(!negInitialized) {
					if(posCount<dim) {
						neg.add(point);
					}
					else {
						System.out.println(Arrays.toString(pos));
						negativeRegions.add(new PointWiseComplementConvexHull(dim, point, pos));
						negInitialized = true;
					}
				}
				else {
					boolean createNew = true;
					for(PointWiseComplementConvexHull nr:negativeRegions) {
						if(nr.containsPoint(point)) {
							createNew = false;
							break;
						}
					}
					if(createNew) {
						if(positiveRegion!=null) {
							negativeRegions.add(new PointWiseComplementConvexHull(dim, point, positiveRegion));
						}
						else {
							negativeRegions.add(new PointWiseComplementConvexHull(dim, point, pos));
						}
					}
				}
			}
			for (Iterator<Long> i = uncertainSamples.iterator(); i.hasNext();) {
				Long key = i.next();
				double [] value = ep.points.get(key);
			    if (positiveRegion!=null && positiveRegion.containsPoint(value)) {
			        i.remove();
			        positiveSamples.add(key);
			    }
			    else {
			    	for(PointWiseComplementConvexHull nr:negativeRegions) {
			    		if(nr.containsPoint(value)) {
							i.remove();
							negativeSamples.add(key);
							break;
						}
			    	}
			    }
			}
			value = (double)positiveSamples.size()/(positiveSamples.size()+uncertainSamples.size());
			System.out.println(positiveSamples.size()+"\t"+negativeSamples.size() + "\t" + uncertainSamples.size() + "\t->\t" + (2*value/(1+value)) + "\n");
		}
	}
	
	public boolean estimateLabel(AttributeValue [] point, svm_model svmModel) throws IOException {
		if(point.length!=dim) {
			throw new IOException(point.length + "\t" + dim);
		}
		for(int i=0; i<dim; i++) {
			temp[i] = point[i].value;
		}
		if (positiveRegion!=null && positiveRegion.containsPoint(temp)) {
	    	return true;
	    }
	    
    	for(PointWiseComplementConvexHull nr:negativeRegions) {
    		if(nr.containsPoint(temp)) {
    			return false;
			}
    	}
    	// uncertain
    	double[] sv_coef = svmModel.sv_coef[0];
		double sum = 0;
		for(int j=0;j<svmModel.l;j++) {
			sum += sv_coef[j] * Kernel.k_function(point,svmModel.SV[j],svmModel.param);
		}
		sum -= svmModel.rho[0];			
    	if(sum>0) {
    		return true;
    	}
    	else {
    		return false;
    	}
	    
	}
	
	public boolean isUsefulSample(Instance sample) throws IOException {
		for(int i=0; i<dim; i++) {
			temp[i] = sample.value(i);
		}
		if (positiveRegion!=null && positiveRegion.containsPoint(temp)) {
			sample.setValue(sample.numAttributes()-1, 1);
	    	return false;
	    }
	    
    	for(PointWiseComplementConvexHull nr:negativeRegions) {
    		if(nr.containsPoint(temp)) {
    			sample.setValue(sample.numAttributes()-1, -1);
    			return false;
			}
    	}
	    
		return true;
	}
/*	public ThreeSetMetric(HashMap<Long, double []> samples) {
		posRegion = new ConvexPolygon();
		negRegions = new ArrayList<NegRegion>();
		
		this.samples = samples;
		
		positiveSamples = new HashSet<Long>();
		negativeSamples = new HashSet<Long>();
		uncertainSamples = new HashSet<Long>(samples.keySet());	
	}
	
	public void startMetric(Instances labeledSamples) throws IOException {
		double [] pos = null;
		double [] neg = null;
		for(int s=0; s<2; s++) {
			Instance sample = labeledSamples.get(s);
			if(sample.classValue()>0) {
				if(pos==null) {
					pos = new double [2];
					for(int i=0; i<2; i++) {
						pos[i] = sample.value(i);
					}
				}
				if(neg!=null) {
					startMetric(pos, neg);
				}
			}
			else {
				if(neg==null) {
					neg = new double [2];
					for(int i=0; i<2; i++) {
						neg[i] = sample.value(i);
					}
				}
				if(pos!=null) {
					startMetric(pos, neg);
				}
			}
		}
	}
	
	public void startMetric(double [] pos, double [] neg) throws IOException {
		posRegion.addVertex(pos);
		negRegions.add(new NegRegion(neg, pos, pos));
		value = (double)positiveSamples.size()/(positiveSamples.size()+uncertainSamples.size());
		start = true;
	}
	
	public int update2dimRatio(Instances labeledSamples, int start) throws IOException {
		int usefulSampleCount = 0;
		for(int s=start; s<labeledSamples.size(); s++) {
			Instance sample = labeledSamples.get(s);
			double [] point = new double [2];
			for(int i=0; i<2; i++) {
				point[i] = sample.value(i);
			}
			if(sample.classValue()>0) {
				if(update2dimRatio(point,true)) {
					usefulSampleCount ++;
				}
			}
			else {
				if(update2dimRatio(point,false)) {
					usefulSampleCount ++;
				}
			}
		}
		return usefulSampleCount;
	}
	public int update2dimRatio(Instances labeledSamples) throws IOException {
		return update2dimRatio(labeledSamples,0);
	}*/
	/**
	 * 
	 * @return whether the point is useful;
	 * @throws IOException 
	 */
/*	public boolean update2dimRatio(double [] point, boolean isPositive) throws IOException {
		if(!start) {
			throw new IOException("call startMetric first");
		}
		if(isPositive) {
			if(posRegion.containsPoint(point)) {
				System.err.println("the postive sample is useless");
				return false;
			}
			else {
				// affect positive region
				posRegion.addVertex(point);
				// affect negative region
				for(NegRegion nr:negRegions) {
					double angle0 = nr.computePositiveAngle(point,0);
					double angle1 = nr.computePositiveAngle(point,1);
					double max = Math.max(nr.angle, Math.max(angle0, angle1));
//					System.err.println("angle: " + angle0 + "\t" + angle1 + "\t" + nr.angle + "\t");
					if(max==angle0) {
						nr.replacePos(1,point);
//						System.err.println("replace 1");
					}
					else if(max==angle1) {
						nr.replacePos(0,point);
//						System.err.println("replace 0");
					}
				}
			}
		}
		else {
			boolean newNeg = true;
			for(NegRegion nr:negRegions) {
				if(nr.containsPoint(point)) {
					newNeg = false;
					System.err.println("the negative sample is useless");
					return false;
				}
			}
			if(newNeg) {
				// create a new negative region
				if(posRegion.vertexCount==1) {
					double [] pos = posRegion.head.next.vertex;
					negRegions.add(new NegRegion(point, pos, pos));
				}
				else {
					ArrayList<Vertex> edgesLeftToThePoint = posRegion.findEdgesToTheLeft(point);
					if(edgesLeftToThePoint.get(0)!=posRegion.head.next) {		
						negRegions.add(new NegRegion(point, edgesLeftToThePoint.get(0).vertex, 
								posRegion.getNextNode(edgesLeftToThePoint.get(edgesLeftToThePoint.size()-1)).vertex));	
					}
					else {
						boolean disconnect = false;
						for(int i=1; i<edgesLeftToThePoint.size(); i++) {
							if(edgesLeftToThePoint.get(i-1).next!=edgesLeftToThePoint.get(i)) {
								disconnect = true;
								negRegions.add(new NegRegion(point, edgesLeftToThePoint.get(i).vertex, 
										edgesLeftToThePoint.get(i-1).next.vertex));
								break;
							}
						}
						if(!disconnect) {
							negRegions.add(new NegRegion(point, posRegion.head.next.vertex, 
									edgesLeftToThePoint.get(edgesLeftToThePoint.size()-1).next.vertex));
						}
					}
				}
			}
		}

		for (Iterator<Long> i = uncertainSamples.iterator(); i.hasNext();) {
			Long key = i.next();
			double [] value = samples.get(key);
		    if (posRegion.containsPoint(value)) {
		        i.remove();
		        positiveSamples.add(key);
		    }
		    else {
		    	for(NegRegion nr:negRegions) {
		    		if(nr.containsPoint(value)) {
						i.remove();
						negativeSamples.add(key);
						break;
					}
		    	}
		    }
		}
		value = (double)positiveSamples.size()/(positiveSamples.size()+uncertainSamples.size());
		System.out.println(positiveSamples.size()+"\t"+negativeSamples.size() + "\t" + uncertainSamples.size() + "\t->\t" + (2*value/(1+value)) + "\n");
		return true;
	}*/

//	double [] temp = new double [2];
	
/*	public boolean estimateLabel(AttributeValue [] point, svm_model svmModel) throws IOException {
		for(int i=0; i<2; i++) {
			temp[i] = point[i].value;
		}
		if (posRegion.containsPoint(temp)) {
	    	return true;
	    }
	    else {
	    	for(NegRegion nr:negRegions) {
	    		if(nr.containsPoint(temp)) {
	    			return false;
				}
	    	}
	    	// uncertain
	    	double[] sv_coef = svmModel.sv_coef[0];
			double sum = 0;
			for(int j=0;j<svmModel.l;j++) {
				sum += sv_coef[j] * Kernel.k_function(point,svmModel.SV[j],svmModel.param);
			}
			sum -= svmModel.rho[0];			
	    	if(sum>0) {
	    		return true;
	    	}
	    	else {
	    		return false;
	    	}
	    }
	}
	
	public boolean isUsefulSample(Instance sample) throws IOException {
		for(int i=0; i<2; i++) {
			temp[i] = sample.value(i);
		}
		if (posRegion.containsPoint(temp)) {
			sample.setClassValue(1);
	    	return false;
	    }
	    else {
	    	for(NegRegion nr:negRegions) {
	    		if(nr.containsPoint(temp)) {
	    			sample.setClassValue(-1);
	    			return false;
				}
	    	}
	    }
		return true;
	}
	
	
	
	public static void main(String[] args) throws IOException {
		double [] lowerB = {0,0};
		double [] upperB = {1400,2000};
		int granularity = 500;
		int numPoints = granularity*granularity;
		
		HashMap<Long, double []> samples = new HashMap<Long, double []>();
		for(int i=0; i<numPoints; i++) {
			int _i = i;
			double [] grid = new double [2];
			for(int j=0; j<lowerB.length; j++) {
				grid[j] = lowerB[j]+(_i%granularity)*(upperB[j]-lowerB[j])/(granularity-1);
				_i = _i/granularity;
			}
//			if(grid[0]>600 && grid[0]<800 && grid[1]>900 && grid[1]<1100) {
				samples.put((long)i, grid);
//			}
		}
		ThreeSetMetric tsm = new ThreeSetMetric(samples,2);
//		BufferedReader br = new BufferedReader(new FileReader("071015-110805/samples_brute-force_1-c.txt"));
//		StringTokenizer st;
//		
//		st = new StringTokenizer(br.readLine(),",");		
//		st.nextToken();
//		double [] neg = new double [2];
//		neg[0] = Double.valueOf(st.nextToken());
//		neg[1] = Double.valueOf(st.nextToken());
//		
//		st = new StringTokenizer(br.readLine(),",");		
//		st.nextToken();
//		double [] pos = new double [2];
//		pos[0] = Double.valueOf(st.nextToken());
//		pos[1] = Double.valueOf(st.nextToken());
//		
//		tsm.startMetric(pos, neg);
//		System.out.println(tsm.value);
//		
//		String nextLine;
//		while((nextLine=br.readLine())!=null) {
//			st = new StringTokenizer(nextLine,",");		
//			st.nextToken();
//			double [] point = new double [2];
//			point[0] = Double.valueOf(st.nextToken());
//			point[1] = Double.valueOf(st.nextToken());
//			if(Integer.valueOf(st.nextToken())==1) {
//				tsm.update2dimRatio(point,true);
//			}
//			else {
//				tsm.update2dimRatio(point,false);
//			}
//			System.out.println((2*tsm.value/(1+tsm.value)));
//		}
//		br.close();	
		ArrayList<Attribute> where_attrs = new ArrayList<Attribute>();
		where_attrs.add(new Attribute("rowc"));
		where_attrs.add(new Attribute("colc"));
		where_attrs.add(new Attribute("class"));
		Instances examples = LoadSamples.loadFromFile(where_attrs, "071015-110805/samples_brute-force_1-c.txt", null);
		tsm.updateRatio(examples);
	}*/

}
