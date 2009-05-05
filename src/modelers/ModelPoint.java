package modelers;


public class ModelPoint {
	protected Object[] _given;
	protected double _toBePredicted;
	
	public ModelPoint(Object[] given, double toBePredicted) {
		_given = given;
		_toBePredicted = toBePredicted;
	}	
	
	public Object[] getGiven() {
		return _given;
	}
	
	public double getToBePredicted() {
		return _toBePredicted;
	}
}
