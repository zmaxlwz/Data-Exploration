package execute;

import system.SystemConfig;
import trees.DeltaBoundRTree;
import tuplereader.DBTupleReader;

public class BuildRTree {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("usage: frontend_config_file backend_config_file");
			System.exit(1);
		}
		SystemConfig config = new SystemConfig(args[0],args[1]);
		
		DBTupleReader<Long> dtr = new DBTupleReader<Long>(config);
		new DeltaBoundRTree(config, dtr);
		dtr.close();
	}
}
