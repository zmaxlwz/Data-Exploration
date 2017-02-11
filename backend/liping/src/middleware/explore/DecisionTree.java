package middleware.explore;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.PriorityQueue;

import SVMAlgos.libsvm.svm_model;
import metrics.EvaluatingPoints;
import middleware.SVMMiddleWare;
import middleware.UserModel;
import middleware.datastruc.DenseInstanceWithID;
import middleware.user.User;
import system.Context;
import system.SystemConfig;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

@SuppressWarnings("deprecation")
public class DecisionTree extends SVMMiddleWare {

	final PrintWriter queries;
	final Instances internalSamples; 
	final EvaluatingPoints grids;

	final boolean selfAdaptive;
	double delta;
	int inCount, outCount;	
	
	PriorityQueue<Double> bigHeap;
	PriorityQueue<Double> smallHeap;	
	
	public DecisionTree(SystemConfig config, Context context, User user) throws Exception {
		super(config, context, user);
		if(context.truthPredicate!=null) {
			queries = new PrintWriter(new FileWriter("queries_"+context.truthPredicate));
		}
		else {
			queries = new PrintWriter(new FileWriter("queries_"+System.currentTimeMillis()));
		}
//		FastVector classValues = new FastVector(3);
//		classValues.addElement(String.valueOf(-1));
		FastVector<String> classValues = new FastVector<String>(2);
		classValues.addElement(String.valueOf(1));
		classValues.addElement(String.valueOf(0));

		internalSamples = new Instances(null, config.where_attrs_with_class, 0);
		internalSamples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		
		grids = new EvaluatingPoints(stat,config.dec_tree_grid_count);		
		Iterator<Long> itr = grids.points.keySet().iterator();
		while(itr.hasNext()) {
			long key = itr.next();
			double [] value = grids.points.get(key);
			Instance sample = new DenseInstanceWithID(value.length+1,key);
			for(int i=0; i<value.length; i++) {
				sample.setValue(i, value[i]);
			}
			internalSamples.add(sample);
		}
		
		this.delta = config.dec_tree_delta;
		if(delta>0 && delta<0.5) {
			selfAdaptive = false;
		}
		else {
			selfAdaptive = true;
			bigHeap = new PriorityQueue<Double>();
			smallHeap = new PriorityQueue<Double>();
		}
	}

