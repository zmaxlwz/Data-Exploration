package middleware.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import SVMAlgos.libsvm.svm_model;
import metrics.EvaluatingPoints;
import middleware.SVMMiddleWare;
import middleware.datastruc.DenseInstanceWithID;
import system.SystemConfig;
import weka.core.Instance;

public class DemoUser implements User {

	final ServerSocket serverSocket;
	final Socket clientSocket; 
	final PrintWriter out;
	public final BufferedReader in;
	
	final HashSet<Long> requestedKeys;
	final HashSet<Instance> labeledSamples;
	int trueCount, falseCount;
	
	EvaluatingPoints visualPoints;
	boolean [] visualPointsPredictedLabels;
	boolean visualPointsSent = false;
	
	public DemoUser(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		System.out.println("Waiting for connection on port " + port);
		clientSocket = serverSocket.accept();
		System.out.println("connected to " + clientSocket.getInetAddress().getHostName() + ":" + clientSocket.getPort());
		out = new PrintWriter(clientSocket.getOutputStream(), false);  
		out.flush();
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));   
        
		requestedKeys = new HashSet<Long>();
		labeledSamples = new HashSet<Instance>();
		trueCount = 0;
		falseCount = 0;
	}
	
	@Override
	public void setVisualPoints(EvaluatingPoints visualPoints) {
		this.visualPoints = visualPoints;
		visualPointsPredictedLabels = new boolean [visualPoints.points.size()];
	}
	
