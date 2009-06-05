package newmodels.profits;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;

public abstract class AbstractProfitsModel extends AbstractModel{
	Query _query;
	double _profit;
	
	public AbstractProfitsModel(Query query, double profit) {
		this._query = query;
		this._profit = profit;
	}
	
	public abstract double update(double newSample);
	
	public double getProfit() {
		return _profit;
	}
	
}
