package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AdjustPR extends RuleBasedAgent {
	
	protected BidBundle _bidBundle;
	protected HashMap<Query, Double> _salesDistribution;
	protected final boolean TARGET = false;
	protected final boolean BUDGET = false;
	protected final boolean DAILYBUDGET = false;
	protected double _alphaIncTS;
	protected double _betaIncTS;
	protected double _alphaDecTS;
	protected double _betaDecTS;
	protected double _alphaIncPR;
	protected double _betaIncPR;
	protected double _alphaDecPR;
	protected double _betaDecPR;
	protected double _initPR;
	protected HashMap<Query, Double> _PR;

	public AdjustPR(double alphaIncTS, double betaIncTS, double alphaDecTS, double betaDecTS, double initPR,double alphaIncPR, double betaIncPR, double alphaDecPR, double betaDecPR) {
		_alphaIncTS = alphaIncTS;
		_betaIncTS = betaIncTS;
		_alphaDecTS = alphaDecTS;
		_betaDecTS = betaDecTS;
		_alphaIncPR = alphaIncPR;
		_betaIncPR = betaIncPR;
		_alphaDecPR = alphaDecPR;
		_betaDecPR = betaDecPR;
		_initPR = initPR;
	}
	
	@Override
	public void initBidder() {
		super.initBidder();
		
		_PR = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_PR.put(q, _initPR);
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
		 * Calculate Average PR
		 */
		double avgPR = 0.0;
		double totWeight = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0) {
				double weight = _salesDistribution.get(q);
				avgPR += (_salesReport.getRevenue(q)/_queryReport.getCost(q))*weight;
				totWeight+=weight;
			}
		}
		avgPR /= totWeight;
		if(Double.isNaN(avgPR)) {
			avgPR = _initPR;
		}

		/*
		 * Adjust Target Sales
		 */
		double totDesiredSales = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0) {
				if (_salesReport.getRevenue(q)/_queryReport.getCost(q) < avgPR) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1-(_alphaDecTS * Math.abs(_salesReport.getRevenue(q)/_queryReport.getCost(q) - avgPR) +  _betaDecTS)));
				}
				else {
					_salesDistribution.put(q, _salesDistribution.get(q)*(1+_alphaIncTS *Math.abs(_salesReport.getRevenue(q)/_queryReport.getCost(q) - avgPR)  +  _betaIncTS));
				}
			}
			else {
				if(_dailyCapacity != 0) {
					_salesDistribution.put(q, _salesDistribution.get(q)*(((1+_alphaIncTS * Math.abs(_salesReport.getRevenue(q)/_queryReport.getCost(q) - avgPR)  +  _betaIncTS)+1)/2.0));
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
		double CPC = (rev * prConv)/_PR.get(q);
		CPC = Math.max(0.0, Math.min(3.5, CPC));
		return CPC;
	}

	protected void adjustPR() {
		for(Query q : _querySpace) {
			double tmp = _PR.get(q);
			if (_salesReport.getConversions(q) >= _salesDistribution.get(q)*_dailyCapacity) {
				tmp *= (1+_alphaIncPR * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaIncPR);
			} else {
				tmp *= (1-(_alphaDecPR * Math.abs(_salesReport.getConversions(q) - _salesDistribution.get(q)*_dailyCapacity) +  _betaDecPR));
			}
			_PR.put(q, tmp);
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
		return "AdjustPR";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustPR(_alphaIncTS,_betaIncTS,_alphaDecTS,_betaDecTS,_initPR, _alphaIncPR, _betaIncPR, _alphaDecPR, _betaDecPR);
	}

}
