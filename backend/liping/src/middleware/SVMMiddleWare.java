package middleware;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

import SVMAlgos.LibSVMLearning;
import SVMAlgos.SVMLearning;
import SVMAlgos.libsvm.Kernel;
import SVMAlgos.libsvm.svm_model;
import metrics.Accuracy;
import metrics.ChangeRate;
import metrics.EvaluatingPoints;
import metrics.ThreeSetMetric;
import middleware.datastruc.AttributeValue;
import middleware.datastruc.DenseInstanceWithID;
import middleware.datastruc.ObjectWithDistance;
import middleware.sampleacquisition.Sampling;
import middleware.user.User;
import system.Context;
import system.SystemConfig;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Constraints:
 * In order to expedite the process of getting random samples from a table, 
 * it is required that the table has an attribute named "row_id".
 * 
 * @author lppeng
 *
 */
public abstract class SVMMiddleWare implements MiddleWare{
	protected final SystemConfig config;
	protected final Context context;
	protected final User user;
	
	protected final PrintWriter pw_accuracy_profile; // only for system running in the "ACCURACY" mode;
	protected final PrintWriter pw_sample;	// always print samples
	
	protected Instances oldSamples;
	int iteration;		
	SVMLearning svm;	
	
	protected final TableAttrStat stat;	
	protected EvaluatingPoints ep;
	
	protected ThreeSetMetric tsm;
	ChangeRate cr; 
	
	public static AttributeValue [] temp;
	
	public SVMMiddleWare(SystemConfig config, Context context, User user) throws Exception {
		this.config = config;
		this.context = context;
		this.user = user;
		
		if (config.mode == SystemConfig.Mode.ACCURACY) {
			pw_accuracy_profile = new PrintWriter(new FileWriter("F-measure_" + context.truthPredicate));
		}
		else {
			pw_accuracy_profile = null;
		}
		if(config.mode!=SystemConfig.Mode.DEMO) {
			pw_sample = new PrintWriter(new FileWriter("samples_" + config.exploreMethod + ".txt"));
		}
		else {
			pw_sample = null;
		}

		oldSamples = null;
		iteration = 0;
		svm = new LibSVMLearning(config.gamma, config.C);
		
		stat = new TableAttrStat(config,context);
		switch(config.evalMode) {
		case GRIDS:
			ep = new EvaluatingPoints(stat, config.eval_granularity);
			break;
		case SAMPLES:
			ep = new EvaluatingPoints(stat, context.stmt, config.eval_granularity, config.key_attr);
			break;
		case FILE:
			ep = new EvaluatingPoints(stat, config.eval_pointFile);
			break;
		}
		
		if(config.mode==SystemConfig.Mode.DEMO) {
			user.setVisualPoints(ep);
		}
		else {
			tsm = new ThreeSetMetric(config,ep);
			cr = new ChangeRate(ep, ((LibSVMLearning)svm).getParam(), config.where_attrs.size());
		}
		
		temp = new AttributeValue [config.where_attrs.size()];
		for(int i=0; i<temp.length; i++) {
			temp[i] = new AttributeValue();
		}
	}
	
	@Override
	public Instances initialSampling() throws Exception {	
		if(config.debug) {
			System.out.println("initial sampling....");
		}
		HashSet<DenseInstanceWithID> samples = null;
	  	switch(config.initialSamplingMethod) {
	  	case FILE:
	  		samples = Sampling.loadFromFile(config, config.initialSampleFile);
	  		break;
	  	case EQUIWIDTH:
	  	case EQUIDEPTH:
	  	case HYBRID:
	  		samples = Sampling.loadFromDB(config, context, stat);
	  		break;
	  	default:
	  		break;
	  	}

	  	samples = user.label(samples, null);
	  	if(samples.size()==0 || !(user.getPositiveCount()>=1 && user.getNegativeCount()>=1)) {
	  		return initialSampling();
	  	}
	  	else {
		  	Instances labeledSamples = new Instances(null, config.where_attrs_with_class, 0);				
		  	labeledSamples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		  	for(DenseInstanceWithID sample:samples) {
		  		labeledSamples.add(sample);
		  	}
		  	return labeledSamples;
	  	}
	}
	
	@Override
	public UserModel classify(Instances samples) throws Exception {
		if(config.debug) {
			System.out.println("classifying....");
		}
		if(oldSamples==null) {
			oldSamples = samples;
			oldSamples.setClassIndex(samples.classIndex());
		}
		else {
			for(int i=0; i<samples.numInstances(); i++) {
				oldSamples.add(samples.instance(i));
			}
		}
		return svm.learn(oldSamples);
	}

