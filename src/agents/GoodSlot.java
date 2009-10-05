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

public class GoodSlot extends RuleBasedAgent {

	protected HashMap<Query, Double> _reinvestment;
	protected HashMap<Query, Double> _revenue;
	protected BidBundle _bidBundle;

	protected boolean TARGET = true;
	protected boolean BUDGET = false;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		buildMaps(models);
		_bidBundle = new BidBundle();
		for (Query query : _querySpace) {
			double current = _reinvestment.get(query);

			if (_day > 1) {
				// handle the case of no impression (the agent got no slot)
				handleNoImpression(query, current);
				// handle the case when the agent got the promoted slots
				handlePromotedSlots(query);
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

			if (BUDGET)  _bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));

		}
		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();
		
		_reinvestment = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_reinvestment.put(query, 0.3);
		}

		_revenue = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			if (query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				_revenue.put(query, 10.0 + 5 / 3);
			}
			if (query.getType() == QueryType.FOCUS_LEVEL_ONE) {
				if (_manSpecialty.equals(query.getManufacturer()))
					_revenue.put(query, 15.0);
				else {
					if (query.getManufacturer() != null)
						_revenue.put(query, 10.0);
					else
						_revenue.put(query, 10.0 + 5 / 3);
				}
			}
			if (query.getType() == QueryType.FOCUS_LEVEL_TWO) {
				if (_manSpecialty.equals(query.getManufacturer()))
					_revenue.put(query, 15.0);
				else
					_revenue.put(query, 10.0);
			}
		}
	}

	protected double getTargetCPC(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		return conversion * _reinvestment.get(q) * _revenue.get(q);
	}

	protected void handleNoImpression(Query q, double currentReinvest) {
		if (!(_queryReport.getPosition(q) < 4)) {
			double newReinvest = Math.min(0.9, currentReinvest * 1.1);
			_reinvestment.put(q, newReinvest);
		}
	}

	protected void handlePromotedSlots(Query q) {
		if (_queryReport.getPosition(q) <= 3) {
			double newReinvest = Math.max(0.1, _reinvestment.get(q) * .9);
			_reinvestment.put(q, newReinvest);
		}
	}

	protected void walking(Query q, double currentReinvest) {

		/*
		 * if((_queryReport.getPosition(q) > 1) && _queryReport.getPosition(q) <
		 * 5){ Random random = new Random(); double currentBid = getQueryBid(q);
		 * double y = currentBid/currentReinvest; double distance =
		 * Math.abs(currentBid - _queryReport.getCPC(q)); double rfDistance =
		 * (currentReinvest - (distance/y))/10;
		 * 
		 * if(random.nextDouble() < 0.5){ if(currentReinvest + rfDistance >=
		 * 0.90) _reinvestment.put(q,0.90); else
		 * _reinvestment.put(q,currentReinvest + rfDistance); } else{
		 * 
		 * if(currentReinvest - rfDistance <= 0.1) _reinvestment.put(q,0.1);
		 * else _reinvestment.put(q, currentReinvest - rfDistance); } }
		 */
	}

	@Override
	public String toString() {
		return "GoodSlot";
	}

}
