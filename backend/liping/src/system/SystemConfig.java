package system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import weka.core.Attribute;

public class SystemConfig {
		
	public final static int PAGE_SIZE = 4096;	
	
	// running mode
	public enum Mode {RUN, ACCURACY, DEMO};
	public final Mode mode;
	public boolean debug;
	
	// db
	public final String host;
	public final int port;
	public final String user;
	public final String pwd;
	public final String db_name;
	
	// query
	public final String tbl_name;
	public final String key_attr;
	public final ArrayList<String> select_attrs;
	public final ArrayList<Attribute> where_attrs;
	public final ArrayList<Attribute> where_attrs_with_class;
	public final boolean focusedExploration;
	public final double [] lowerBound;
	public final double [] upperBound;
	
	// initial samples
	public enum InitialSamplingMethod {FILE, EQUIWIDTH, EQUIDEPTH, HYBRID};
	public final InitialSamplingMethod initialSamplingMethod;
	public String initialSampleFile;
	public int [] numBins;
	public int numSamplesPerBin;
	
	// learnging algo: svm with rbf kernel
	public enum LearningMethod {LIBSVM};
	public final LearningMethod learningMethod;
	public double gamma, C;

	// explore algo:
	public final int samples_per_round; 
	public enum ExploreMethod {OPT_TREE, DECISION_TREE, FULL_SCAN, RANDOM, GRIDS};
	public final ExploreMethod exploreMethod;	
	
	// opt-tree
	public int opt_tree_node_granularity; 
	public int opt_tree_presample_count; 
	
	// decision-tree
	public double dec_tree_delta; 
	public int dec_tree_grid_count;
	
	// random
	public int random_sample_count_per_iteration; 	
	
	// eval 
	public enum EvalMode {GRIDS, SAMPLES, FILE};
	public final EvalMode evalMode; 
	public int eval_granularity; 
	public String eval_pointFile; 
	
	// stopping criterion
	public int sampleLimit; 
	public double accuracyThreshold; 
	public double fittingThreshold; 
	
	// timeseries forecast
	public int numPredictions;
	public double confidenceLevel;
	
	// visual true query
	public static JSONArray visualTruth;
	public static String visualTruthShape;
	
