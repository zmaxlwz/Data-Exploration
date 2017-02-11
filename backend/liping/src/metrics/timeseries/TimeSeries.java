package metrics.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class TimeSeries {
	final int numPredictions;
	final double confidenceLevel;
	final Instances series;
	final  WekaForecaster forecaster;
	
	List<List<NumericPrediction>> forecast;
	
	public TimeSeries(int numPredictions, double confidenceLevel) throws Exception {
		this.numPredictions = numPredictions;
		this.confidenceLevel = confidenceLevel;
		
		ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		attrs.add(new Attribute("value"));
		series = new Instances(null, attrs, 0);
		
		// new forecaster
	    forecaster = new WekaForecaster();
	    forecaster.setConfidenceLevel(confidenceLevel);
	    forecaster.setCalculateConfIntervalsForForecasts(numPredictions);
	    forecaster.setFieldsToForecast(attrs.get(0).name());
	    // default underlying classifier is SMOreg (SVM) - we'll use gaussian processes for regression instead
	    forecaster.setBaseForecaster(new GaussianProcesses());
	}
	
	public void update(double newValue) throws Exception {
		Instance i = new DenseInstance(1);
		i.setValue(0, newValue);
		series.add(i);

		if(series.size()>numPredictions+12+1) {
		    // build the model
		    forecaster.buildForecaster(series);
		    
		    // prime the forecaster with enough recent historical data to cover up to the maximum lag. 
		    forecaster.primeForecaster(series);
		    
		    // forecast for $numPredictions beyond the end of the training data
		    forecast = forecaster.forecast(numPredictions);
		    System.out.print(newValue + ":\t");
		    for(List<NumericPrediction> predsAtStep:forecast) {
		    	NumericPrediction pred = predsAtStep.get(0);
		    	double [] intvl = pred.predictionIntervals()[0];
		    	System.out.println(pred.predicted() + "\t" + Arrays.toString(intvl) + "\t" + (intvl[1]-intvl[0]));
		    }
		}
		else {
			System.out.println(newValue + ":\t");
		}
	}
	
	public static void main(String[] args) throws Exception {
		double [] values = {0.178527673, 0.160904397, 0.154835472, 0.139932827, 0.1345598, 0.108427436, 0.099910238, 0.110410002, 0.10654578, 0.108125706, 0.068096768, 0.060408417, 0.084070121, 0.07529748, 0.04061501, 0.053465518, 0.100864775, 0.039712121, 0.077450606, 0.016804577, 0.036749322, 0.01359406, 0.069722895, 0.04862673, 0.011896782, 0.020432277, 0.011699795, 0.014028035, 0.031478493, 0.022108111, 0.016085666, 0.024999348, 0.058106954, 0.015226497, 0.007903162, 0.017967631, 0.005366317, 0.010105329, 0.02945265, 0.006182645, 0.044360475, 0.002693712, 0.009271501, 0.020628919, 0.003429709, 0.011670158, 0.004019557, 0.004365615, 0.005748446, 0.006739247, 0.014852713, 0.003095756, 0.003162219, 0.008166542, 0.014345363, 0.002502697, 0.010846318, 0.011389507, 0.002500852, 0.003521117, 0.006236616, 0.000997289, 0.002454694, 0.017385924, 0.00109086, 0.001206289, 0.015861305, 0.005314069, 0.001511138, 0.006405981, 0.009187085, 0.001078394, 0.002843098, 0.000484306, 0.001198196, 0.002448945, 0.004000058, 0.000780013, 0.000751501, 0.002515359, 0.005777488, 0.001302357, 0.00283225, 0.000714034, 0.001691204, 0.00053764, 0.000372409};
		TimeSeries ts = new TimeSeries(1,0.95);
		for(int i=0; i<values.length; i++) {
			ts.update(values[i]);
		}
	}

}
