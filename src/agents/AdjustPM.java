package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class AdjustPM extends Goldilocks {

	public AdjustPM(double incTS, double decTS, double initPM,double decPM, double incPM, double minPM, double maxPM, double budgetModifier) {
		super(incTS,decTS,initPM,decPM,incPM,minPM,maxPM,budgetModifier);
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
		 * Calculate Average PM
		 */
		double avgPM = 0.0;
		int numPM = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0) {
				avgPM += (_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getRevenue(q);
				numPM++;
			}
		}
		avgPM /= numPM;

		/*
		 * Adjust Target Sales
		 */
		double totDesiredSales = 0;
		for(Query q : _querySpace) {
			if(_queryReport.getCost(q) != 0 &&
					_salesReport.getRevenue(q) !=0) {
				if ((_salesReport.getRevenue(q) - _queryReport.getCost(q))/_salesReport.getRevenue(q) < avgPM) {
					_desiredSales.put(q, _desiredSales.get(q)*_decTS);
				}
				else {
					_desiredSales.put(q, _desiredSales.get(q)*_incTS);
				}
			}
			else {
				_desiredSales.put(q, _desiredSales.get(q)*((_incTS+1)/2.0));
			}
			totDesiredSales += _desiredSales.get(q);
		}

		/*
		 * Normalize
		 */
		double normFactor = _dailyCapacity/totDesiredSales;
		for(Query q : _querySpace) {
			_desiredSales.put(q, _desiredSales.get(q)*normFactor);
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
		return "AdjustPM";
	}

	@Override
	public AbstractAgent getCopy() {
		return new AdjustPM(_incTS,_decTS,_initPM, _decPM, _incPM, _minPM, _maxPM, _budgetModifier);
	}

}