	@Override
	public abstract Instances explore(UserModel model) throws Exception ;	
	
	@Override
	public void close() {
		try {
			context.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(pw_accuracy_profile!=null) {
			pw_accuracy_profile.close();
		}
		if(pw_sample!=null) {
			pw_sample.close();
		}
	}
	
	public void demoRun() throws Exception {
		oldSamples = null;
		iteration = 0;		

		Instances samples = initialSampling();
		
		UserModel model = null;
		
		while(user.isAlive()) {
			model = classify(samples);
			samples = explore(model);
			if(samples==null) {
				throw new Exception("exploration failed");
			}
			iteration ++;
		}
	}
	
	public void run() throws Exception {
		oldSamples = null;
		iteration = 0;		

		Instances samples = initialSampling();
		if(samples==null || !(user.getPositiveCount()>=1 && user.getNegativeCount()>=1)) {
			throw new Exception("initial sampling failed");
		}
		
		Date d;
		d = new Date(System.currentTimeMillis());
		for(int i=0; i<samples.size(); i++) {
			pw_sample.println(d.toString() + ": " + samples.get(i).toString());
		}
		pw_sample.flush();
		
		cr.update(oldSamples, samples);
		tsm.updateRatio(samples);		

		int sampleCount = samples.size();

		UserModel model = null;
		
		while(user.isAlive()) {			
			model = classify(samples);
			svm_model _model = (svm_model)model;			
		
			cr.updateChangeRate(_model);
			
			// print trace and compute accuracy
			d = new Date(System.currentTimeMillis());
			if(pw_accuracy_profile!=null) {
				pw_accuracy_profile.println(d.toString() + " model " + _model.toString());
				pw_accuracy_profile.flush();
				double accu = Accuracy.evaluateF(context.truth, tsm, _model);
				pw_accuracy_profile.println(d.toString() + " " + user.getPositiveCount() + " positive samples and " + user.getNegativeCount() 
						+ " negative samples achieve accuracy (F-measure) " + accu + " at least " + (2*tsm.value/(1+tsm.value)));
				pw_accuracy_profile.flush();
			}
			
			if(config.debug) {
				System.out.println("accuracy: " + 2*tsm.value/(1+tsm.value) + " vs " + config.accuracyThreshold);
				System.out.println("samples: " + sampleCount + " vs " + config.sampleLimit);
			}
			
			if(2*tsm.value/(1+tsm.value)>config.accuracyThreshold || sampleCount>config.sampleLimit) { //TODO: add fitting
				break;
			}			
			
			if(config.debug) {
				System.out.println("exploring....");
			}
			samples = explore(model);
			if(samples==null) {
				throw new Exception("exploration failed");
			}
			else {
				cr.update(oldSamples, samples);
				tsm.updateRatio(samples);
				sampleCount += samples.size();
			}
			iteration ++;
		} 
	}	
	
	public void resume(String sampleFile) throws Exception {
/*		oldSamples = null;
		iteration = 0;
		Instances samples = LoadSamples.loadFromFile(config.where_attrs_with_class, sampleFile, user);
		if(samples==null) {
			System.err.println("initial sampling failed");
			return;
		}
		
		Date d = new Date(System.currentTimeMillis());
		for(int i=0; i<samples.size(); i++) {
			pw_sample.println(d.toString() + ": " + samples.get(i).toString());
		}
		pw_sample.flush();
		
		for(Instance i:samples) {
			DenseInstanceWithID _i = (DenseInstanceWithID)i;
			user.getLabeledKeys().add(_i.getID());
			if(_i.classValue()==1) {
				user.increasePosCount();
			}
			else {
				user.increaseNegCount();
			}
		}
		
		cr.update(oldSamples, samples);
//		tsm.startMetric(samples);
//		tsm.update2dimRatio(samples,2);
		tsm.updateRatio(samples);
		
		int sampleCount = samples.size();
		
		UserModel model = null;
		
		while(true) {
			
			model = classify(samples);
			svm_model _model = (svm_model)model;			
		
			cr.updateChangeRate(_model);
			
			// print trace and compute accuracy
			d = new Date(System.currentTimeMillis());
			if(pw_accuracy_profile!=null) {
				pw_accuracy_profile.println(d.toString() + " model " + _model.toString());
				pw_accuracy_profile.flush();
				double accu = Accuracy.evaluateF(context.truth, tsm, _model);
				pw_accuracy_profile.println(d.toString() + " " + user.getPositiveCount() + " positive samples and " + user.getNegativeCount() 
						+ " negative samples achieve accuracy (F-measure) " + accu + " at least " + (2*tsm.value/(1+tsm.value)));
				pw_accuracy_profile.flush();
			}
			
			if(2*tsm.value/(1+tsm.value)>config.accuracyThreshold || sampleCount>config.sampleLimit) { //TODO: add fitting
				break;
			}			
			
			samples = explore(model);
			if(samples==null) {
				throw new Exception("exploration failed");
			}
			else {
				cr.update(oldSamples, samples);
				tsm.updateRatio(samples);
				sampleCount += samples.size();
			}
			iteration ++;
		}*/
	}
	
	public static double f_x(svm_model svmModel, Instance sample) {
		for(int j=0; j<temp.length; j++) {
			temp[j].set(sample.index(j), sample.value(j)); 
		}
		return f_x(svmModel,temp);
	}
	
	public static double f_x(svm_model svmModel, double [] x) {
		for(int j=0; j<temp.length; j++) {
			temp[j].set(j, x[j]); 
		}
		return f_x(svmModel,temp);
	}
	
	public static double f_x(svm_model svmModel, AttributeValue[] x) {
		double[] sv_coef = svmModel.sv_coef[0];
		double sum = 0;
		for(int i=0;i<svmModel.l;i++) {
			sum += sv_coef[i] * Kernel.k_function(x,svmModel.SV[i],svmModel.param);
		}
		sum -= svmModel.rho[0]; // hyperplane: wx-b=0
		return sum;
	}
	
	protected Instances getSamples(final svm_model svmModel, String sql) throws Exception  {
		PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> topK = new PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> (1,new Comparator<ObjectWithDistance<DenseInstanceWithID>>() {
		    public int compare(ObjectWithDistance<DenseInstanceWithID> n1, ObjectWithDistance<DenseInstanceWithID> n2) {
		        double d1 = n1.getDistance();
		        double d2 = n2.getDistance();
		        if(d1<d2) {
		        	return 1;
		        }
		        else if(d1==d2) {
		        	return 0;
		        }
		        else {
		        	return -1;
		        }
		    }
		});
	
		long start = System.currentTimeMillis();
		ResultSet rs = context.stmt.executeQuery(sql);
		long queryTime = System.currentTimeMillis() - start;
		ResultSetMetaData rsmd = rs.getMetaData();
		int count = 0;
		
		start = System.currentTimeMillis();
		while(rs.next()){
			count++;
	    	
			long key = rs.getLong(1);
	    	double [] additionalAttributeValues = new double [config.select_attrs.size()];
	    	for(int i=0; i<config.select_attrs.size(); i++) { 
	    		String columnType = rsmd.getColumnTypeName(i+2);    		
	    		if(columnType.startsWith("float")) {
	    			additionalAttributeValues[i] = rs.getDouble(i+2);
	    		}
	    		else if(columnType.startsWith("int")) {
	    			additionalAttributeValues[i] = rs.getLong(i+2);
	    		}
	    		else {
	    			throw new Exception("columntype " + columnType);
	    		}
	    	}
	    	DenseInstanceWithID sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),key,additionalAttributeValues);
 			for(int i=0; i<config.where_attrs.size(); i++) {
 				String columnType = rsmd.getColumnTypeName(i+2+config.select_attrs.size());
 				if(columnType.startsWith("float")) {
 					sample.setValue(i, rs.getDouble(i+2+config.select_attrs.size()));
	    		}
	    		else if(columnType.startsWith("int")) {
	    			sample.setValue(i, rs.getLong(i+2+config.select_attrs.size()));
	    		}
	    		else {
	    			throw new Exception("columntype " + columnType);
	    		}
			}
 			
	    	topK.add(new ObjectWithDistance<DenseInstanceWithID>(Math.abs(f_x(svmModel, (DenseInstanceWithID)sample)),(DenseInstanceWithID)sample));
	    	
	    	if(topK.size()>config.samples_per_round) {
	    		topK.remove();
	    	}
	   	}
		long evalTime = System.currentTimeMillis() - start;
	    rs.close();
	    
