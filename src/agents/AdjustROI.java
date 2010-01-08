package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AdjustROI extends RuleBasedAgent {
	
	protected BidBundle _bidBundle;
	protected HashMap<Query, Double> _salesDistribution;
	protected final boolean TARGET = false;
	protected final boolean BUDGET = false;
	protected final boolean DAILYBUDGET = false;
	protected double _alphaIncTS;
	protected double _betaIncTS;
	protected double _alphaDecTS;
	protected double _betaDecTS;
	protected double _alphaIncROI;
	protected double _betaIncROI;
	protected double _alphaDecROI;
	protected double _betaDecROI;
	protected double _initROI;
	protected HashMap<Query, Double> _ROI;

	public AdjustROI(double alphaIncTS, double betaIncTS, double alphaDecTS, double betaDecTS, double initPR,double alphaIncPR, double betaIncPR, double alphaDecPR, double betaDecPR) {
		_alphaIncTS = alphaIncTS;
		_betaIncTS = betaIncTS;
		_alphaDecTS = alphaDecTS;
		_betaDecTS = betaDecTS;
		_alphaIncROI = alphaIncPR;
		_betaIncROI = betaIncPR;
		_alphaDecROI = alphaDecPR;
		_betaDecROI = betaDecPR;
		_initROI = initPR;
	}
	
	@Override
	public void initBidder() {
		super.initBidder();
		
		_ROI = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_ROI.put(q, _initROI);
		}
		
		_salesDistribution = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_salesDistribution.put(q, 1.0/_querySpace.size());
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
				if (_salesReport.getRevenue(q)/_queryReport.getCost(q) < avgROI) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1-(_alphaDecTS * Math.abs(_salesReport.getRevenue(q)/_queryReport.getCost(q) - avgROI) +  _betaDecTS)));
				}
				else {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1+_alphaIncTS *Math.abs(_salesReport.getRevenue(q)/_queryReport.getCost(q) - avgROI)  +  _betaIncTS));
				}
			}
			else {
				if(_dailyCapacity != 0) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(((1+_alphaIncTS * Math.abs(_salesReport.getRevenue(q)/_queryReport.getCost(q) - avgROI)  +  _betaIncTS)+1)/2.0));
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
			adjustPR();
		}

		for (Query query : _querySpace) {
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, targetCPC+.01);

			if (TARGET) {
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getComponent() == null)
					_bidBundle.setAd(query, new Ad(new Product(query.getManufacturer(), _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getManufacturer() == null)
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO) && query.getManufacturer().equals(_manSpecialty)) 
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
			}

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
		if(_day <= 6)
			prConv = _baselineConversion.get(q);
		else
			prConv = _conversionPrModel.getPrediction(q);
		double rev = _salesPrices.get(q);
		double CPC = (rev * prConv)/(_ROI.get(q)+1);
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		return CPC;
	}

	protected void adjustPR() {
		for(Query q : _querySpace) {
			double tmp = _ROI.get(q);
			if (_salesReport.getConversions(q) >= _salesDistribution.get(q)*_dailyCapacity) {
				tmp *= (1+_alphaIncROI * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaIncROI);
			} else {
				tmp *= (1-(_alphaDecROI * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaDecROI));
			}
			_ROI.put(q, tmp);
		}
	}
	
	@Override
	protected double getDailySpendingLimit(Query q, double targetCPC) {
		if(_day >= 6 && _conversionPrModel != null) {
			return (targetCPC * _salesDistribution.get(q)*_dailyCapacity) / _conversionPrModel.getPrediction(q);
		}
		else {
			return (targetCPC * _salesDistribution.get(q)*_dailyCapacity) / _baselineConversion.get(q);
		}
	}

	@Override
	public String toString() {
		return "AdjustROI";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustROI(_alphaIncTS,_betaIncTS,_alphaDecTS,_betaDecTS,_initROI, _alphaIncROI, _betaIncROI, _alphaDecROI, _betaDecROI);
	}

}
