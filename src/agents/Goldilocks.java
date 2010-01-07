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
	protected double _alphaIncTS;
	protected double _betaIncTS;
	protected double _alphaDecTS;
	protected double _betaDecTS;
	protected double _alphaIncPM;
	protected double _betaIncPM;
	protected double _alphaDecPM;
	protected double _betaDecPM;
	protected double _initPM;
	protected HashMap<Query, Double> _PM;

	public Goldilocks(double alphaIncTS, double betaIncTS, double alphaDecTS, double betaDecTS, double initPM,double alphaIncPM, double betaIncPM, double alphaDecPM, double betaDecPM, double budgetModifier) {
		_alphaIncTS = alphaIncTS;
		_betaIncTS = betaIncTS;
		_alphaDecTS = alphaDecTS;
		_betaDecTS = betaDecTS;
		_alphaIncPM = alphaIncPM;
		_betaIncPM = betaIncPM;
		_alphaDecPM = alphaDecPM;
		_betaDecPM = betaDecPM;
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
			if (_salesReport.getConversions(q) >= _salesDistribution.get(q)*_dailyCapacity) {
				tmp *= (1+_alphaIncPM * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaIncPM);
			} else {
				tmp *= (1-(_alphaDecPM * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaDecPM));
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