	public SystemConfig(Reader frontEndConfigReader, String backEndConfigFile, String mode) throws Exception {
		this.mode = Mode.valueOf(mode);
		
		JSONTokener ftokener = new JSONTokener(frontEndConfigReader);
		JSONObject frontEndConfig = new JSONObject(ftokener);
		
		// db
		JSONObject db = frontEndConfig.getJSONObject("db");
		host = db.getString("host");
		port = db.getInt("port");
		user = db.getString("user");
		pwd = db.getString("pwd");
		db_name = db.getString("db");
		
		//query
		JSONObject query = frontEndConfig.getJSONObject("query");
		tbl_name = query.getString("tableName");	
		key_attr = query.getString("key");
		select_attrs = new ArrayList<String>();
		JSONArray j_select_attrs = query.getJSONArray("visualAttributes");
		for(int i=0; i<j_select_attrs.length(); i++) {
			select_attrs.add(j_select_attrs.getString(i));
		}
		where_attrs = new ArrayList<Attribute>();
		where_attrs_with_class = new ArrayList<Attribute>();		
		
		JSONArray j_partition_attrs = query.getJSONArray("attributes");
		for(int i=0; i<j_partition_attrs.length(); i++) {
			Attribute a = new Attribute(j_partition_attrs.getString(i),i); // IMPORTANT: initialize with dictated index value
			where_attrs.add(a); 
			where_attrs_with_class.add(a);
		}
		where_attrs_with_class.add(new Attribute("class"));
		
		focusedExploration = query.getBoolean("focusedExploration");
		if(focusedExploration) {
			lowerBound = new double [where_attrs.size()];
			upperBound = new double [where_attrs.size()];
			JSONArray j_lowerBounds = query.getJSONArray("lowerBounds");
			JSONArray j_upperBounds = query.getJSONArray("upperBounds");
			if(j_lowerBounds.length()!=j_partition_attrs.length() || j_upperBounds.length()!=j_partition_attrs.length()) {
				throw new Exception("problematic front-end configuration: " + 
						j_lowerBounds.length() + "\t" + j_upperBounds.length() + "\t" + j_partition_attrs.length());
			}
			for(int i=0; i<j_partition_attrs.length(); i++) {
				lowerBound[i] = j_lowerBounds.getDouble(i); 
				upperBound[i] = j_upperBounds.getDouble(i);
			}
		}
		else {
			lowerBound = null;
			upperBound = null;
		}		
		
		BufferedReader bbr = new BufferedReader(new FileReader(backEndConfigFile));
		JSONTokener btokener = new JSONTokener(bbr);
		JSONObject backEndConfig = new JSONObject(btokener);
		
		// initial sampling
		JSONObject sampling = backEndConfig.getJSONObject("initialsamples");
		initialSamplingMethod = InitialSamplingMethod.valueOf(sampling.getString("method"));
		switch(initialSamplingMethod) {
		case FILE:
			initialSampleFile = sampling.getString("FILE");
			break;
		case EQUIWIDTH:
		case EQUIDEPTH:
		case HYBRID:
			int numSamples = sampling.getInt("sample_limit");
			int bins = (int)Math.pow(numSamples, 1.0/where_attrs.size());
			//JSONArray j_numBins = sampling.getJSONArray("num_bins");
			//int bins = sampling.getInt("num_bins");
			//numSamplesPerBin = sampling.getInt("samples_per_bin");
		  	numBins = new int [where_attrs.size()];
		  	for(int i=0; i<numBins.length; i++) {
		  		//numBins[i] = j_numBins.getInt(i);
		  		numBins[i] = bins;
		  	}
		  	numSamplesPerBin = 1;
		  	break;
		default:
			throw new Exception("Invalid initial sampling: " + initialSamplingMethod.toString());
		}
		
		// classification
		JSONObject learning = backEndConfig.getJSONObject("learning");
		learningMethod = LearningMethod.valueOf(learning.getString("method"));
		switch(learningMethod) {
		case LIBSVM:
			JSONObject libsvm = learning.getJSONObject("LIBSVM");
			gamma = libsvm.getDouble("gamma");
			C = libsvm.getDouble("C");
			break;
		default:
			throw new Exception("Invalid learning algo");
		}
		
		// exploration
		JSONObject explore = backEndConfig.getJSONObject("explore");
		samples_per_round = explore.getInt("sample_round_limit");	
		exploreMethod = ExploreMethod.valueOf(explore.getString("method"));
		switch(exploreMethod) {
		case OPT_TREE:
			JSONObject optTree = explore.getJSONObject("OPT_TREE");
			opt_tree_node_granularity = optTree.getInt("node_eval");
			opt_tree_presample_count = optTree.getInt("presample_count");
			break;
		case DECISION_TREE:
			JSONObject decTree = explore.getJSONObject("DECISION_TREE");
			dec_tree_delta = decTree.getDouble("delta");
			dec_tree_grid_count = decTree.getInt("grid_count");
			break;
		case FULL_SCAN:
			break;
		case RANDOM:
			random_sample_count_per_iteration = explore.getInt("RANDOM");
			break;
		case GRIDS:
			break;
		default:
			throw new Exception("Invalid exploration algo");
		}
		
		// evaluating points when computing metrics
		JSONObject eval = backEndConfig.getJSONObject("eval");
		evalMode = EvalMode.valueOf(eval.getString("mode"));
		switch(evalMode) {
		case GRIDS:
			eval_granularity = eval.getInt("granularity");
			break;
		case SAMPLES:
			eval_granularity = eval.getInt("granularity");
			break;
		case FILE:
			eval_pointFile = eval.getString("FILE");
			break;
		default:
			throw new Exception("Invalid evaluating points");
		}
		
		// to visual ground truth, only work for rectangle or ellipse
		try {
			visualTruth = backEndConfig.getJSONArray("visual_truth");	
			visualTruthShape = backEndConfig.getString("truth_shape");
		}
		catch(JSONException je) {
			visualTruth = null;
			visualTruthShape = null;
		}
		
		if(this.mode!=Mode.DEMO) {
			// stopping criterion
			JSONObject stop = backEndConfig.getJSONObject("stop");
			sampleLimit = stop.getInt("limit"); 
			accuracyThreshold = stop.getDouble("accuracy_thre"); 
			//fittingThreshold = stop.getDouble("fitting_thre"); 
			//numPredictions = stop.getInt("num_predictions");
			//confidenceLevel = stop.getDouble("confidence");
		}
		bbr.close();
	}
	
	public SystemConfig(String frontEndConfigFile, String backEndConfigFile, String mode) throws Exception{
		this(new BufferedReader(new FileReader(frontEndConfigFile)), backEndConfigFile, mode);		
	}
	
	public SystemConfig(String frontEndConfigFile, String backEndConfigFile) throws Exception{
		this(new BufferedReader(new FileReader(frontEndConfigFile)), backEndConfigFile, "RUN");		
	}
}
