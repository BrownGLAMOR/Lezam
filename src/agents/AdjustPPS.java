package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AdjustPPS extends RuleBasedAgent {
	
	protected BidBundle _bidBundle;
	protected HashMap<Query, Double> _salesDistribution;
	protected final boolean TARGET = false;
	protected final boolean BUDGET = false;
	protected final boolean DAILYBUDGET = true;
	protected double _alphaIncTS;
	protected double _betaIncTS;
	protected double _alphaDecTS;
	protected double _betaDecTS;
	protected double _alphaIncPPS;
	protected double _betaIncPPS;
	protected double _alphaDecPPS;
	protected double _betaDecPPS;
	protected double _initPPS;
	protected HashMap<Query, Double> _PPS;

	public AdjustPPS(double alphaIncTS, double betaIncTS, double alphaDecTS, double betaDecTS, double initPPS,double alphaIncPPS, double betaIncPPS, double alphaDecPPS, double betaDecPPS) {
		_alphaIncTS = alphaIncTS;
		_betaIncTS = betaIncTS;
		_alphaDecTS = alphaDecTS;
		_betaDecTS = betaDecTS;
		_alphaIncPPS = alphaIncPPS;
		_betaIncPPS = betaIncPPS;
		_alphaDecPPS = alphaDecPPS;
		_betaDecPPS = betaDecPPS;
		_initPPS = initPPS;
	}
	
	@Override
	public void initBidder() {
		super.initBidder();
		
		_PPS = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_PPS.put(q, _initPPS);
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
				if ((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q) < avgPPS) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1-(_alphaDecTS * Math.abs((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q) - avgPPS) +  _betaDecTS)));
				}
				else {
					if(_dailyCapacity != 0) {
						_salesDistribution.put(q, _salesDistribution.get(q)*(1+_alphaIncTS *Math.abs((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q) - avgPPS)  +  _betaIncTS));
					}
				}
			}
			else {
				_salesDistribution.put(q, _salesDistribution.get(q)*(((1+_alphaIncTS * Math.abs((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q) - avgPPS)  +  _betaIncTS)+1)/2.0));
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
		double CPC = (rev - _PPS.get(q))* prConv;
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		return CPC;
	}

	protected void adjustPPS() {
		for(Query q : _querySpace) {
			double tmp = _PPS.get(q);
			if (_salesReport.getConversions(q) >= _salesDistribution.get(q)*_dailyCapacity) {
				tmp *= (1+_alphaIncPPS * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaIncPPS);
			} else {
				tmp *= (1-(_alphaDecPPS * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaDecPPS));
			}
			_PPS.put(q, tmp);
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
		return "AdjustPPS";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustPPS(_alphaIncTS,_betaIncTS,_alphaDecTS,_betaDecTS,_initPPS, _alphaIncPPS, _betaIncPPS, _alphaDecPPS, _betaDecPPS);
	}
}
