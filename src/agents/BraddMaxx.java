package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.NewAbstractConversionModel;
import agents.SimAbstractAgent;
import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class BraddMaxx extends SimAbstractAgent {
	private static final int MAX_TIME_HORIZON = 5;

	private Set<Product> _productSpace;
	private HashMap<Query, Double> _queryAvgProfit;
	private HashMap<Query, Set<Product>> _queryToProducts;

	private HashMap<Product, Double> _profit;

	private int _timeHorizon;
	
	private NewAbstractConversionModel _model;
	private GoodConversionPrModel _oldModel;

	private int _day;
	private int _capacity;

	public BraddMaxx() {
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
		HistoricPrConversionModel convModel = null;
		for(AbstractModel m : models)
			if(m instanceof HistoricPrConversionModel)
				convModel = (HistoricPrConversionModel)m;

		double avgBid = 0;
		double avgConvRate = 0;

		System.out.println("\nBidding");
		BidBundle bids = new BidBundle();
		for(Query q : _querySpace) {
			System.out.print("\t" + q.toString() + ": ");
			Ad ad = null;
			if(q.getType() == QueryType.FOCUS_LEVEL_TWO)
				//				ad = new Ad(new Product(q.getManufacturer(), q.getComponent()));
				ad = new Ad(null);
			else
				ad = new Ad(null);

			System.out.print(ad.toString() + ", ");
			double pr = 0.1;
			if(_day > 2) {
				pr = convModel.getPrediction(q);
				double oldPr = _oldModel.getPrediction(q);
				
				System.out.println("Old prediction: " + oldPr +"\tNew Prediction: " + pr + "\tDelta: " + (oldPr - pr));
			}

			double myBid = (_queryAvgProfit.get(q) * 0.4) * pr;

			avgBid += (myBid / (double)_querySpace.size());
			avgConvRate += (pr / (double)_querySpace.size());

			//System.out.println(myBid);
			bids.setBidAndAd(q, myBid, ad);
		}
		//System.out.println("Limit: " + ((((double)_capacity / 4.0) * avgBid) / avgConvRate));
		//bids.setCampaignDailySpendLimit((((double)_capacity / 4.0) * avgBid) / avgConvRate);

		return bids;
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

		_model = new HistoricPrConversionModel(_querySpace);
		_model.setTimeHorizon(3);
		m.add(_model);

		_oldModel = new GoodConversionPrModel(_querySpace);

		return m;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if( (salesReport != null) && (queryReport != null) ) {
			_day++;
			
			_model.updateModel(queryReport, salesReport);
			
			_timeHorizon = Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			_oldModel.setTimeHorizon(_timeHorizon);
			_oldModel.updateModel(queryReport, salesReport);
		}
	}
}