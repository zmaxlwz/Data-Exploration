package middleware;

public interface UserModel {
	/**
	 * translate the model in order to talk to postgresql using JDBC 
	 * @return a SQL query in String type 
	 */
	String translateToSQL(); 
}
