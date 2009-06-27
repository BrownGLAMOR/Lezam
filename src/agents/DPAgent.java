package agents;

import java.util.HashMap;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottobid.SimpleSlotToBid;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.slottonumclicks.SimpleClick;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class DPAgent extends SimAbstractAgent{
	
	protected AbstractUnitsSoldModel unitsSoldModel; 
	protected HashMap<Query, AbstractRevenueModel> revenueModels;
	protected HashMap<Query, AbstractPrConversionModel> prConversionModels;
	protected HashMap<Query, AbstractSlotToBidModel> slotToBidModels;
	protected HashMap<Query, AbstractSlotToNumClicks> slotToClicksModels;
	
	protected BidBundle bidBundle;
	protected BidBundle oldBidBundle;
	
	protected int distributionCapacity;
	protected int distributionWindow;
	protected int dailyCapacity;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		oldBidBundle = bidBundle;
		
		if (_day == 0) {
			//TODO
			return bidBundle;
		}
		
		// find relevant variables
		
		int unitsSold = 0;
		for (Query query : _querySpace) {
			unitsSold += _salesReport.getConversions(query);
		}
		
		int targetCapacity = (int)Math.min(2*dailyCapacity - unitsSold, dailyCapacity*.5);
		
		HashMap<Query, HashMap<Double, Integer>> item = new HashMap<Query, HashMap<Double, Integer>>();
		for (Query query : _querySpace) {
			HashMap<Double, Integer> bidToClicks = new HashMap<Double, Integer>();
			
			double currentPosition = _queryReport.getPosition(query);
			if (currentPosition != Double.NaN) {
				int maxClicks = slotToClicksModels.get(query).getPrediction(currentPosition);
				
			}
			else {
				
			}
			
		}
		
		
		// build bid bundle
			
		return null;
	}

	@Override
	public void initBidder() {
		
		// set constants
		
		distributionCapacity = _advertiserInfo.getDistributionCapacity();
		distributionWindow = _advertiserInfo.getDistributionWindow();
		dailyCapacity = distributionCapacity / distributionWindow;
		
		// initialize strategy related variables
		
		bidBundle = null;
		oldBidBundle = null;
	}

	@Override
	public Set<AbstractModel> initModels() {
		
		unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, distributionCapacity, distributionWindow);
		
		revenueModels = new HashMap<Query, AbstractRevenueModel>(); 
		for (Query query : _querySpace) {
			revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}
		
		prConversionModels = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			prConversionModels.put(query, new SimplePrConversion(query, _advertiserInfo, unitsSoldModel));	
		}
		
		slotToBidModels = new HashMap<Query, AbstractSlotToBidModel>();
		for (Query query : _querySpace) {
			slotToBidModels.put(query, new SimpleSlotToBid(query, _slotInfo));
		}
		
		slotToClicksModels = new HashMap<Query, AbstractSlotToNumClicks>();
		for (Query query : _querySpace) {
			slotToClicksModels.put(query, new SimpleClick(query, _advertiserInfo, _slotInfo));
		}
		
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if (_day == 0) return;
		
		unitsSoldModel.update(salesReport);
		
		for (Query query : _querySpace) {
			revenueModels.get(query).update(salesReport, queryReport);
		}
		
		for (Query query : _querySpace) {
			prConversionModels.get(query).updateModel(queryReport, salesReport);
		}
		
		for (Query query : _querySpace) {
			slotToBidModels.get(query).updateModel(queryReport, salesReport, oldBidBundle);
		}
		
	}

}
