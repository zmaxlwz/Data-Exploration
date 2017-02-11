package execute;

import java.sql.Connection;
import java.sql.Statement;

import org.apache.commons.math3.util.Pair;

import middleware.SVMMiddleWare;
import middleware.explore.DecisionTree;
import middleware.explore.FullScan;
import middleware.explore.GridPoints;
import middleware.explore.OptTree;
import middleware.explore.RandomSampling;
import middleware.user.Annotator;
import system.Context;
import system.JDBCUtil;
import system.SystemConfig;

public class Resume {

	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.out.println("usage: frontend_config_file backend_config_file mode debug previous_sample_file");
			System.exit(1);
		}
		
		SystemConfig config = new SystemConfig(args[0],args[1],args[2]);
		config.debug = Boolean.parseBoolean(args[3]);
		String preSamplesFileName = args[4];
		
		Context context = new Context();
		Pair <Connection, Statement> jdbcConnection = JDBCUtil.connectDB(config);
		context.conn = jdbcConnection.getKey();
		context.stmt = jdbcConnection.getValue();
		
		SVMMiddleWare middleWare = null;
		
		context.parseTruthPredicate(config, args[1]);
		Annotator annotator = new Annotator(config.where_attrs_with_class, context.truth);
		
		switch(config.exploreMethod) {
		case OPT_TREE:
			middleWare = new OptTree(config, context, annotator);
			break;
		case DECISION_TREE:
			middleWare = new DecisionTree(config, context, annotator);
			break;
		case FULL_SCAN:
			middleWare = new FullScan(config, context, annotator);
			break;
		case RANDOM:
			middleWare = new RandomSampling(config, context, annotator);
			break;
		case GRIDS:
			middleWare = new GridPoints(config, context, annotator);
			break;
		default:
			throw new Exception("explore mode can only be one of the followings: OPT_TREE, DECISION_TREE, FULL_SCAN, RANDOM, GRIDS");
		}
		
		middleWare.resume(preSamplesFileName);
		middleWare.close();
	}
}
