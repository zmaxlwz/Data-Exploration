package SVMAlgos;

import weka.core.Instances;
import middleware.UserModel;


public interface SVMLearning {
	public UserModel learn(Instances sample) throws Exception;
}
