package models.revenue;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;

public class RevenueMovingAvg extends AbstractRevenueModel {
	protected final double _alpha = .75;
	protected Query query;
	protected double _revenue;
	protected double _latestSample;
	
	public RevenueMovingAvg(Query query, RetailCatalog retailCatalog) {
		if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
			String manufacturer = query.getManufacturer();
			String component = query.getComponent();
			Product product = new Product(manufacturer, component);
			this._revenue = retailCatalog.getSalesProfit(product);
		}
		else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			String tmp = query.getManufacturer();
			if (tmp == null) {
				String component = query.getComponent();
				double avgRevenue = 0;
				for (String manufacturer : retailCatalog.getManufacturers()) {
					Product product = new Product(manufacturer, component);
					avgRevenue +=  retailCatalog.getSalesProfit(product);
				}
				avgRevenue /= retailCatalog.getManufacturers().size();
				this._revenue = avgRevenue;
			}
			else {
				String manufacturer = query.getManufacturer();
				double avgRevenue = 0;
				for (String  component: retailCatalog.getComponents()) {
					Product product = new Product(manufacturer, component);
					avgRevenue +=  retailCatalog.getSalesProfit(product);
				}
				avgRevenue /= retailCatalog.getComponents().size();
				this._revenue = avgRevenue;
			}
		}
		else {
			double avgRevenue = 0;
			for (String manufacturer : retailCatalog.getManufacturers()) {
				for (String  component: retailCatalog.getComponents()) {
					Product product = new Product(manufacturer, component);
					avgRevenue +=  retailCatalog.getSalesProfit(product);
				}
			}
			avgRevenue /= retailCatalog.getManufacturers().size()*retailCatalog.getComponents().size();
			this._revenue = avgRevenue;
		}
	}

	public void update(double newSample) {
		_latestSample = newSample;
		_revenue = _alpha * _latestSample + (1 - _alpha) * _revenue;
	}

	public Query getQuery() {
		return query;
	}
	
	public double getRevenue() {
		return _revenue;
	}

	@Override
	public void update(SalesReport salesReport, QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractModel getCopy() {
		return null;
	}

}