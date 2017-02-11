package metrics.threeset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.math3.util.Pair;

/**
 * each negative region has a dual simplex
 * @author lppeng
 *
 */
public class PointWiseComplementConvexHull {
	final int dim;
	public Vertex negVertex;
	HashSet<Facet> facets;
	
	public PointWiseComplementConvexHull(int dim, double [] negVertex, double [][] posVertex) throws IOException {
		this.dim = dim;
		if(posVertex.length<dim) {
			throw new IOException("Expecting " + dim + " vertices but was given " + posVertex.length);
		}
		this.negVertex = new Vertex(dim, negVertex);
		
		Vertex [] vertices = new Vertex [dim+1];
		vertices[0] = this.negVertex;
		for(int i=1; i<=dim; i++) { 
			vertices[i] = new Vertex(dim, posVertex[i-1]);
		}
		
		facets = new HashSet<Facet>();
		for(int i=1; i<=dim; i++) { // for each facet, do not include the i-th positive vertex
			Vertex [] facet = new Vertex [dim];
			facet[0] = this.negVertex; // each facet always has the negative vertex
			int index = 0;
			for(int j=1; j<=dim; j++) {
				if(j==i) {
					continue;
				}
				facet[++index] = vertices[j];
			}			
			facets.add(new Facet(dim, facet, vertices[i], false));
		}
		for(int i=dim; i<posVertex.length; i++) {
			if(posVertex[i]!=null) {
				addVertex(posVertex[i]);
			}
		}
	}
	
	
	public PointWiseComplementConvexHull(int dim, double [] negVertex, ConvexHull positiveRegion) throws IOException {
		this.dim = dim;
		this.negVertex = new Vertex(dim, negVertex);
		facets = new HashSet<Facet>();
		// step 1: find all visible facets
		ArrayList<Facet> visibleFacets = new ArrayList<Facet>();
		for(Facet f:positiveRegion.getFacets()) {
			if(f.isVisible(negVertex)) {
				visibleFacets.add(f);
			}
		}
		// step 2: find the horizon ridges
		HashMap<Ridge, Vertex> horizonRidges = new HashMap<Ridge,Vertex>();
		for(Facet f:visibleFacets) {
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
		Iterator<Ridge> ridgeItr = horizonRidges.keySet().iterator();
		while(ridgeItr.hasNext()) {
			Ridge r = ridgeItr.next();
			Facet f = new Facet(r,this.negVertex,horizonRidges.get(r), false);
			facets.add(f);
		}
	}
	
	public void addVertex(double [] point) throws IOException {
		if(point.length!=dim) {
			throw new IOException("cannot add a " + point.length +"-dimensional vertex to a " + dim + "-dimensional convex hull");
		}
		// step 1: find all visible facets
		ArrayList<Facet> invisibleFacets = new ArrayList<Facet>();
		for(Facet f:facets) {
			if(!f.isVisible(point)) {
				invisibleFacets.add(f);
			}
		}
		if(invisibleFacets.isEmpty()) {
			return;
		}
		else if(invisibleFacets.size()==facets.size()) {	
			throw new IOException("a positive point cannot be inside the negative region: " + negVertex);
		}
		else {
			// step 2: remove visible facets and find horizon ridges (that connects a visible facet and an invisible facet)
			HashMap<Ridge, Vertex> horizonRidges = new HashMap<Ridge,Vertex>();			
			for(Facet f:invisibleFacets) {
				facets.remove(f);
				Pair<Ridge, Vertex> [] ridges = f.getRidge(negVertex);
				for(int i=0; i<ridges.length; i++) {
					Pair<Ridge, Vertex> p = ridges[i];
					Ridge r = p.getKey();
					if(horizonRidges.containsKey(r)) {
						horizonRidges.remove(r); // each ridge connects two facets, if both facets are visible, it's not a horizon ridge
					}
					else {
						horizonRidges.put(r,p.getValue());
					}
				}
			}	
			// step 3: add new facets
			Vertex v = new Vertex(dim, point);
			Iterator<Ridge> ridgeItr = horizonRidges.keySet().iterator();
			while(ridgeItr.hasNext()) {
				Ridge r = ridgeItr.next();
				Facet f = new Facet(r,v,horizonRidges.get(r), false);
				facets.add(f);
			}
		}
	}
	
	public boolean containsPoint(double [] point) throws IOException {
		if(point.length!=dim) {
			throw new IOException("cannot judge whether a " + point.length +"-dimensional point is inside a " + dim + "-dimensional negative region");
		}
		for(Facet f:facets) {
			if(f.isVisible(point)) {
				return false;
			}
		}
		return true;
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
}
