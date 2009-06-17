package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * 
 * @author mbarrows
 *
 */

public class XFixedProfitAgent extends SimAbstractAgent {
	
	protected BidBundle _bidBundle;

	protected final double X = 0; //X is the amount of profit an agent will try to make on each query at minimum
	protected String _manufacturer;
	protected String _component;
	
	protected final double FO_BASELINE_PR_CONV = 0.1;
	protected final double F1_BASELINE_PR_CONV = 0.2;
	protected final double F2_BASELINE_PR_CONV = 0.3;
	
	protected HashMap<Query, Double> _values;
	
	protected int getFocusLevel(Query query){
		String manufacturer = query.getManufacturer();
		String component = query.getComponent();
		if (manufacturer.equals(null) && component.equals(null)){
			return 0;
		} else if (!manufacturer.equals(null) && !component.equals(null)){
			return 2;
		} else {
			return 1;
		}
	}
	
	protected double eta(double pi){
		return 1.5*pi/(0.5*pi+1);
	}
	
	protected double getValue(Query query){
		double value = 10;
		double conversionPr = 0;
		int focusLvl = getFocusLevel(query);
		String manufacturer = query.getManufacturer();
		String component = query.getComponent();
		if (manufacturer.equals(null)){
			value = (10*2 + 15)/3;
		} else if (manufacturer.equals(_manufacturer)){
			value = 15;
		}
		if (focusLvl == 0){
			conversionPr = (FO_BASELINE_PR_CONV*2 + eta(FO_BASELINE_PR_CONV))/3;
		} else if (focusLvl == 1){
			if (component.equals(null)){
				conversionPr = (F1_BASELINE_PR_CONV*2 + eta(F1_BASELINE_PR_CONV))/3;
			} else if (component.equals(_component)){
				conversionPr = eta(F1_BASELINE_PR_CONV);
			} else {
				conversionPr = F1_BASELINE_PR_CONV;
			}
		} else {
			if (component.equals(_component)){
				conversionPr = eta(F2_BASELINE_PR_CONV);
			} else {
				conversionPr = F2_BASELINE_PR_CONV;
			}
		}
		value = Math.max(value*conversionPr, 0);
		return value;
	}
	
	@Override
	public void initBidder() {
		_manufacturer = _advertiserInfo.getManufacturerSpecialty();
		_component = _advertiserInfo.getComponentSpecialty();
		
		// set values
		
		_values = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_values.put(query, getValue(query));
		}
		
		// initialize the bid bundle
		
		_bidBundle = new BidBundle();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		for (Query query : _querySpace) {
			// set bids
			double bid = _values.get(query) - X;
			_bidBundle.setBid(query, bid);
			// TODO: allow for sales in not all of the queries
			// TODO: target ads
			
			// set spend limit
			double dailySalesLimit = _advertiserInfo.getDistributionCapacity()/5/16;
			double dailyLimit = _bidBundle.getBid(query)*dailySalesLimit*1.1; // magic factor
			_bidBundle.setDailyLimit(query, dailyLimit);
		}
		return _bidBundle;
	}

	@Override
	public Set<AbstractModel> initModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}
}
