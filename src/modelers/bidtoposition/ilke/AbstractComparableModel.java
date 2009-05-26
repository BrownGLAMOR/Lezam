package modelers.bidtoposition.ilke;


/**
 * Extend this class if you want to use ModelComparator with your model
 * @author ilkekaya
 *
 */
public abstract class AbstractComparableModel {
		
	public abstract void insertPoint(ModelDataPoint mp);
	
	public abstract void train();
	
	public abstract double getPrediction(Object[] given);
	
	public abstract void reset();
	
	
}
