package agents.rulebased;


import java.util.Set;
import models.AbstractModel;
import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class EquatePPS extends RuleBasedAgent{
	protected BidBundle _bidBundle;
	protected boolean BUDGET = false;
	protected double _PPS;
	protected double _alphaIncPPS;
	protected double _betaIncPPS;
	protected double _gammaIncPPS;
	protected double _deltaIncPPS;
	protected double _alphaDecPPS;
	protected double _betaDecPPS;
	protected double _gammaDecPPS;
	protected double _deltaDecPPS;
	protected double _initPPS;
	protected double _threshTS;

	public EquatePPS() {
		this(11.4635, 0.0020563, -0.140335, 0.954242, 0.986738, 0.00216053, 0.0283649, 0.952448, 0.703051, 2.37485, 1.1427, 0.944878, 1.33054, 1.89365, 0.838944, 1.40315, 0.383182, 1.23569);
	}
	
	public EquatePPS(double initPPS,
			double alphaIncPPS,
			double betaIncPPS,
			double gammaIncPPS,
			double deltaIncPPS,
			double alphaDecPPS,
			double betaDecPPS,
			double gammaDecPPS,
			double deltaDecPPS,
			double threshTS,
			double lambdaCapLow,
			double lambdaCapMed,
			double lambdaCapHigh,
			double lambdaBudgetLow,
			double lambdaBudgetMed,
			double lambdaBudgetHigh,
			double dailyCapMin,
			double dailyCapMax) {
		_initPPS = initPPS;
		_alphaIncPPS = alphaIncPPS;
		_betaIncPPS = betaIncPPS;
		_gammaIncPPS = gammaIncPPS;
		_deltaIncPPS = deltaIncPPS;
		_alphaDecPPS = alphaDecPPS;
		_betaDecPPS = betaDecPPS;
		_gammaDecPPS = gammaDecPPS;
		_deltaDecPPS = deltaDecPPS;
		_threshTS = threshTS;
		_lambdaCapLow = lambdaCapLow;
		_lambdaCapMed = lambdaCapMed;
		_lambdaCapHigh = lambdaCapHigh;
		_lambdaBudgetLow = lambdaBudgetLow;
		_lambdaBudgetMed = lambdaBudgetMed;
		_lambdaBudgetHigh = lambdaBudgetHigh;
		_dailyCapMin = dailyCapMin;
		_dailyCapMax = dailyCapMax;
	}



	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		_bidBundle = new BidBundle();
		if(_day < 2) { 
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}

		if (_day > 1 && _salesReport != null && _queryReport != null) {
			/*
			 * Equate PPS
			 */
			double sum = 0.0;
			for(Query query:_querySpace){
				sum+= _salesReport.getConversions(query);
			}

			if(Math.abs(sum - _dailyCapacity) <= _threshTS) {
				//Do Nothing
			}
			else if(sum <= _dailyCapacity) {
				_PPS *=  (1-(_alphaDecPPS*Math.abs(sum - _dailyCapacity)  +  _betaDecPPS) * Math.pow(_gammaDecPPS, _day*_deltaDecPPS));
			}
			else {
				_PPS *=  (1+(_alphaIncPPS*Math.abs(sum - _dailyCapacity)  +  _betaIncPPS) * Math.pow(_gammaIncPPS, _day*_deltaIncPPS));
			}
			
			if(Double.isNaN(_PPS) || _PPS <= 0) {
				_PPS = _initPPS;
			}
			if(_PPS > 15.0) {
				_PPS = 15.0;
			}
		}

		for(Query query: _querySpace){
			double targetCPC = getTargetCPC(query);
			_bidBundle.addQuery(query, getBidFromCPC(query, targetCPC), new Ad(), Double.MAX_VALUE);
		}

		if(BUDGET) {
			_bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
		}
		else {
			_bidBundle.setCampaignDailySpendLimit(Double.MAX_VALUE);
		}

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();

		_PPS = _initPPS;
	}

	protected double getTargetCPC(Query q){		

		double prConv;
		if(_day <= 6) {
			prConv = _baselineConversion.get(q);
		}
		else {
			prConv = _conversionPrModel.getPrediction(q);
		}

		double rev = _salesPrices.get(q);
		double CPC = (rev - _PPS)* prConv;
		CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));
		
		return CPC;
	}

	@Override
	public String toString() {
		return "EquatePPS";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquatePPS(_initPPS, _alphaIncPPS, _betaIncPPS,_gammaIncPPS,_deltaIncPPS, _alphaDecPPS, _betaDecPPS, _gammaDecPPS, _deltaDecPPS, _threshTS, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed,_lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
	}

}