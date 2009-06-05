package newmodels.profits;

import edu.umich.eecs.tac.props.Query;

public class ProfitsMovingAvg extends AbstractProfitsModel {
	
	protected final double _alpha = .75;
	public ProfitsMovingAvg(Query query, double profit) {
		super(query, profit);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double update(double newSample) {
		_profit = _alpha*newSample + (1 - _alpha)*_profit;
		return _profit;
	}

}
