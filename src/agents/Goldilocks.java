package agents;

import java.util.HashMap;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public abstract class Goldilocks extends RuleBasedAgent {

	protected BidBundle _bidBundle;
	protected HashMap<Query, Double> _desiredSales;
	protected final boolean TARGET = false;
	protected final boolean BUDGET = true;
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
		
		_desiredSales = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_desiredSales.put(q, _dailyQueryCapacity);
		}
	}

	protected void adjustPM() {
		for(Query q : _querySpace) {
			double tmp = _PM.get(q);
			// if we does not get enough clicks (and bad position), then decrease PM
			// (increase bids, and hence slot)
			if (_salesReport.getConversions(q) >= _desiredSales.get(q)) {
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
			return _budgetModifier*targetCPC * _desiredSales.get(q) / _conversionPrModel.getPrediction(q);
		}
		else {
			return _budgetModifier*targetCPC * _desiredSales.get(q) / _baselineConversion.get(q);
		}
	}
}