	    if(pw_accuracy_profile!=null) {
	    	pw_accuracy_profile.println("number of tuples loaded: " + count + " queryTime: " + queryTime + " evalTime: " + evalTime);
	    	pw_accuracy_profile.flush();
	    }
		Instances labeledSamples = new Instances(null, config.where_attrs_with_class, 0);				
		labeledSamples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		HashSet<DenseInstanceWithID> filtered = new HashSet<DenseInstanceWithID>();
		
		Iterator<ObjectWithDistance<DenseInstanceWithID>> itr_topK = topK.iterator();
		while(itr_topK.hasNext()) {
			DenseInstanceWithID sample = itr_topK.next().getObject();						
			if(tsm==null || tsm.isUsefulSample(sample)) {
				filtered.add(sample);
			}
			else {
				labeledSamples.add(sample);
				if(pw_sample!=null) {
					pw_sample.println(new Date(System.currentTimeMillis()).toString() + ": " + sample.toString());
				}
			}
			
		}
		filtered = user.label(filtered, svmModel);
		if(filtered.size()==0) {
			return explore(svmModel);
		}
		else {
			for(DenseInstanceWithID sample:filtered) {
				labeledSamples.add(sample);
				if(pw_sample!=null) {
					pw_sample.println(new Date(System.currentTimeMillis()).toString() + ": " + sample.toString());
				}
			}
			
			if(pw_sample!=null) {
				pw_sample.flush();
			}		
			return labeledSamples;
		}
	}

	/*	Instances falseNeg; //holding false negatives 
	private void findFalseNeg(svm_model svmModel) {
		if(falseNeg == null) {
			falseNeg = new Instances(null, where_attrs, 0);
			falseNeg.setClass(where_attrs.get(where_attrs.size()-1));
		}
		else {
			falseNeg.clear();
		}
		for(int i=0; i<oldSamples.numInstances(); i++) {
			Instance sample = oldSamples.get(i);
			double estimate = SVMMiddleWare.f_x(svmModel, sample);
			if(estimate<0 && sample.classValue()>0) {
				falseNeg.add(sample);
				pw.println("Iteration: " + iteration + " false negative: " + sample.toString());
			}
		}
	}*/
	/*if(!falseNeg.isEmpty()) { // explore
	
	StringBuilder sb = new StringBuilder();
	sb.append("select ");
	sb.append(key_attr);
	sb.append(',');
	for(int i=0; i<where_attrs.size()-1; i++) {
		sb.append(where_attrs.get(i).name());
		if(i!=where_attrs.size()-2) {
			sb.append(',');
		}
		else {
			sb.append(' ');
		}
	}
	sb.append("from ");
	sb.append(tbl_name);
	sb.append(" where ");
	
	boolean notfirst = false;
	Iterator<Long> itr = user.labeledKeys.iterator();
	while(itr.hasNext()) {
		if(notfirst) {
			sb.append(" and ");
		}
		notfirst = true;
		sb.append(key_attr);
		sb.append("<>");
		sb.append(itr.next());
	}
	sb.append(';');
	
	double [] mins = new double [falseNeg.size()];
	Arrays.fill(mins, Double.MAX_VALUE);
	Instance [] nneighbors = new Instance [falseNeg.size()];
	Arrays.fill(nneighbors, null);
	try {
		ResultSet rs = stmt.executeQuery(sb.toString());
		ResultSetMetaData rsmd = rs.getMetaData();
		
		while(rs.next()){
	    	int columnCount = rsmd.getColumnCount();
	    	Instance sample = new DenseInstanceWithID(columnCount,rs.getLong(1)); // column index starts with 1, the first attribute is the key attribute
	    	
	    	for(int i=2; i<=columnCount; i++) { 
	    		String columnType = rsmd.getColumnTypeName(i);    		
	    		if(columnType.startsWith("float")) {
	    			sample.setValue(where_attrs.get(i-2), rs.getDouble(i));
	    		}
	    		else if(columnType.startsWith("int")) {
	    			sample.setValue(where_attrs.get(i-2), rs.getLong(i));
	    		}
	    		else if(columnType.equals("string")){
	    			System.err.println(columnType);
	    		}
	    	}
	    	for(int i=0; i<falseNeg.size(); i++) {
				if(distance(falseNeg.get(i), sample)<mins[i]) {
					nneighbors[i] = sample;
				}
			}
	   	}
	}
	catch (SQLException e) {
		e.printStackTrace();
	}
	d = new Date(System.currentTimeMillis());
	for(int i=0; i<falseNeg.size(); i++) {
		System.out.println("iteration " + iteration + " false negative: " + falseNeg.get(i).toString());
		if(nneighbors[i]!=null) {
			user.setClass(nneighbors[i]);
			result.add(nneighbors[i]);
			SystemConfig.pw_sample.println("fn" + d.toString() + ": " + nneighbors[i].toString() );
		}
	}
}*/

	
}
