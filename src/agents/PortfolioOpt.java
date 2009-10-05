/**
 * Used to be CHAgent
 */
package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.prconv.AbstractConversionModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

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

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		buildMaps(models);
		_bidBundle = new BidBundle();

		if(_day > 1) {
			// adjust parameters
			for (Query q : _querySpace) {
				adjustWantedSales(q);
			}

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
		}

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

	protected void adjustWantedSales(Query q) {
		/* if we sold less than what we expected, but we got good position,
			 then lower our expectation*/
		if (_salesReport.getConversions(q) < _wantedSales.get(q)) {
			if (_queryReport.getPosition(q) <= 3) {
				_wantedSales.put(q, _wantedSales.get(q) * .8);
			}
		} else {
			/* if we sold more than what we expected, but we got bad
				 position, then increase our expectation*/
			if (!(_queryReport.getPosition(q) <= 4)) {
				_wantedSales.put(q, _wantedSales.get(q) * 1.25);
			}
		}
	}

	@Override
	public String toString() {
		return "ClickSlot";
	}

}
