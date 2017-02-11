package middleware;

import java.io.IOException;

import weka.core.Instances;


public interface MiddleWare {
	Instances initialSampling() throws Exception;
	/**
	 * classification algorithm
	 * @param samples: a list of samples in the current round
	 * @param labels: corresponding labels, 0 means unlabeled 
	 * @return a new user model for the current round
	 * @throws Exception 
	 */
	UserModel classify(Instances samples) throws Exception;
	
	/**
	 * prepare samples for users to label in the next iteration
	 * 		step 1: form sample requests based on the current user model
	 *      step 2: translate the sample requests to SQL queries
     *		step 3: run SQL queries in postgresql using JDBC 
     *		step 4: use TupleIterator to get all the tuples returned by the SQL query and put them into an ArrayList
	 * @param model: null or the model in the current iteration
	 * @param firstRun: true for initial sampling
	 * @return a list of to-be-labeled samples 
	 * @throws IOException 
	 */
	Instances explore(UserModel model) throws Exception;
	void close();
}
