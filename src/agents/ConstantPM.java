/**
 * Used to be BraddMaxx
 * Used to be Crest
 */
package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.AbstractConversionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import agents.AbstractAgent;
import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class ConstantPM extends AbstractAgent {
	private static final boolean SET_TARGET = true;
	private static final boolean SET_BUDGET = false;
	
	private static final double BUDGET_CAPACITY = 1.5 * 0.2;
	
	private static final int MAX_TIME_HORIZON = 5;

	private Set<Product> _productSpace;
	private HashMap<Query, Double> _queryAvgProfit;
	private HashMap<Query, Set<Product>> _queryToProducts;

	private HashMap<Product, Double> _profit;

	private int _timeHorizon;
	
	private AbstractConversionModel _model;

	private int _day;
	private int _capacity;

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
		
		double avgBid = 0;
		double avgConvRate = 0;

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
			double pr = 0.1;

			double myBid = (_queryAvgProfit.get(q) * 0.4) * pr;

			avgBid += (myBid / (double)_querySpace.size());
			avgConvRate += (pr / (double)_querySpace.size());

			double budget =
				((double)_capacity * BUDGET_CAPACITY * avgBid) / avgConvRate;
			
			//System.out.println(myBid);
			bids.setBidAndAd(q, myBid, ad);
			if(SET_BUDGET)
				bids.setCampaignDailySpendLimit(budget);
		}
		//System.out.println("Limit: " + ((((double)_capacity / 4.0) * avgBid) / avgConvRate));
		//bids.setCampaignDailySpendLimit((((double)_capacity / 4.0) * avgBid) / avgConvRate);

		return bids;
	}

	private void buildMaps(Set<AbstractModel> models) {
		for(AbstractModel model : models) {
			if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				_model = convPrModel;
			}
		}
	}

	@Override
	public void initBidder() {
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
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> m = new HashSet<AbstractModel>();

		_model = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		((HistoricPrConversionModel) _model).setTimeHorizon(3);
		m.add(_model);

		return m;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if( (salesReport != null) && (queryReport != null) ) {
			_day++;
			_timeHorizon = Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);

			((HistoricPrConversionModel) _model).setTimeHorizon(_timeHorizon);
			_model.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			
		}
	}
	
	@Override
	public String toString() {
		return "Constant PM";
	}
	
}