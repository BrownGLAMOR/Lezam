package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractConversionModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class ConstantSales extends RuleBasedAgent {

	protected HashMap<Query, Double> _revenue;
	protected HashMap<Query, Double> _PM;

	protected double _desiredSale;
	protected BidBundle _bidBundle;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = false;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		// build bid bundle
		for (Query query : _querySpace) {
			if(_day > 1) {
				adjustPM(query);
			}
			
			double targetCPC = getTargetCPC(query);
			double bid = _CPCToBidModel.getPrediction(query, targetCPC);
			if(Double.isNaN(bid)) {
				bid = targetCPC;
			}
			_bidBundle.setBid(query, _CPCToBidModel.getPrediction(query, targetCPC));

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

			if (BUDGET || _day < 10) _bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));
		}
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		setDailyQueryCapacity();

		_desiredSale = _dailyQueryCapacity;

		_baselineConversion = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			if (q.getType() == QueryType.FOCUS_LEVEL_ZERO)
				_baselineConversion.put(q, 0.1);
			if (q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				if (q.getComponent() == _compSpecialty)
					_baselineConversion.put(q, 0.27);
				else
					_baselineConversion.put(q, 0.2);
			}
			if (q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				if (q.getComponent() == _compSpecialty)
					_baselineConversion.put(q, 0.39);
				else
					_baselineConversion.put(q, 0.3);
			}
		}

		_PM = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_PM.put(q, 0.7);
		}

		_revenue = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			if (query.getManufacturer() == _manSpecialty)
				_revenue.put(query, 15.0);
			else
				_revenue.put(query, 10.0);
		}
	}

	protected void adjustPM(Query q) {
		double tmp;
		// if we does not get enough clicks (and bad position), then decrease PM
		// (increase bids, and hence slot)
		if (_salesReport.getConversions(q) < _desiredSale) {
			tmp = (1 - _PM.get(q)) * 1.1;
			tmp = Math.min(.9, tmp);
		} else {
			// if we get too many clicks (and good position), increase
			// PM(decrease bids and hence slot)
			tmp = (1 - _PM.get(q)) * .9;
			tmp = Math.max(.1, tmp);
		}
		_PM.put(q, 1 - tmp);
	}

	protected double getTargetCPC(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		return _revenue.get(q)*(1 - _PM.get(q))*conversion;
	}

	@Override
	public String toString() {
		return "Click";
	}

}
