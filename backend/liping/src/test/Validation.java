package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import SVMAlgos.LibSVMLearning;
import SVMAlgos.libsvm.svm_model;
import middleware.SVMMiddleWare;
import middleware.datastruc.AttributeValue;
import middleware.datastruc.DenseInstanceWithID;
import system.SystemConfig;
import weka.core.Instance;
import weka.core.Instances;

public class Validation {

	public static void main(String[] args) throws Exception {
		SystemConfig config = new SystemConfig(args[1],args[2]);
		LibSVMLearning svm = new LibSVMLearning(config.gamma, config.C);
		Instances samples = new Instances(null, config.where_attrs_with_class, 0);				
		samples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		
		String sampleFileName = args[0];
		BufferedReader br = new BufferedReader(new FileReader(sampleFileName));
		String nextLine;
		while((nextLine=br.readLine())!=null) {
			StringTokenizer st = new StringTokenizer(nextLine, ",");
			long key = Long.valueOf(st.nextToken());
			
			Instance sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),key);
 			for(int i=0; i<config.where_attrs.size(); i++) {
 				sample.setValue(i, Double.valueOf(st.nextToken()));
			}
			sample.setValue(config.where_attrs_with_class.size()-1,Integer.valueOf(st.nextToken()));
			samples.add(sample);
		}
		br.close();
		
		System.out.println("total number of samples: " + samples.size());
		
		SVMMiddleWare.temp = new AttributeValue [config.where_attrs.size()];
		for(int i=0; i<config.where_attrs.size(); i++) {
			SVMMiddleWare.temp[i] = new AttributeValue();
		}
		
		int window = Integer.valueOf(args[3]);
		PrintWriter pw = new PrintWriter(new FileWriter("fmeasure%_" + window + ".txt"));
		Instances _subSamples = new Instances(samples,0,2); // size of _subSamples >=2
		for(int i=2+window-1; i<samples.size(); i++) {
			svm_model model = (svm_model) svm.learn(_subSamples);
			
			// evaluate accuracy
			double correct = 0, incorrect = 0;
			double tp=0, fp=0, fn = 0;
			for(int j=0; j<window; j++) {
				Instance sample = samples.get(samples.size()-1-j);
				double f = SVMMiddleWare.f_x(model, sample);
				if(f>=0) {
					if(sample.classValue()>=0) {
						tp ++;
						correct++;
					}
					else {
						fp ++;
						incorrect++;
					}
				}
				else {
					if(sample.classValue()>=0) {
						fn ++;
						incorrect++;
					}
					else {
						correct++;
					}
				}
			}
			double precision = tp/(tp+fp);
		    double recall = tp/(tp+fn);
		    double fmeasure = 2*precision*recall/(precision+recall);
		    pw.println(i + "\t" + (i-window+1) + "\t" + 
		    		tp + "\t" + fp + "\t" + fn + "\t" + precision + "\t" + recall + "\t" + fmeasure);
		    		//correct + "\t" + incorrect + "\t" + correct/(correct+incorrect));
		    _subSamples.add(samples.get(i-window+1));
		}
		pw.close();
	}

}
