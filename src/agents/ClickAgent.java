package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.targeting.BasicTargetModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class ClickAgent extends SimAbstractAgent {

	protected NewAbstractConversionModel _conversionPrModel;
	
	protected HashMap<Query, Double> _baselineConversion;
	protected HashMap<Query, Double> _revenue;
	protected HashMap<Query, Double> _PM;
	
	protected double _desiredSale;
	protected final double _lamda = 0.9;
	protected BidBundle _bidBundle;
	protected final int MAX_TIME_HORIZON = 5;
	protected final boolean TARGET = true;
	protected final boolean BUDGET = false;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		// build bid bundle
		for (Query query : _querySpace) {
			adjustPM(query);
			_bidBundle.setBid(query, getQueryBid(query));
			
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
			
			if (BUDGET) _bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}
		return _bidBundle;
	}

	@Override
	public void initBidder() {

		_desiredSale = _capacity * 1.5 / _capWindow / _querySpace.size();

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

		_bidBundle = new BidBundle();
		for (Query query : _querySpace) {
			_bidBundle.setBid(query, getQueryBid(query));
			//_bidBundle.setDailyLimit(query, setQuerySpendLimit(query));
		}

	}

	@Override
	public Set<AbstractModel> initModels() {
		_conversionPrModel = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if (_day > 1 && salesReport != null && queryReport != null) {

			int timeHorizon = (int) Math.min(Math.max(1, _day - 1),
					MAX_TIME_HORIZON);
			_conversionPrModel.setTimeHorizon(timeHorizon);
			_conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
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

	protected double setQuerySpendLimit(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		double dailySalesLimit = _desiredSale/conversion;
		return _bidBundle.getBid(q) * dailySalesLimit;
	}

	protected double getQueryBid(Query q) {
		double conversion;
		if (_day <= 6)
			conversion = _baselineConversion.get(q);
		else
			conversion = _conversionPrModel.getPrediction(q);
		return _revenue.get(q)*(1 - _PM.get(q))*conversion;
	}

}
