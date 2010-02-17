package agents.rulebased;


import java.util.Set;
import models.AbstractModel;
import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class EquatePR extends RuleBasedAgent{
	
	protected BidBundle _bidBundle;
	protected boolean BUDGET = false;
	protected double _PR;
	protected double _alphaIncPR;
	protected double _betaIncPR;
	protected double _gammaIncPR;
	protected double _deltaIncPR;
	protected double _alphaDecPR;
	protected double _betaDecPR;
	protected double _gammaDecPR;
	protected double _deltaDecPR;
	protected double _initPR;
	protected double _threshTS;
	
	public EquatePR(double initPR,
			double alphaIncPR,
			double betaIncPR,
			double gammaIncPR,
			double deltaIncPR,
			double alphaDecPR,
			double betaDecPR,
			double gammaDecPR,
			double deltaDecPR,
			double threshTS,
			double lambdaCapLow,
			double lambdaCapMed,
			double lambdaCapHigh,
			double lambdaBudgetLow,
			double lambdaBudgetMed,
			double lambdaBudgetHigh,
			double dailyCapMin,
			double dailyCapMax) {
		_initPR = initPR;
		_alphaIncPR = alphaIncPR;
		_betaIncPR = betaIncPR;
		_gammaIncPR = gammaIncPR;
		_deltaIncPR = deltaIncPR;
		_alphaDecPR = alphaDecPR;
		_betaDecPR = betaDecPR;
		_gammaDecPR = gammaDecPR;
		_deltaDecPR = deltaDecPR;
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
			 * Equate PMs
			 */
			double sum = 0.0;
			for(Query query:_querySpace){
				sum+= _salesReport.getConversions(query);
			}

			if(Math.abs(sum - _dailyCapacity) <= _threshTS) {
				//Do Nothing
			}
			else if(sum <= _dailyCapacity) {
				_PR *=  (1-(_alphaDecPR*Math.abs(sum - _dailyCapacity)  +  _betaDecPR) * Math.pow(_gammaDecPR, _day*_deltaDecPR));
			}
			else {
				_PR *=  (1+(_alphaIncPR*Math.abs(sum - _dailyCapacity)  +  _betaIncPR) * Math.pow(_gammaIncPR, _day*_deltaIncPR));
			}
			
			if(Double.isNaN(_PR)) {
				_PR = _initPR;
			}
			if(_PR <= 1.0) {
				_PR = 1.0;
			}
		}

		for(Query query: _querySpace){
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, getBidFromCPC(query, targetCPC));
		}

		if(BUDGET) {
			_bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
		}

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();

		_PR = _initPR;
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
		double CPC = (rev * prConv)/_PR;
		CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));
		
		return CPC;
	}

	@Override
	public String toString() {
		return "EquatePR";
	}

	@Override
	public AbstractAgent getCopy() {
		return new EquatePR(_initPR, _alphaIncPR, _betaIncPR,_gammaIncPR,_deltaIncPR, _alphaDecPR, _betaDecPR, _gammaDecPR, _deltaDecPR, _threshTS, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed,_lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
	}

}