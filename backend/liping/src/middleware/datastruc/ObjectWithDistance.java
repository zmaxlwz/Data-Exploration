package middleware.datastruc;

public class ObjectWithDistance <T> {
	private double distance;
	private T object;
	public ObjectWithDistance(double distance, T object) {
		this.distance = distance;
		this.object = object;
	}
	public double getDistance () {
		return distance;
	}
	public T getObject () {
		return object;
	}
}
