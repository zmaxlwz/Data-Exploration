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
import middleware.user.DemoUser;
import system.Context;
import system.JDBCUtil;
import system.SystemConfig;

public class RunDemoSocket {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("usage: user_port opt_config_file_name");
			System.exit(1);
		}
		System.out.println("Welcome to Demo!");
		DemoUser demoUser = new DemoUser(Integer.valueOf(args[0]));
		System.out.println("a new demo user...");
		SystemConfig config = new SystemConfig(demoUser.in,args[1],"DEMO");
		System.out.println("configured...");
		
		Context context = new Context();
		Pair <Connection, Statement> jdbcConnection = JDBCUtil.connectDB(config);
		context.conn = jdbcConnection.getKey();
		context.stmt = jdbcConnection.getValue();
		System.out.println("connected to DB...");
		
		SVMMiddleWare middleWare = null;
		
		switch(config.exploreMethod) {
		case OPT_TREE:
			middleWare = new OptTree(config, context, demoUser);
			break;
		case DECISION_TREE:
			middleWare = new DecisionTree(config, context, demoUser);
			break;
		case FULL_SCAN:
			middleWare = new FullScan(config, context, demoUser);
			break;
		case RANDOM:
			middleWare = new RandomSampling(config, context, demoUser);
			break;
		case GRIDS:
			middleWare = new GridPoints(config, context, demoUser);
			break;
		default:
			throw new Exception("explore mode can only be one of the followings: OPT_TREE, DECISION_TREE, FULL_SCAN, RANDOM, GRIDS");
		}
		middleWare.demoRun();
		middleWare.close();	
	}

}
