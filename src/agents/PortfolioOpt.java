/**
 * Used to be CHAgent
 */
package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class PortfolioOpt extends RuleBasedAgent {
	protected HashMap<Query, Double> _revenue;
	protected HashMap<Query, Double> _honestFactor;
	protected HashMap<Query, Double> _wantedSales;

	protected BidBundle _bidBundle;
	protected double _dailyCapacity;

	protected ArrayList<BidBundle> _bidBundles;

	protected final double _errorOfLimit = .1;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = false;

	protected final static double LEARNING_RATE = .075;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		buildMaps(models);

		if(_day < 2) { 
			_bidBundle = new BidBundle();
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}

		_bidBundle = new BidBundle();

		adjustWantedSales();

		double normalizeFactor = 0;
		for (Query query : _querySpace) {
			normalizeFactor += _wantedSales.get(query);
		}

		int unitsSold = 0;
		for (Query query : _querySpace) {
			unitsSold += _salesReport.getConversions(query);
		}

		int targetCapacity = (int)Math.max(2*_dailyCapacity - unitsSold, _dailyCapacity*.5);
		normalizeFactor = targetCapacity/normalizeFactor;
		for (Query query : _querySpace) {
			_wantedSales.put(query, _wantedSales.get(query)*normalizeFactor);
		}

		for (Query q : _querySpace) {
			adjustHonestFactor(q);
		}

		// build bid bundle
		for (Query query : _querySpace) {
			double targetCPC = getTargetCPC(query);
			double bid = _CPCToBidModel.getPrediction(query, targetCPC);
			if(Double.isNaN(bid)) {
				bid = targetCPC;
			}
			_bidBundle.setBid(query, _CPCToBidModel.getPrediction(query, targetCPC));

			// set target ads
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

			if (BUDGET) 
				_bidBundle.setDailyLimit(query, getDailySpendingLimit(query, targetCPC));			
		}

		_bidBundles.add(_bidBundle);

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		setDailyQueryCapacity();

		_honestFactor = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_honestFactor.put(q, .3);
		}

		double slice = _dailyCapacity / 20;
		_wantedSales = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			if (q.getManufacturer() == _manSpecialty)
				_wantedSales.put(q, 2 * slice);
			else
				_wantedSales.put(q, slice);

		}

		_revenue = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			if (query.getManufacturer() == _manSpecialty)
				_revenue.put(query, 15.0);
			else
				_revenue.put(query, 10.0);
		}

		_bidBundle = new BidBundle();

		_bidBundles = new ArrayList<BidBundle>();
	}

	protected double getTargetCPC(Query q) {
		double prConv;
		if (_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);
		return _revenue.get(q) * _honestFactor.get(q) * prConv;
	}

	@Override
	protected double getDailySpendingLimit(Query q, double targetCPC) {
		double prConv;
		if (_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);
		double dailySalesLimit = Math.max(_wantedSales.get(q)/prConv,1);

		return targetCPC * dailySalesLimit;
	}

	protected void adjustHonestFactor(Query q) {
		double newHonest;

		/* if we sold less than what we expected, and we got bad position
		 and also wanted sales does not tend to go over capacity, then raise
		 our bid*/
		if (_salesReport.getConversions(q) < _wantedSales.get(q)) {
			if (!(_queryReport.getPosition(q) <= 4)) {

				newHonest = Math.min(_honestFactor.get(q) * 1.1, .9);
				_honestFactor.put(q, newHonest);

			}
		} else {
			/* if we sold more than what expected, and we got good position,
			 then lower the bid*/
			if (_salesReport.getConversions(q) >=  _wantedSales.get(q)) {
				if (_queryReport.getPosition(q) <= 3) {
					/*newHonest = (_queryReport.getCPC(q) - 0.01)
							/ (_revenue.get(q) * conversion);*/
					newHonest = Math.min(_honestFactor.get(q)*0.9, .1);
					_honestFactor.put(q, newHonest);
				}
			}
		}

	}

	protected void adjustWantedSales() {

		HashMap<Query, Double> relatives = getRelatives(_queryReport, _salesReport);

		HashMap<Query, Double> weights = EGUpdate(_wantedSales,relatives);
		
		for(Query query : _querySpace) {
			_wantedSales.put(query, weights.get(query));
		}
	}

	private HashMap<Query, Double> getRelatives(QueryReport queryReport, SalesReport salesReport) {
		HashMap<Query,Double> newprices = new HashMap<Query,Double>();
		HashMap<Query,Double> relatives = new HashMap<Query, Double>();
		for(Query query:_querySpace) {
			// Zero case, not in the query at all
			// We don't want to not explore this at all, so for the time being
			// we will set it to 1.
			if( Math.abs( queryReport.getCost(query) ) <= .001 || 
					salesReport.getRevenue(query) <= .001) {
				newprices.put(query, 1.0);
			}
			// Otherwise we put the actual relative in there
			else {
				newprices.put(query,salesReport.getRevenue(query) / queryReport.getCost(query));
			}
			relatives.put(query, newprices.get(query));
			//			_oldprices.put(query,newprices.get(query));
			System.out.println("\n\n"+"*************"+"\n"+relatives.get(query));
		}
		return relatives;
	}

	protected HashMap<Query,Double> EGUpdate(HashMap<Query,Double> weights, HashMap<Query,Double> relatives) {
		// Update weights using the EG algorithm
		// First get the dot product
		double dotProduct = 0;
		for(Query query:_querySpace)  {
			dotProduct += weights.get(query)*relatives.get(query);
		}

		// Now get the denominator
		double totalWeights = 0;
		for(Query query:_querySpace) {
			totalWeights += weights.get(query)*Math.exp((LEARNING_RATE)*relatives.get(query)/dotProduct);
		}

		// Now update the weights
		for(Query query:_querySpace) {
			weights.put(query, (weights.get(query)*Math.exp((LEARNING_RATE)*relatives.get(query)/dotProduct))/totalWeights);
		}
		return weights;
	}


	@Override
	public String toString() {
		return "ClickSlot";
	}

}
