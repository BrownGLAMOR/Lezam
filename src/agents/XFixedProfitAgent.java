package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
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
	
	protected double eta(double pi){
		double num = 1.5*pi;
		double den = (0.5*pi+1);
		return num/den;
	}
	
	protected double getValue(Query query){
		double value = 0;
		double revenue = 10;
		double conversionPr = 0;
		String manufacturer = query.getManufacturer();
		String component = query.getComponent();
		
		if (manufacturer == null){
			revenue = 11.666666;
		} else if (manufacturer.equals(_manufacturer)){
			revenue = 15;
		}
		
		if (query.getType() == QueryType.FOCUS_LEVEL_ZERO){
			conversionPr = (FO_BASELINE_PR_CONV*2 + eta(FO_BASELINE_PR_CONV));
			conversionPr /= 3;
		} else if (query.getType() == QueryType.FOCUS_LEVEL_ONE){
			if (component == null){
				conversionPr = (F1_BASELINE_PR_CONV*2 + eta(F1_BASELINE_PR_CONV));
				conversionPr /= 3;
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
		value = revenue*conversionPr;
		//value = Math.max(revenue*conversionPr, 0);
		return value;
	}
	
	@Override
	public void initBidder() {
		_manufacturer = _advertiserInfo.getManufacturerSpecialty();
		_component = _advertiserInfo.getComponentSpecialty();
		/*
		// set values
		
		_values = new HashMap<Query, Double>();
		for (Query query : _querySpace) {
			_values.put(query, getValue(query));
		}
		*/
		// initialize the bid bundle
		
		buildBidBundle();
		//_bidBundle = new BidBundle();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		//for debugging
		_manufacturer = _advertiserInfo.getManufacturerSpecialty();
		_component = _advertiserInfo.getComponentSpecialty();
		
		return buildBidBundle();
	}

	public BidBundle buildBidBundle(){
		
		_bidBundle = new BidBundle();
		
		for (Query query : _querySpace) {
			// set bids
			//double bid = _values.get(query);
			_bidBundle.addQuery(query, getValue(query), null);
			//_bidBundle.setBid(query, 1.0);
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
