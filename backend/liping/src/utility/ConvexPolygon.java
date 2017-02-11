package utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ConvexPolygon, edges are linked counter-clock-wise
 * @author lppeng
 *
 */
public class ConvexPolygon {
	boolean DEBUG = false;
	
	public Vertex head; // head.next is the first node
	Vertex tail; // tail is the last node, tail.next=null
	public int vertexCount;
	
	public ConvexPolygon() {
		head = new Vertex(null);
		tail = head;
		vertexCount = 0;
	}
	
	public Vertex getNextNode(Vertex n) {
		if(n!=tail) {
			return n.next;
		}
		else {
			return head.next;
		}
	}
	
	public void addVertex(double [] point) throws IOException {
		if(point.length!=2) {
			throw new IOException(String.valueOf(point.length));
		}
		if(vertexCount<2) {
			Vertex vertex = new Vertex(point);
			tail.next = vertex;
			tail = tail.next;
			vertexCount = vertexCount + 1;
		}
		else {
			ArrayList<Vertex> edgesLeftToThePoint = findEdgesToTheLeft(point);
			if(edgesLeftToThePoint.size()==0) { 
				return; // point is inside the polygon, no need to modify the polygon
			}
			else {
				Vertex vertex = new Vertex(point);
				if(edgesLeftToThePoint.get(0)!=head.next) {	
					Vertex next = edgesLeftToThePoint.get(edgesLeftToThePoint.size()-1).next;
					edgesLeftToThePoint.get(0).next = vertex;
					vertex.next = next;
					if(edgesLeftToThePoint.get(edgesLeftToThePoint.size()-1)==tail) {
						tail = vertex;
					}
				}
				else {
					boolean disconnect = false;
					for(int i=1; i<edgesLeftToThePoint.size(); i++) {
						if(edgesLeftToThePoint.get(i-1).next!=edgesLeftToThePoint.get(i)) {
							disconnect = true;
							edgesLeftToThePoint.get(i).next = vertex;
							tail = vertex;
							head.next = edgesLeftToThePoint.get(i-1).next;
							break;
						}
					}
					if(!disconnect) {
						Vertex next = edgesLeftToThePoint.get(edgesLeftToThePoint.size()-1).next;
						head.next.next = vertex;
						vertex.next = next;
						if(edgesLeftToThePoint.get(edgesLeftToThePoint.size()-1)==tail) {
							System.err.println("shouldn't reach here");
							tail = vertex;
						}
					}
				}
				vertexCount = vertexCount - edgesLeftToThePoint.size() + 2;
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(vertexCount);
		sb.append(" : ");
		Vertex n = head.next;
		while(n!=null) {
//			System.out.println(Arrays.toString(n.vertex));
			sb.append(Arrays.toString(n.vertex));
			sb.append("->");
			n = n.next;
		}
		sb.delete(sb.length()-2, sb.length());
		return sb.toString();
	}
	
	public ArrayList<Vertex> findEdgesToTheLeft(double[] point) {
		ArrayList<Vertex> edgesLeftToThePoint = new ArrayList<Vertex>();
		Vertex n = head.next;
		while(n!=null) {
			if(isEdgeToTheLeft(n,point)) {
				edgesLeftToThePoint.add(n);
			}
			n = n.next;
		}
		if(DEBUG) {
			System.out.println("edges starting with vertices " + edgesLeftToThePoint.toString() 
				+ " are to the left of point" + Arrays.toString(point));
		}
		return edgesLeftToThePoint;
	}
	public boolean isEdgeToTheLeft(Vertex n, double [] point) {		
		// get the vector from the i-th vertex to the next vertex;
		double x1, y1;
		x1 = getNextNode(n).vertex[0] - n.vertex[0];
		y1 = getNextNode(n).vertex[1] - n.vertex[1];
		
		// rotate (x1,y1) 90 degree clock-wise
		double temp = x1;
		x1 = y1;
		y1 = -temp;
		
		// get the vector from the i-th vertex to the input point;
		double x2 = point[0]-n.vertex[0];
		double y2 = point[1]-n.vertex[1];
		
		double innerProduct = x1*x2+y1*y2;
		if(innerProduct>0) { // to the right
			return true;
		}
		else if(innerProduct==0){ // on the line
			if(point[0]<Math.min(n.vertex[0], getNextNode(n).vertex[0]) || 
					point[0]>Math.max(n.vertex[0], getNextNode(n).vertex[0]) || 
				point[1]<Math.min(n.vertex[1], getNextNode(n).vertex[1]) ||
					point[1]>Math.max(n.vertex[1], getNextNode(n).vertex[1])) { // outside the segment
				return true;
			}
		}			
		return false;
	}
	
	public boolean containsPoint(double [] point) throws IOException {
		if(point.length!=2) {
			throw new IOException(String.valueOf(point.length));
		}
		if(vertexCount==0) {
			return false;
		}
		Vertex n = head.next;
		while(n!=null) {
			// for the counter clockwise polygon, if a point is inside the polygon, all edges are on the right side of the point
			if(isEdgeToTheLeft(n,point)) { 
//				System.err.println(n.toString() + "->" + getNextNode(n).toString() + "\t" + Arrays.toString(point));
				return false;
			}
			n = n.next;
		}
		return true;
	}
	public static void main(String[] args) throws IOException {
		ConvexPolygon cp = new ConvexPolygon();
		double [][] v = {{666.370117,941.110596},{670.139221,940.443115},{690.017273,939.859497}};
		for(int i=0; i<v.length; i++ ) {
			cp.addVertex(v[i]);
			System.out.println(cp.toString());
		}
		double [][] p = {{669.273376,940.620728}};
		for(int i=0; i<p.length; i++) {
			System.out.println(cp.containsPoint(p[i]));
		}
	}

}