	@Override
	public Instances explore(UserModel model) throws Exception {
		svm_model svmModel = (svm_model)model;
		
		if(selfAdaptive) {
			findDelta(svmModel);
		}
		prepareInternalSamples(svmModel);
		
		// train old samples using Decision Tree
		J48 tree = new J48();
		tree.setUnpruned(true); 
		tree.setUseMDLcorrection(false);
		tree.setCollapseTree(false);
		tree.setBinarySplits(true);
		tree.setReducedErrorPruning(false);
		try {
			tree.buildClassifier(internalSamples);
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		// form the sql query based on the desision tree model
		StringBuilder sb = new StringBuilder();
		sb.append("select ");
		sb.append(config.key_attr);
		for(String s:config.select_attrs) {
			sb.append(',');
			sb.append(s);
		}
		for(Attribute a:config.where_attrs) {
			sb.append(',');
			sb.append(a.name());
		}
		sb.append(" from ");
		sb.append(config.tbl_name);
		sb.append(" where ");
		
		int stringLen = sb.length();
		
		String treeStruc = tree.toString();
		System.out.println(treeStruc);
		String [] lines = treeStruc.split("\n");
		String [] levelPredicates = new String[lines.length];
		int numConjunctions = 0;
		for(int i=0; i<lines.length; i++) {
			if(lines[i].contains("<=") || lines[i].contains(">")) {
				int level = 0;
				for (int j=0; j<lines[i].length(); j++) {
					if (lines[i].charAt(j)=='|') {
						level ++;
					}
				}
				String [] parts = lines[i].split(":");
				levelPredicates[level] = parts[0].replaceAll("\\|", "").replaceAll(" ", "").trim();
				if (parts.length > 1 && parts[1].trim().startsWith("0")) {
					if (numConjunctions > 0) {
						sb.append(" OR ");
					}
					sb.append('(');
					for (int j=0; j<=level; j++) {
						if (j==0) {
							sb.append(levelPredicates[j]);
						}
						else {
							sb.append(" AND ");
							sb.append(levelPredicates[j]);
						}
					}
					sb.append(')');
					numConjunctions++;
				}
			}
		}
		
		boolean notfirst = false;
		if(sb.length()>stringLen) {
			sb.insert(stringLen, '(');
			sb.append(')');
			notfirst = true;
		}
		System.out.println(sb.toString());
		/*Iterator<Long> itr = user.getLabeledKeys().iterator();
		while(itr.hasNext()) {
			if(notfirst) {
				sb.append(" and ");
				notfirst = true;
			}
			sb.append("row_id");
			sb.append("<>");
			sb.append(itr.next());
		}*/
		Iterator<Instance> itr = user.getLabeledSamples().iterator();
		while(itr.hasNext()) {
			if(notfirst) {
				sb.append(" and ");
				notfirst = true;
			}
			sb.append("not (");
			Instance sample = itr.next();
			for(int i=0; i<config.where_attrs.size(); i++) {
				sb.append(config.where_attrs.get(i).name());
				sb.append('=');
				sb.append(sample.value(i));
				if(i!=config.where_attrs.size()-1) {
					sb.append(" and ");
				}
			}
			sb.append(')');
		}
		sb.append(';');
		
		if(queries!=null) {
			queries.println(sb);
			queries.flush();
		}
		return getSamples(svmModel, sb.toString());
	}		
		
	/**
	 * 
	 * @param svmModel
	 * @param x
	 * @return 0 if in delta region, 1 if not
	 */
	private int assignNewLabel(svm_model svmModel, Instance x) {		
		int result = -1; // a default value
		double sum = f_x(svmModel, x);
		if(Math.abs(1.0/(1+Math.exp(-sum)) - 0.5)<=delta) {
			result = 0;
			inCount++;
		}
		else {
			result = 1;
			outCount++;
		}
		return result;
	}
	
	private void prepareInternalSamples(final svm_model svmModel) {
		inCount = 0;
		outCount = 0;
		
//		System.out.println("size 1: " + internalSamples.size());
		for(Instance sample:internalSamples) {
			sample.setValue(internalSamples.classAttribute(), String.valueOf(assignNewLabel(svmModel, sample)));
		}
//		while(internalSamples.size()>ep.points.size()) {
//			internalSamples.remove(ep.points.size());
//		}
//		System.out.println("size 2: " + internalSamples.size());
		
		// partition samples into two classes
		
		for(int i=0; i<config.samples_per_round; i++) { // each newly added training sample
			DenseInstanceWithID sample = (DenseInstanceWithID)oldSamples.get(oldSamples.size()-1-i);
			Instance internalSample = (DenseInstanceWithID)sample.copy();
			
//			for(int j=0; j<temp.length; j++) {
//				internalSample.setValue(j, sample.value(j));
//				temp[j].set(sample.index(j), sample.value(j)); 
//			}		
//			int newLabel = assignNewLabel(svmModel, temp);
//			internalSample.setValue(internalSamples.classAttribute(), String.valueOf(newLabel)); // decision tree cannot take numeric attributes
			internalSample.setValue(internalSamples.classAttribute(), String.valueOf(assignNewLabel(svmModel, internalSample)));
			
			internalSamples.add(internalSample);
		}
//		System.out.println("size 3: " + internalSamples.size());
		System.out.println("delta = " + delta + "; Counts: " + inCount + "\t" + outCount);
	}

	

	
	private void findDelta(final svm_model svmModel) {
		bigHeap.clear(); // smaller than the median
		smallHeap.clear(); // greater than or equal to the median
		delta = 0.23106;
		
		if(!grids.points.isEmpty()) {
			Iterator<Long> itr = grids.points.keySet().iterator();
			delta = Math.abs(1.0/(1+Math.exp(-f_x(svmModel, grids.points.get(itr.next())))) - 0.5);
			smallHeap.offer(delta);
			while(itr.hasNext()) {
				long key = itr.next();
				double [] value = grids.points.get(key);
				double deltaV = Math.abs(1.0/(1+Math.exp(-f_x(svmModel, value))) - 0.5);
				if(deltaV>=delta) {
					smallHeap.offer(deltaV);
					if(bigHeap.size()-smallHeap.size()==-2) {
						bigHeap.offer(-smallHeap.poll());
					}
				}
				else {
					bigHeap.offer(-deltaV);
					if(bigHeap.size()-smallHeap.size()==2) {
						smallHeap.offer(-bigHeap.poll());
					}
				}
			}
		}
		if(oldSamples!=null) {
			for(int i=0; i<oldSamples.numInstances(); i++) {
				DenseInstanceWithID sample = (DenseInstanceWithID)oldSamples.get(i);			
				double deltaV = Math.abs(1.0/(1+Math.exp(-f_x(svmModel, sample))) - 0.5);			
				if(deltaV>=delta) {
					smallHeap.offer(deltaV);
					if(bigHeap.size()-smallHeap.size()==-2) {
						bigHeap.offer(-smallHeap.poll());
					}
				}
				else {
					bigHeap.offer(-deltaV);
					if(bigHeap.size()-smallHeap.size()==2) {
						smallHeap.offer(-bigHeap.poll());
					}
				}
			}
		}
		if(bigHeap.size()==smallHeap.size()) {
			if(!bigHeap.isEmpty()) {
				delta = (-bigHeap.peek()+smallHeap.peek())/2;
			}
		}
		else if(bigHeap.size()>smallHeap.size()) {
			delta = -bigHeap.peek();
		}
		else {
			delta = smallHeap.peek();
		}
	}
}
