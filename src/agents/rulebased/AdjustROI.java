package agents.rulebased;

import java.util.HashMap;
import java.util.Set;
import models.AbstractModel;
import agents.AbstractAgent;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class AdjustROI extends RuleBasedAgent {
	
	protected BidBundle _bidBundle;
	protected final boolean BUDGET = false;
	protected final boolean DAILYBUDGET = false;
	protected double _initROI;
	protected double _alphaIncTS;
	protected double _betaIncTS;
	protected double _gammaIncTS;
	protected double _deltaIncTS;
	protected double _alphaDecTS;
	protected double _betaDecTS;
	protected double _gammaDecTS;
	protected double _deltaDecTS;
	protected double _alphaIncROI;
	protected double _betaIncROI;
	protected double _gammaIncROI;
	protected double _deltaIncROI;
	protected double _alphaDecROI;
	protected double _betaDecROI;
	protected double _gammaDecROI;
	protected double _deltaDecROI;
	protected double _threshTS;
	protected double _threshROI;
	protected HashMap<Query, Double> _ROI;

	public AdjustROI(double initROI,
			double alphaIncTS,
			double betaIncTS,
			double gammaIncTS,
			double deltaIncTS,
			double alphaDecTS,
			double betaDecTS,
			double gammaDecTS,
			double deltaDecTS,
			double alphaIncROI,
			double betaIncROI,
			double gammaIncROI,
			double deltaIncROI,
			double alphaDecROI,
			double betaDecROI,
			double gammaDecROI,
			double deltaDecROI,
			double threshTS,
			double threshROI,
			double lambdaCapLow,
			double lambdaCapMed,
			double lambdaCapHigh,
			double lambdaBudgetLow,
			double lambdaBudgetMed,
			double lambdaBudgetHigh,
			double dailyCapMin,
			double dailyCapMax) {
		_initROI = initROI;
		_alphaIncTS = alphaIncTS;
		_betaIncTS = betaIncTS;
		_gammaIncTS = gammaIncTS;
		_deltaIncTS = deltaIncTS;
		_alphaDecTS = alphaDecTS;
		_betaDecTS = betaDecTS;
		_gammaDecTS = gammaDecTS;
		_deltaDecTS = deltaDecTS;
		_alphaIncROI = alphaIncROI;
		_betaIncROI = betaIncROI;
		_gammaIncROI = gammaIncROI;
		_deltaIncROI = deltaIncROI;
		_alphaDecROI = alphaDecROI;
		_betaDecROI = betaDecROI;
		_gammaDecROI = gammaDecROI;
		_deltaDecROI = deltaDecROI;
		_threshTS = threshTS;
		_threshROI = threshROI;
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
		
		_ROI = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_ROI.put(q, _initROI);
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
		 * Calculate Average ROI
		 */
		double avgROI = 0.0;
		double totWeight = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0) {
				double weight = _salesDistribution.get(q);
				avgROI += ((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_queryReport.getCost(q)) * weight;
				totWeight+=weight;
			}
		}
		avgROI /= totWeight;
		if(Double.isNaN(avgROI)) {
			avgROI = _initROI;
		}

		/*
		 * Adjust Target Sales
		 */
		double totDesiredSales = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0) {
				double ROIq = (_salesReport.getRevenue(q) - _queryReport.getCost(q))/_queryReport.getCost(q);
				if(Math.abs(ROIq - avgROI) <= _threshROI) {
					//Do Nothing
				}
				else if (ROIq < avgROI) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1-(_alphaDecTS * Math.abs(ROIq - avgROI) +  _betaDecTS)* Math.pow(_gammaDecTS, _day*_deltaDecTS)));
				}
				else {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1+(_alphaIncTS *Math.abs(ROIq - avgROI)  +  _betaIncTS)* Math.pow(_gammaIncTS, _day*_deltaIncTS)));
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
		 * Adjust PM
		 */
		if(_day > 1) {
			adjustROI();
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
		double CPC = (rev * prConv)/(_ROI.get(q)+1);
		CPC = Math.max(0.0, Math.min(_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9, CPC));
		return CPC;
	}

	protected void adjustROI() {
		for(Query q : _querySpace) {
			double tmp = _ROI.get(q);
			double sales = _salesReport.getConversions(q);
			double dailyCap = _salesDistribution.get(q)*_dailyCapacity;
			if(Math.abs(sales - dailyCap) <= _threshTS) {
				//Do Nothing
			}
			else if (sales >= dailyCap) {
				tmp *= (1+(_alphaIncROI * Math.abs(sales - dailyCap) +  _betaIncROI) * Math.pow(_gammaIncROI, _day*_deltaIncROI));
			} else {
				tmp *= (1-(_alphaDecROI * Math.abs(sales - dailyCap) +  _betaDecROI) * Math.pow(_gammaDecROI, _day*_deltaDecROI));
			}
			if(Double.isNaN(tmp) || tmp <= 0.0) {
				tmp = _initROI;
			}
			_ROI.put(q, tmp);
		}
	}

	@Override
	public String toString() {
		return "AdjustROI";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustROI(_initROI,_alphaIncTS,_betaIncTS,_gammaIncTS,_deltaIncTS,_alphaDecTS,_betaDecTS,_gammaDecTS,_deltaDecTS, _alphaIncROI, _betaIncROI,_gammaIncROI,_deltaIncROI, _alphaDecROI, _betaDecROI, _gammaDecROI, _deltaDecROI, _threshTS,_threshROI, _lambdaCapLow, _lambdaCapMed, _lambdaCapHigh, _lambdaBudgetLow, _lambdaBudgetMed,_lambdaBudgetHigh, _dailyCapMin, _dailyCapMax);
	}
}
