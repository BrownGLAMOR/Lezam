package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AdjustPPS extends Goldilocks {

	public AdjustPPS(double alphaIncTS, double betaIncTS, double alphaDecTS, double betaDecTS, double initPM,double alphaIncPM, double betaIncPM, double alphaDecPM, double betaDecPM, double budgetModifier) {
		super(alphaIncTS, betaIncTS, alphaDecTS, betaDecTS,initPM,alphaIncPM,betaIncPM,alphaDecPM,betaDecPM,budgetModifier);
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
		int numPPS = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0 &&
					_salesReport.getConversions(q) != 0) { //the last check is unnecessary, but safe
				avgPPS += (_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getConversions(q);
				numPPS++;
			}
		}
		avgPPS /= numPPS;
		if(Double.isNaN(avgPPS)) {
			avgPPS = 7.5;
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
		 * Adjust PM
		 */
		if(_day > 1) {
			adjustPM();
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

	protected double getTargetCPC(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		return _salesPrices.get(q)*(1 - _PM.get(q))*conversion;
	}

	@Override
	public String toString() {
		return "AdjustPPS";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustPPS(_alphaIncTS,_betaIncTS,_alphaDecTS,_betaDecTS,_initPM, _alphaIncPM, _betaIncPM, _alphaDecPM, _betaDecPM, _budgetModifier);
	}
}
