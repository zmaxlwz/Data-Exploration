package trees;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

import middleware.SVMMiddleWare;
import middleware.datastruc.AttributeValue;
import middleware.datastruc.DenseInstanceWithID;
import middleware.datastruc.ObjectWithDistance;
//import SVMAlgos.Kernel;
import SVMAlgos.libsvm.svm_model;
import spatialindex.rtree.Node;
import spatialindex.rtree.RTree;
import spatialindex.spatialindex.Region;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IBuffer;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;
import spatialindex.storagemanager.RandomEvictionsBuffer;
import system.SystemConfig;
import tuplereader.TupleReader;
import weka.core.Attribute;
import weka.core.Instance;

public class DeltaBoundRTree {	
	
	final SystemConfig config;
	final AttributeValue [] temp;
	
	final RTree tree;
	final PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> topK;
	public int nodeCount;
	public int tupleCount;

	double minUpperBound;
	
	public DeltaBoundRTree(SystemConfig config) throws Exception {
		if(config.debug) {
			System.out.println("loading indexes of table <" + config.tbl_name + "> on attributes " + config.where_attrs);
		}
		this.config = config;
		temp = new AttributeValue[config.where_attrs.size()];
		
		PropertySet treePS = new PropertySet();
		treePS.setProperty("Dimension", config.where_attrs.size());
		treePS.setProperty("FillFactor", 0.7);
		int fanout = (SystemConfig.PAGE_SIZE-12)/(16*config.where_attrs.size()+8+12); // TODO: check whether fanout is correctly computed based on the data struc of nodes in the tree
		treePS.setProperty("IndexCapacity", fanout);
		treePS.setProperty("LeafCapacity", fanout);
		if(config.debug) {
			System.out.println(treePS.getProperty("IndexCapacity") + "\t" + treePS.getProperty("LeafCapacity"));
		}
		
		File f;
		// Create a disk based storage manager.
		PropertySet filePS = new PropertySet();				
		filePS.setProperty("PageSize", SystemConfig.PAGE_SIZE);
		String indexFileName = config.tbl_name;
		for(Attribute a:config.where_attrs) {
			indexFileName += "_" + a.name();
		}
		filePS.setProperty("FileName", indexFileName); // .idx and .dat extensions will be added.
		
		f = new File(indexFileName + ".idx");
		if(f.exists()) {
			filePS.setProperty("Overwrite", false);
			treePS.setProperty("IndexIdentifier", (long)1);
		}
		else {
			throw new Exception("construct an index first!");
		}
		
		IStorageManager diskfile = null;
		try {
			diskfile = new DiskStorageManager(filePS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		IBuffer file = new RandomEvictionsBuffer(diskfile, 800000, false);
			// applies a main memory random buffer on top of the persistent storage manager
			// (LRU buffer, etc can be created the same way).
		tree = new RTree(treePS,file);
		
		if(config.debug) {
			System.out.println(tree.toString());
		}
		
		topK = new PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> (1,new Comparator<ObjectWithDistance<DenseInstanceWithID>>() {
		    public int compare(ObjectWithDistance<DenseInstanceWithID> n1, ObjectWithDistance<DenseInstanceWithID> n2) {
		        double d1 = n1.getDistance();
		        double d2 = n2.getDistance();
		        if(d1<d2) {
		        	return 1;
		        }
		        else if(d1==d2) {
		        	return 0;
		        }
		        else {
		        	return -1;
		        }
		    }
		});
	}
	
	public DeltaBoundRTree(SystemConfig config, TupleReader tr) throws Exception {		
		if(config.debug) {
			System.out.println("loading indexes of table <" + config.tbl_name + "> on attributes " + config.where_attrs);
		}
		this.config = config;
		temp = new AttributeValue[config.where_attrs.size()];
		
		boolean nonExist = true; 
		// Create a new, empty, RTree with minimum load 70%, using "file" as
		// the StorageManager and the RSTAR splitting policy.
		PropertySet treePS = new PropertySet();
		treePS.setProperty("Dimension", config.where_attrs.size());
		treePS.setProperty("FillFactor", 0.7);
		int fanout = (SystemConfig.PAGE_SIZE-12)/(16*config.where_attrs.size()+8+12); // TODO: check whether fanout is correctly computed based on the data struc of nodes in the tree
		treePS.setProperty("IndexCapacity", fanout);
		treePS.setProperty("LeafCapacity", fanout);
		System.out.println(treePS.getProperty("IndexCapacity") + "\t" + treePS.getProperty("LeafCapacity"));		
		
		File f;
		// Create a disk based storage manager.
		PropertySet filePS = new PropertySet();				
		filePS.setProperty("PageSize", SystemConfig.PAGE_SIZE);
		String indexFileName = config.tbl_name;
		for(Attribute a:config.where_attrs) {
			indexFileName += "_" + a.name();
		}
		filePS.setProperty("FileName", indexFileName); // .idx and .dat extensions will be added.
		
		f = new File(indexFileName + ".idx");
		if(f.exists()) {
			nonExist = false;
			filePS.setProperty("Overwrite", false);
			treePS.setProperty("IndexIdentifier", (long)1);
		}
		else {
			filePS.setProperty("Overwrite", true);
		}

		IStorageManager diskfile = null;
		try {
			diskfile = new DiskStorageManager(filePS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		IBuffer file = new RandomEvictionsBuffer(diskfile, 800000, false);
			// applies a main memory random buffer on top of the persistent storage manager
			// (LRU buffer, etc can be created the same way).
		tree = new RTree(treePS,file);
		if(nonExist) {
			construct(tr);
		}
		if(config.debug) {
			System.out.println(tree.toString());
		}
		
		topK = new PriorityQueue<ObjectWithDistance<DenseInstanceWithID>> (1,new Comparator<ObjectWithDistance<DenseInstanceWithID>>() {
		    public int compare(ObjectWithDistance<DenseInstanceWithID> n1, ObjectWithDistance<DenseInstanceWithID> n2) {
		        double d1 = n1.getDistance();
		        double d2 = n2.getDistance();
		        if(d1<d2) {
		        	return 1;
		        }
		        else if(d1==d2) {
		        	return 0;
		        }
		        else {
		        	return -1;
		        }
		    }
		});
	}
	
	public void construct(TupleReader reader) {
		if(reader==null) {
			return;
		}
		double[] point = new double[config.where_attrs.size()];
		
		DenseInstanceWithID t;
		while(( t= (DenseInstanceWithID)reader.getNext())!=null) {
			for(int i = 0; i < t.numAttributes(); i++) { 							
				point [i] = t.value(i);		
			}
			Region r = new Region(point, point);
			tree.insertData(null, r, t.getID());
		}
		tree.flush();
	}
	
	/*public Instances branch_and_bound(final svm_model svmModel, HashSet<Long> labeledKeys) {
		return branch_and_bound(svmModel,labeledKeys,Double.MAX_VALUE);
	}
	
	public Instances branch_and_bound(final svm_model svmModel, HashSet<Long> labeledKeys, double threshold) {
		minUpperBound = threshold;
		return retrieve(svmModel,labeledKeys);
	}
	
	public Instances retrieve(final svm_model svmModel, HashSet<Long> labeledKeys) {
		Instances samples = new Instances(null, config.where_attrs_with_class, 0);				
		samples.setClass(config.where_attrs_with_class.get(config.where_attrs_with_class.size()-1));
		
		nodeCount = 0;
		tupleCount = 0;
		topK.clear();
		
		Node root = tree.readNode(tree.l_m_rootID);
		branch_bound_visit(root, svmModel, labeledKeys);

		Iterator<ObjectWithDistance<DenseInstanceWithID>> itr_topK = topK.iterator();
		while(itr_topK.hasNext()) {
			DenseInstanceWithID sample = itr_topK.next().getObject();
	    	samples.add(sample);
		}
		return samples;
	}
	
	private void branch_bound_visit(Node node, svm_model model, HashSet<Long> labeledKeys) {
		nodeCount ++;
		if (node.isLeaf()) {	//Leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				tupleCount ++;
				if(!labeledKeys.contains(node.l_m_pIdentifier[cChild])) {
					Instance sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),node.getChildIdentifier(cChild));
					for(int i=0; i<config.where_attrs.size(); i++) {
						sample.setValue(i, node.m_pMBR[cChild].getLow(i));
					}
					topK.add(new ObjectWithDistance<DenseInstanceWithID>(Math.abs(SVMMiddleWare.f_x(model, (DenseInstanceWithID)sample)),(DenseInstanceWithID)sample));			    	
			    	if(topK.size()>config.samples_per_round) {
			    		topK.remove();
			    	}
				}
			}
		}
		else {	//Non-leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				double bound = findLowerBound(model, node.m_pMBR[cChild]);
				if(bound<=minUpperBound) {// TODO: only works for top-1 
					if (topK.size()<config.samples_per_round || bound<topK.peek().getDistance()) {  
						branch_bound_visit(tree.readNode(node.l_m_pIdentifier[cChild]), model, labeledKeys);
					}
				}
			}
		}
	}*/
	
	public HashSet<DenseInstanceWithID> branch_and_bound(final svm_model svmModel, HashSet<Instance> labeledSamples) {
		return branch_and_bound(svmModel,labeledSamples,Double.MAX_VALUE);
	}
	
	public HashSet<DenseInstanceWithID> branch_and_bound(final svm_model svmModel, HashSet<Instance> labeledSamples, double threshold) {
		minUpperBound = threshold;
		return retrieve(svmModel,labeledSamples);
	}
	
	public HashSet<DenseInstanceWithID> retrieve(final svm_model svmModel, HashSet<Instance> labeledSamples) {
		HashSet<DenseInstanceWithID> samples = new HashSet<DenseInstanceWithID>();						
		nodeCount = 0;
		tupleCount = 0;
		topK.clear();
		
		Node root = tree.readNode(tree.l_m_rootID);
		branch_bound_visit(root, svmModel, labeledSamples);

		Iterator<ObjectWithDistance<DenseInstanceWithID>> itr_topK = topK.iterator();
		while(itr_topK.hasNext()) {
			DenseInstanceWithID sample = itr_topK.next().getObject();
	    	samples.add(sample);
		}
		return samples;
	}
	private void branch_bound_visit(Node node, svm_model model, HashSet<Instance> labeledSamples) {
		nodeCount ++;
		if (node.isLeaf()) {	//Leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				tupleCount ++;
				Instance sample = new DenseInstanceWithID(config.where_attrs_with_class.size(),node.getChildIdentifier(cChild));
				for(int i=0; i<config.where_attrs.size(); i++) {
					sample.setValue(i, node.m_pMBR[cChild].getLow(i));
				}
				if(!labeledSamples.contains(sample)) {		
					topK.add(new ObjectWithDistance<DenseInstanceWithID>(Math.abs(SVMMiddleWare.f_x(model, (DenseInstanceWithID)sample)),(DenseInstanceWithID)sample));			    	
			    	if(topK.size()>config.samples_per_round) {
			    		topK.remove();
			    	}
				}
			}
		}
		else {	//Non-leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				double bound = findLowerBound(model, node.m_pMBR[cChild]);
				if(bound<=minUpperBound) {// TODO: only works for top-1 
					if (topK.size()<config.samples_per_round || bound<topK.peek().getDistance()) {  
						branch_bound_visit(tree.readNode(node.l_m_pIdentifier[cChild]), model, labeledSamples);
					}
				}
			}
		}
	}
	
	public double findLowerBound(svm_model model, Region r) {
		double min = Double.MAX_VALUE;
		int numPoints = (int)Math.pow(config.opt_tree_node_granularity, 1.0/r.getDimension());
		int granularity = (int)Math.pow(numPoints,r.getDimension());
		for(int i=0; i<granularity; i++) {
			int _i = i;
			for(int j=0; j<r.getDimension(); j++) {
				temp[j] = new AttributeValue(j, r.getLow(j)+(_i%numPoints)*(r.getHigh(j)-r.getLow(j))/(numPoints-1));
				_i = _i/numPoints;
			}
			double dist = Math.abs(SVMMiddleWare.f_x(model, temp));
			min = Math.min(min, dist);
		}
		return min;
	}
	
/*	public boolean find(long id, Node node) {
		if (node.isLeaf()) {	//Leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				if(node.l_m_pIdentifier[cChild]==id) {
					System.out.println(node.m_level + "\t" + node.getIdentifier() + "\t" + node.m_pMBR[cChild].toString());
					return true;
				}
			}
		}
		else {	//Non-leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				if(find(id, tree.readNode(node.l_m_pIdentifier[cChild]))) {
					System.out.println(node.m_level + "\t" + node.getIdentifier() + "\t" + node.m_pMBR[cChild].toString());
					return true;
				};
			}
		}
		return false;
	}
	
	public boolean find(Region r, Node node) {
		if (node.isLeaf()) {	//Leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				if(overlap(r,node.m_pMBR[cChild])) {
					System.out.println(node.m_pMBR[cChild].toString());
					return true;
				}
			}
		}
		else {	//Non-leaf node
			for (int cChild = 0; cChild < node.m_children; cChild++) {
				if (overlap(r, node.m_pMBR[cChild])) {  
					if(find(r, tree.readNode(node.l_m_pIdentifier[cChild]))) {
						System.out.println(node.m_pMBR[cChild].toString());
						return true;
					};
				}
			}
		}
		return false;
	}
	
	private boolean overlap(Region r, Region MBR) {
		for(int i=0; i<r.getDimension(); i++) {
			if(r.getLow(i)>MBR.getHigh(i) || r.getHigh(i)<MBR.getLow(i)) {
				return false;
			}
		}
		return true;
	}*/
}
