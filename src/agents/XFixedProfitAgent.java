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

	protected double X = 1.75; //X is the amount of profit an agent will try to make on each query at minimum
	protected String _manufacturer;
	protected String _component;
	
	protected final double FO_BASELINE_PR_CONV = 0.1;
	protected final double F1_BASELINE_PR_CONV = 0.2;
	protected final double F2_BASELINE_PR_CONV = 0.3;
	
	protected double magic_factor = 1/.3;
	protected double IS_factor = .9;
	
	protected HashMap<Query, Double> _values;
	
	protected double eta(double pi){
		return 1.5*pi/(0.5*pi+1);
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
		value = revenue*conversionPr*IS_factor;
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
		
		//for debugging - allows for reloading agent
		_manufacturer = _advertiserInfo.getManufacturerSpecialty();
		_component = _advertiserInfo.getComponentSpecialty();
		
		return buildBidBundle();
	}

	public BidBundle buildBidBundle(){
		
		int sum = 0;
		
		for (Query query : _querySpace) {
			System.out.println(query.getManufacturer() + "," + query.getComponent());
			System.out.println(_salesReport.getConversions(query));
			sum += _salesReport.getConversions(query);
		}
		
		int oversold = sum -_advertiserInfo.getDistributionCapacity()/5;
		
		System.out.println(sum + " / " + _advertiserInfo.getDistributionCapacity()/5 + " units sold");
		System.out.println("magic factor: " + magic_factor);
		System.out.println("X: " + X);
		
		/*
		// underselling
		if (_advertiserInfo.getDistributionCapacity()/5 > 2*sum){
			X -= 0.3;
		} else if (_advertiserInfo.getDistributionCapacity()/5 > sum){
			X -= 0.1;
		} 
		// overselling
		else if (1.25*_advertiserInfo.getDistributionCapacity()/5 < sum){
			X += 0.2;
		} else {
			X += 0.1;
		}
		*/
		
		double delta = ((double)sum/(_advertiserInfo.getDistributionCapacity()/5)-1)/4;
		System.out.println("delta = " + delta);
		X += delta;
		
		_bidBundle = new BidBundle();
		
		int numberOfZeroQueries = 0;
		
		
		for (Query query : _querySpace) {
			System.out.println(query.getManufacturer() + "," + query.getComponent());
			System.out.println(_salesReport.getConversions(query));
			sum += _salesReport.getConversions(query);
			// set bids
			double bid = getValue(query)-X;
			if (bid < 0){
				numberOfZeroQueries++;
				bid = 0;
			}
			_bidBundle.addQuery(query, bid, null);
			//_bidBundle.setBid(query, 1.0);
			// TODO: allow for sales in not all of the queries
			// TODO: target ads
			
		}
		
		
		
		
		
		for (Query query : _querySpace) {
			// set spend limit
			double dailySalesLimit = (_advertiserInfo.getDistributionCapacity()-oversold)/5/(16-numberOfZeroQueries);
			double dailyLimit = _bidBundle.getBid(query)*(dailySalesLimit+1)*magic_factor; // magic factor
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
