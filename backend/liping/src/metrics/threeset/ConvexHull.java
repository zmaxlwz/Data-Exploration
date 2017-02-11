package metrics.threeset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import utility.LinearAlgebra;

/**
 * A convex hull that can be updated by adding vertices
 */
public class ConvexHull {
	private final int dim;
	private HashSet<Facet> facets;
	private final long dimFactorial;
	
	public ConvexHull (int dim, double[][] simplex) throws IOException {
		this.dim = dim;
		long _dimFactorial=1;
		for(int i=2; i<=dim; i++) {
			_dimFactorial *=i;
		}
		dimFactorial = _dimFactorial;
		
		if (simplex.length != dim+1) {
			throw new IOException("Expected number of vertices = "+dim+1+", but get "+simplex.length+" vertices");
		}
		
		Vertex [] vertices = new Vertex [simplex.length];
		for(int i=0; i<simplex.length; i++) { 
			vertices[i] = new Vertex(dim, simplex[i]);
		}
		
		facets = new HashSet<Facet>();
		for(int i=0; i<simplex.length; i++) { // for each facet, do not include vertex i
			Vertex [] facet = new Vertex [dim];
			int index = -1;
			for(int j=0; j<simplex.length; j++) {
				if(j==i) {
					continue;
				}
				facet[++index] = vertices[j];
			}			
			facets.add(new Facet(dim, facet, vertices[i], true));
		}
	}
	
	public void addVertex(double [] point) throws IOException {	
		if(point.length!=dim) {
			throw new IOException("cannot add a " + point.length +"-dimensional vertex to a " + dim + "-dimensional convex hull");
		}
		// step 1: find all visible facets
		ArrayList<Facet> visibleFacets = new ArrayList<Facet>();
		for(Facet f:facets) {
			if(f.isVisible(point)) {
				visibleFacets.add(f);
			}
		}
		if(visibleFacets.isEmpty()) {
			return;
		}
		// step 2: remove visible facets and find horizon ridges (that connects a visible facet and an invisible facet)
		HashMap<Ridge, Vertex> horizonRidges = new HashMap<Ridge,Vertex>();
		for(Facet f:visibleFacets) {
			facets.remove(f);
			Ridge [] ridges = f.getRidge();
			for(int i=0; i<ridges.length; i++) {
				Ridge r = ridges[i];
				if(horizonRidges.containsKey(r)) {
					horizonRidges.remove(r); // each ridge connects two facets, if both facets are visible, it's not a horizon ridge
				}
				else {
					horizonRidges.put(r,f.getVertices()[i]);
				}
			}
		}		
		// step 3: add new facets
		Vertex v = new Vertex(dim, point);
		Iterator<Ridge> ridgeItr = horizonRidges.keySet().iterator();
		while(ridgeItr.hasNext()) {
			Ridge r = ridgeItr.next();
			Facet f = new Facet(r,v,horizonRidges.get(r), true);
			facets.add(f);
		}
	}
	
	public HashSet<Facet> getFacets() {
		return facets;
	}
	
	public boolean containsPoint(double [] point) throws IOException {
		if(point.length!=dim) {
			throw new IOException("cannot judge whether a " + point.length +"-dimensional point is inside a " + dim + "-dimensional convex hull");
		}
		for(Facet f:facets) {
			if(f.isVisible(point)) {
				return false;
			}
		}
		return true;
	}
	
	public double volume() throws IOException {
		if(facets==null || facets.size()<dim+1) {
			return 0;
		}
		Iterator<Facet> itr = facets.iterator();
		Facet firstFacet = itr.next();
		Vertex v0 = firstFacet.getVertices()[0];
		
		double volume = 0;
		while(itr.hasNext()) {
			Facet f = itr.next();
			Vertex [] vs = f.getVertices();
			
			boolean containsV0 = false;
			for(Vertex v:vs) {
				if(v==v0) {
					containsV0 = true;
					break;
				}
			}
			
			if(!containsV0) {
				double [][] matrix = new double [dim][dim];
				for(int i=0; i<dim; i++) {
					for(int j=0; j<dim; j++) {
						matrix[i][j] = vs[i].getValues()[j] - v0.getValues()[j];
					}
				}
				volume = volume + Math.abs(LinearAlgebra.determinant(matrix))/dimFactorial;
			}
		}
		return volume;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(facets.size());
		sb.append(" facets\n");
		for(Facet f:facets) {
			sb.append(f.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static void main(String [] args) throws IOException {
		double [][] v = {{2,0,0},{0,2,0},{0,0,2},{0,0,0}};
		ConvexHull ch = new ConvexHull(3,v);
		double [][] newv = {{0,2,2},{2,0,2},{2,2,0},{2,2,2}};
		for(int i=0; i<newv.length; i++) {
			ch.addVertex(newv[i]);
		}
		System.out.println(ch.volume());
	}
}
