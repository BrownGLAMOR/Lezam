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

	protected ArrayList<BidBundle> _bidBundles;

	protected boolean TARGET = false;
	protected boolean BUDGET = true;

	private double goodslot = 3;
	private double badslot = 2;
	private Double decPM = .8;
	private Double incPM = 1.2;
	private double minPM = .4;
	private double maxPM = .8;
	private double initPM = .4;
	
	
	protected final static double LEARNING_RATE = .225;

	
	public PortfolioOpt() {
		_budgetModifier = 1.4;
	}

	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		buildMaps(models);

		if(_day < 2) { 
			_bidBundle = new BidBundle();
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad());
			}
			return _bidBundle;
		}

		_bidBundle = new BidBundle();

		adjustWantedSales();
		
		for (Query q : _querySpace) {
			adjustHonestFactor(q);
		}
		
		// build bid bundle
		for (Query query : _querySpace) {
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, targetCPC+.01);

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
		System.out.println(_bidBundle);
		_bidBundles.add(_bidBundle);

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();

		_honestFactor = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			_honestFactor.put(q, initPM);
		}

		double slice = _dailyCapacity / 20.0;
		_wantedSales = new HashMap<Query, Double>();
		for (Query q : _querySpace) {
			if (_manSpecialty.equals(q.getManufacturer()))
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

		return targetCPC * dailySalesLimit*_budgetModifier;
	}

	protected void adjustHonestFactor(Query q) {
		double newHonest;

		double tmp = _honestFactor.get(q);
		// if we does not get enough clicks (and bad position), then decrease PM
		// (increase bids, and hence slot)
		if (_salesReport.getConversions(q) >= _wantedSales.get(q) && _queryReport.getPosition(q) < goodslot) {
			tmp = _honestFactor.get(q) * incPM;
			tmp = Math.min(maxPM, tmp);
		} else if(_salesReport.getConversions(q) < _wantedSales.get(q) && _queryReport.getPosition(q) >= badslot) {
			// if we get too many clicks (and good position), increase
			// PM(decrease bids and hence slot)
			tmp = _honestFactor.get(q) * decPM;
			tmp = Math.max(minPM, tmp);
		}
		_honestFactor.put(q, tmp);

	}

	protected void adjustWantedSales() {
		HashMap<Query, Double> relatives = getRelatives(_queryReport, _salesReport);
		normalizeWeights(_wantedSales);
		HashMap<Query, Double> weights = EGUpdate(_wantedSales,relatives);
		normalizeWeights(weights);
		for(Query query : _querySpace) {
			_wantedSales.put(query, weights.get(query)*_dailyCapacity);
		}
		System.out.println("New Wanted Sales");
		for(Query query : _querySpace) {
			System.out.println(query + "  " + _wantedSales.get(query));
		}
	}
	
	protected void normalizeWeights(HashMap<Query, Double> weights) {
		double total = 0;
		for(Query query:weights.keySet())
			total += weights.get(query);
		
		for(Query query:weights.keySet()) {
			double value = weights.get(query)/total;
			weights.put(query, value);
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
		return "PortfolioOpt";
	}
	
	@Override
	public AbstractAgent getCopy() {
		return new PortfolioOpt();
	}

}
