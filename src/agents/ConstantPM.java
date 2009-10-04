/**
 * Used to be BraddMaxx
 * Used to be Crest
 */
package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractConversionModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class ConstantPM extends RuleBasedAgent {
	private static final boolean SET_TARGET = true;
	private static final boolean SET_BUDGET = false;

	private Set<Product> _productSpace;
	private HashMap<Query, Double> _queryAvgProfit;
	private HashMap<Query, Set<Product>> _queryToProducts;

	private HashMap<Product, Double> _profit;

	private int _day;

	public ConstantPM() {
		_day = 0;
	}

	public HashMap<Query, Double> initHashMap(HashMap<Query, Double> map) {
		for(Query q : _querySpace) {
			map.put(q, (double)0);
		}

		return map;
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);

		System.out.println("\nBidding");
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
			
			bids.setBidAndAd(q, _CPCToBidModel.getPrediction(q, targetCPC), ad);
			
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
		return _queryAvgProfit.get(q) * 0.4 * conversion;
	}
	
	private void buildMaps(Set<AbstractModel> models) {
		for(AbstractModel model : models) {
			if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				_conversionPrModel = convPrModel;
			}
		}
	}

	@Override
	public void initBidder() {
		setDailyQueryCapacity();
		
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
		
		_capacity = _advertiserInfo.getDistributionCapacity();
		_queryAvgProfit = initHashMap(new HashMap<Query, Double>());

		_queryToProducts = new HashMap<Query, Set<Product>>();
		for(Query q : _querySpace) {
			HashSet<Product> s = new HashSet<Product>();
			_queryToProducts.put(q, s);
		}

		String spec = _advertiserInfo.getManufacturerSpecialty();

		_profit = new HashMap<Product, Double>();
		_productSpace = new HashSet<Product>();
		for(String m : _retailCatalog.getManufacturers()) {
			double mult = 1.0;
			if(m.equals(spec))
				mult += _advertiserInfo.getManufacturerBonus();

			for(String c : _retailCatalog.getComponents()) {
				Product p = new Product(m,c);
				_productSpace.add(p);

				_profit.put(p, mult * _retailCatalog.getSalesProfit(p));

				for(Query q : _querySpace) {
					Set<Product> s = _queryToProducts.get(q);
					if( (q.getComponent() == null || q.getComponent().equals(c)) && (q.getManufacturer() == null || q.getManufacturer().equals(m)))
						s.add(p);
				}
			}
		}
		
		for(Query q : _querySpace) {
			Set<Product> s = _queryToProducts.get(q);
			double profit = 0;

			for(Product p : s)
				profit += (_profit.get(p) / (double)s.size());
			_queryAvgProfit.put(q, profit);
			System.out.println(q + ": " + profit);
		}
	}
	
	@Override
	public String toString() {
		return "Constant PM";
	}
	
}