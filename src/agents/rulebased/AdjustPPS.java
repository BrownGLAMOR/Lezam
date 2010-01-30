package agents.rulebased;

import java.util.HashMap;
import java.util.Set;

import models.AbstractModel;
import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class AdjustPPS extends RuleBasedAgent {
	
	protected BidBundle _bidBundle;
	protected final boolean BUDGET = true;
	protected final boolean DAILYBUDGET = true;
	protected double _initPPS;
	protected double _alphaIncTS;
	protected double _betaIncTS;
	protected double _gammaIncTS;
	protected double _deltaIncTS;
	protected double _alphaDecTS;
	protected double _betaDecTS;
	protected double _gammaDecTS;
	protected double _deltaDecTS;
	protected double _alphaIncPPS;
	protected double _betaIncPPS;
	protected double _gammaIncPPS;
	protected double _deltaIncPPS;
	protected double _alphaDecPPS;
	protected double _betaDecPPS;
	protected double _gammaDecPPS;
	protected double _deltaDecPPS;
	protected double _threshTS;
	protected double _threshPPS;
	protected HashMap<Query, Double> _PPS;
	
	public AdjustPPS() {
		this(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
	}

	public AdjustPPS(double initPPS,
			double alphaIncTS,
			double betaIncTS,
			double gammaIncTS,
			double deltaIncTS,
			double alphaDecTS,
			double betaDecTS,
			double gammaDecTS,
			double deltaDecTS,
			double alphaIncPPS,
			double betaIncPPS,
			double gammaIncPPS,
			double deltaIncPPS,
			double alphaDecPPS,
			double betaDecPPS,
			double gammaDecPPS,
			double deltaDecPPS,
			double threshTS,
			double threshPPS,
			double lambdaCapLow,
			double lambdaCapMed,
			double lambdaCapHigh,
			double lambdaBudgetLow,
			double lambdaBudgetMed,
			double lambdaBudgetHigh,
			double dailyCapMin,
			double dailyCapMax) {
		_initPPS = initPPS;
		_alphaIncTS = alphaIncTS;
		_betaIncTS = betaIncTS;
		_gammaIncTS = gammaIncTS;
		_deltaIncTS = deltaIncTS;
		_alphaDecTS = alphaDecTS;
		_betaDecTS = betaDecTS;
		_gammaDecTS = gammaDecTS;
		_deltaDecTS = deltaDecTS;
		_alphaIncPPS = alphaIncPPS;
		_betaIncPPS = betaIncPPS;
		_gammaIncPPS = gammaIncPPS;
		_deltaIncPPS = deltaIncPPS;
		_alphaDecPPS = alphaDecPPS;
		_betaDecPPS = betaDecPPS;
		_gammaDecPPS = gammaDecPPS;
		_deltaDecPPS = deltaDecPPS;
		_threshTS = threshTS;
		_threshPPS = threshPPS;
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
	public void initBidder() {
		super.initBidder();
		
		_PPS = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_PPS.put(q, _initPPS);
		}
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		_bidBundle = new BidBundle();

		if(_day < 2) { 
			_bidBundle = new BidBundle();
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}

		/*
		 * Calculate Average PPS
		 */
		double avgPPS = 0.0;
		double totWeight = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0 &&
					_salesReport.getConversions(q) != 0) { //the last check is unnecessary, but safe
				double weight = _salesDistribution.get(q);
				avgPPS += ((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q))*weight;
				totWeight+=weight;
			}
		}
		avgPPS /= totWeight;
		if(Double.isNaN(avgPPS)) {
			avgPPS = _initPPS;
		}

		/*
		 * Adjust Target Sales
		 */
		double totDesiredSales = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0 &&
					_salesReport.getConversions(q) != 0) { //the last check is unnecessary, but safe
				double PPSq = (_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q);
				if(Math.abs(PPSq - avgPPS) <= _threshPPS) {
					//Do Nothing
				}
				else if (PPSq < avgPPS) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1-(_alphaDecTS * Math.abs(PPSq - avgPPS) +  _betaDecTS)* Math.pow(_gammaDecTS, _day*_deltaDecTS)));
				}
				else {
					if(_dailyCapacity != 0) {
						_salesDistribution.put(q, _salesDistribution.get(q)*(1+(_alphaIncTS *Math.abs(PPSq - avgPPS)  +  _betaIncTS)* Math.pow(_gammaIncTS, _day*_deltaIncTS)));
					}
				}
			}
			totDesiredSales += _salesDistribution.get(q);
		}

		/*
		 * Normalize
		 */
		double normFactor = 1.0/totDesiredSales;
		for(Query q : _querySpace) {
			_salesDistribution.put(q, _salesDistribution.get(q)*normFactor);
		}

		/*
		 * Adjust PPS
		 */
		if(_day > 1) {
			adjustPPS();
		}

		for (Query query : _querySpace) {
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, getBidFromCPC(query, targetCPC));

			if(DAILYBUDGET) {
				_bidBundle.setDailyLimit(query, getDailySpendingLimit(query,targetCPC));
			}

		}
		if(BUDGET) {
			_bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
		}
		return _bidBundle;
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
		double CPC = (rev - _PPS.get(q))* prConv;
		CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));
		return CPC;
	}

	protected void adjustPPS() {
		for(Query q : _querySpace) {
			double tmp = _PPS.get(q);
			double sales = _salesReport.getConversions(q);
			double dailyCap = _salesDistribution.get(q)*_dailyCapacity;
			if(Math.abs(sales - dailyCap) <= _threshTS) {
				//Do Nothing
			}
			else if (sales >= dailyCap) {
				tmp *= (1+(_alphaIncPPS * Math.abs(sales - dailyCap) +  _betaIncPPS) * Math.pow(_gammaIncPPS, _day*_deltaIncPPS));
			} else {
				tmp *= (1-(_alphaDecPPS * Math.abs(sales - dailyCap) +  _betaDecPPS) * Math.pow(_gammaDecPPS, _day*_deltaDecPPS));
			}
			if(Double.isNaN(tmp) || tmp <= 0) {
				tmp = _initPPS;
			}
			if(tmp > 15.0) {
				tmp = 15.0;
			}
			_PPS.put(q, tmp);
		}
	}

	@Override
	public String toString() {
		return "AdjustPPS";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustPPS(_initPPS,_alphaIncTS,_betaIncTS,_gammaIncTS,_deltaIncTS,_alphaDecTS,_betaDecTS,_gammaDecTS,_deltaDecTS, _alphaIncPPS, _betaIncPPS,_gammaIncPPS,_deltaIncPPS, _alphaDecPPS, _betaDecPPS, _gammaDecPPS, _deltaDecPPS, _threshTS,_threshPPS, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed,_lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
	}
}