/*	@Override
	public void setClass(Instance sample, svm_model model) throws IOException {
		DenseInstanceWithID _sample = (DenseInstanceWithID)sample;
		labeledKeys.add(_sample.getID());
		// send to front-end
		JSONObject json = new JSONObject();
		
		JSONArray jSamples = new JSONArray();
		JSONArray jSample = new JSONArray();
		jSample.put(((DenseInstanceWithID)sample).getID());
		// TODO: add 
		for(int i=0; i<sample.numAttributes()-1; i++) {
			jSample.put(sample.value(i));
		}
		jSamples.put(jSample);
		
		json.put("samples", jSamples);
		
		if(visualPoints!=null) {
			JSONArray jLabels = new JSONArray();
			for(double [] point:visualPoints.points.values()) {
				jLabels.put(SVMMiddleWare.f_x(model, point)>0?1:-1);
			}
			json.put("labels", jLabels);
		}
		out.println(json.toString());
		out.flush();
		
		System.out.println("send to the front-end: " + json.toString());
		
		// get label
		JSONTokener ftokener = new JSONTokener(in);
		JSONObject frontEndMsg = new JSONObject(ftokener);
		boolean stop = frontEndMsg.getBoolean("stop");
		if(stop) {
			serverSocket.close();
		}
		JSONArray j_labeledSamples = frontEndMsg.getJSONArray("samples");
//		if(j_labeledSamples.length()!=1) {
//			throw new IOException("only expecting one label but " + j_labeledSamples.length() + " are returned");
//		}
		
		JSONArray j_labeledSample = j_labeledSamples.getJSONArray(0);
//		if(j_labeledSample.length()!=attributesWithClass.size()) {
//			throw new IOException("dimensionalities do not match: " + attributesWithClass.size() + " wanted but " + j_labeledSample.length() + " received");
//		}
		
		int label = j_labeledSample.getInt(j_labeledSample.length()-1);
		sample.setValue(j_labeledSample.length()-1, label);
		if(label==1) {
			trueCount ++;
		}
		else if(label==-1) {
			falseCount ++;
		}
		labeledSamples.add(sample);
	}
	
	@Override
	public Instances label(HashMap<Long, double[]> samples, svm_model model) throws IOException {
		// send to front-end
		JSONObject json = new JSONObject();
		
		JSONArray jSamples = new JSONArray();
		for(double [] sample:samples.values()) {
			JSONArray jSample = new JSONArray(sample);
			jSamples.put(jSample);
		}
		json.put("samples", jSamples);
		
		System.out.println("send to the front-end: " + json.toString());
		
		if(visualPoints!=null) {
			if(model==null) {
				JSONArray jPoints = new JSONArray();
				for(double [] point:visualPoints.points.values()) {
					JSONArray jPoint = new JSONArray(point);
					jPoints.put(jPoint);
				}
				json.put("points", jPoints);
			}
			else {
				JSONArray jLabels = new JSONArray();
				for(double [] point:visualPoints.points.values()) {
					jLabels.put(SVMMiddleWare.f_x(model, point)>0?1:-1);
				}
				json.put("labels", jLabels);
			}
		}
		out.println(json.toString());
		out.flush();
		
		System.out.println("send to the front-end: " + json.toString());
		
		labeledKeys.addAll(samples.keySet());
		
		if(attributesWithClass==null) {
			throw new IOException("must call setAttributesWithClass() first");
		}
		Instances labeledSamples = new Instances(null, attributesWithClass, 0);
		labeledSamples.setClass(attributesWithClass.get(attributesWithClass.size()-1));
		// get labels
		JSONTokener ftokener = new JSONTokener(in);
		JSONObject frontEndMsg = new JSONObject(ftokener);
		boolean stop = frontEndMsg.getBoolean("stop");
		if(stop) {
			serverSocket.close();
		}
		JSONArray j_labeledSamples = frontEndMsg.getJSONArray("samples");
		for(int i=0; i<j_labeledSamples.length(); i++) {
			JSONArray j_labeledSample = j_labeledSamples.getJSONArray(i);
			Instance sample = new DenseInstanceWithID(j_labeledSample.length(),0);
			for(int j=0; j<j_labeledSample.length()-1; j++) {
				sample.setValue(j, j_labeledSample.getDouble(j));
			}
			int label = j_labeledSample.getInt(j_labeledSample.length()-1);
			sample.setValue(j_labeledSample.length()-1, label);
			if(label==1) {
				trueCount ++;
			}
			else if(label==-1) {
				falseCount ++;
			}
			this.labeledSamples.add(sample);
			labeledSamples.add(sample);
		}
		return labeledSamples;
	}*/

	@Override
	public HashSet<Long> getLabeledKeys() {
		return requestedKeys;
	}

	@Override
	public int getPositiveCount() {
		return trueCount;
	}

	@Override
	public int getNegativeCount() {
		return falseCount;
	}

	@Override
	public void increasePosCount() {
		trueCount ++;
	}

	@Override
	public void increaseNegCount() {
		falseCount ++;
	}

	@Override
	public HashSet<Instance> getLabeledSamples() {
		return labeledSamples;
	}
	
	@Override
	public boolean isAlive() {
		return (!serverSocket.isClosed());
	}

	@Override
	public HashSet<DenseInstanceWithID> label(HashSet<DenseInstanceWithID> samples, svm_model model) throws Exception {
		// send to front-end
		JSONObject json = new JSONObject();
		
		JSONArray jSamples = new JSONArray();
		for(DenseInstanceWithID sample:samples) {
			JSONArray jSample = new JSONArray();
			jSample.put(sample.getID());
			for(int i=0; i<sample.getAdditionalAttributeValues().length; i++) {
				jSample.put(sample.getAdditionalAttributeValues()[i]);
			}
			for(int i=0; i<sample.numValues()-1; i++) {
				jSample.put(sample.value(i));
			}
			jSamples.put(jSample);
			requestedKeys.add(sample.getID());
		}
		json.put("samples", jSamples);
		
		if(visualPoints!=null) {
			if(model==null) {
				if(!visualPointsSent) {
					JSONArray jPoints = new JSONArray();
					for(double [] point:visualPoints.points.values()) {
						JSONArray jPoint = new JSONArray(point);
						jPoints.put(jPoint);
					}
					json.put("points", jPoints);
					visualPointsSent = true;
					
					if(SystemConfig.visualTruth!=null) {
						json.put("truth", SystemConfig.visualTruth);
						json.put("truth_shape", SystemConfig.visualTruthShape);
					}
				}
			}
			else {
				JSONArray jLabels = new JSONArray();
				for(double [] point:visualPoints.points.values()) {
					jLabels.put(SVMMiddleWare.f_x(model, point)>0?1:-1);
				}
				json.put("labels", jLabels);
			}
		}
		out.println(json.toString());
		out.flush();
		
		System.out.println("send to the front-end: " + json.toString());
		
		HashSet<DenseInstanceWithID> labeledSamples = new HashSet<DenseInstanceWithID>();

		// get labels
		JSONTokener ftokener = new JSONTokener(in);
		JSONObject frontEndMsg = new JSONObject(ftokener);
		boolean stop = frontEndMsg.getBoolean("stop");
		if(stop) {
			serverSocket.close();
		}
		JSONArray j_labeledSamples = frontEndMsg.getJSONArray("samples");		
		for(int i=0; i<j_labeledSamples.length(); i++) {
			JSONArray j_labeledSample = j_labeledSamples.getJSONArray(i);
			DenseInstanceWithID sample = new DenseInstanceWithID(j_labeledSample.length(),-1);
			for(int j=0; j<j_labeledSample.length()-1; j++) {
				sample.setValue(j, j_labeledSample.getDouble(j));
			}
			int label = j_labeledSample.getInt(j_labeledSample.length()-1);
			sample.setValue(j_labeledSample.length()-1, label);
			if(label==1) {
				trueCount ++;
			}
			else if(label==-1) {
				falseCount ++;
			}
			this.labeledSamples.add(sample);
			labeledSamples.add(sample);
		}
		return labeledSamples;
	}
}
