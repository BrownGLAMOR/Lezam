package agents;

import java.util.HashMap;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public abstract class Goldilocks extends RuleBasedAgent {

	protected BidBundle _bidBundle;
	protected HashMap<Query, Double> _salesDistribution;
	protected final boolean TARGET = false;
	protected final boolean BUDGET = false;
	protected final boolean DAILYBUDGET = true;
	protected double _incTS;
	protected double _decTS;
	protected double _decPM;
	protected double _incPM;
	protected double _minPM;
	protected double _maxPM;
	protected double _initPM;
	protected HashMap<Query, Double> _PM;

	public Goldilocks(double incTS, double decTS, double initPM,double decPM, double incPM, double minPM, double maxPM, double budgetModifier) {
		_incTS = incTS;
		_decTS = decTS;
		_decPM = decPM;
		_incPM = incPM;
		_minPM = minPM;
		_maxPM = maxPM;
		_initPM = initPM;
		_budgetModifier = budgetModifier;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		
		_PM = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_PM.put(q, _initPM);
		}
		
		_salesDistribution = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_salesDistribution.put(q, 1.0/_querySpace.size());
		}
	}

	protected void adjustPM() {
		for(Query q : _querySpace) {
			double tmp = _PM.get(q);
			// if we does not get enough clicks (and bad position), then decrease PM
			// (increase bids, and hence slot)
			if (_salesReport.getConversions(q) >= _salesDistribution.get(q)*_dailyCapacity) {
				tmp = _PM.get(q) * _incPM;
				tmp = Math.min(_maxPM, tmp);
			} else {
				// if we get too many clicks (and good position), increase
				// PM(decrease bids and hence slot)
				tmp = _PM.get(q) * _decPM;
				tmp = Math.max(_minPM, tmp);
			}
			_PM.put(q, tmp);
		}
	}
	
	@Override
	protected double getDailySpendingLimit(Query q, double targetCPC) {
		if(_day >= 6 && _conversionPrModel != null) {
			return (_budgetModifier*targetCPC * _salesDistribution.get(q)*_dailyCapacity) / _conversionPrModel.getPrediction(q);
		}
		else {
			return (_budgetModifier*targetCPC * _salesDistribution.get(q)*_dailyCapacity) / _baselineConversion.get(q);
		}
	}
}
