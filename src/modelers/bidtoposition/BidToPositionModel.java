package modelers.bidtoposition;

import regressions.ModelPoint;

public abstract class BidToPositionModel {

	public abstract double getPrediction(Object[] given);
	
	public abstract void insertPoint(ModelPoint mp);
	
	public abstract void reset();
		
	public abstract void train();
	
}
