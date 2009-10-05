/**
 * Used to be BraddMaxx
 * Used to be Crest
 */
package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class ConstantPM extends RuleBasedAgent {
	private static final boolean SET_TARGET = true;
	private static final boolean SET_BUDGET = false;

	private HashMap<Query, Double> _revenue;
	private HashMap<Query, Set<Product>> _queryToProducts;

	private int _day;

	public ConstantPM() {
		_day = 0;
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);

		if(_day < 2) { 
			BidBundle bundle = new BidBundle();
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				bundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return bundle;
		}
		
		
		BidBundle bids = new BidBundle();
		for(Query q : _querySpace) {
			System.out.print("\t" + q.toString() + ": ");
			Ad ad = null;
			if(q.getManufacturer() == null) {
				if(SET_TARGET)
					ad = new Ad(new Product(_manSpecialty, _compSpecialty));
				else
					ad = new Ad(null);
			} else
				ad = new Ad(null);

			System.out.print(ad.toString() + ", ");

			double targetCPC = getTargetCPC(q);
			double bid = _CPCToBidModel.getPrediction(q, targetCPC);
			if(Double.isNaN(bid)) {
				bid = targetCPC;
			}
			bids.setBid(q, _CPCToBidModel.getPrediction(q, targetCPC));
			
			if(SET_BUDGET)
				bids.setDailyLimit(q, getDailySpendingLimit(q, targetCPC));
		}

		return bids;
	}

	protected double getTargetCPC(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		return _revenue.get(q) * 0.4 * conversion;
	}
	
	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();
		
		_queryToProducts = new HashMap<Query, Set<Product>>();
		for(Query q : _querySpace) {
			HashSet<Product> s = new HashSet<Product>();
			_queryToProducts.put(q, s);
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
	
	@Override
	public String toString() {
		return "Constant PM";
	}
	
}