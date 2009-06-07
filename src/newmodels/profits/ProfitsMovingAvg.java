package newmodels.profits;

import edu.umich.eecs.tac.props.Query;

public class ProfitsMovingAvg {
	
	protected Query _query;
	protected double _profit;
	protected final double _alpha = .75;
	protected double _latestSample;
	
	public ProfitsMovingAvg(Query query, double profit) {
		this._query = query;
		this._profit = profit;
	}
	
	public void update(double newSample) {
		_latestSample = newSample;
		_profit = _alpha*_latestSample + (1 - _alpha)*_profit;
	}
	
	public Query getQuery() {
		return _query;
	}
	
	public double getProfit() {
		return _profit;
	}
	
	public double getLatestSample() {
		return _latestSample;
	}

}
