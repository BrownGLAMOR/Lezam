package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.revenue.AbstractRevenueModel;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottobid.SimpleSlotToBid;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.slottonumclicks.SimpleClick;
import newmodels.slottonumclicks.StaticSlotToClicks;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class DPAgent extends AbstractAgent{
	
	// models
	protected AbstractUnitsSoldModel unitsSoldModel; 
	protected HashMap<Query, AbstractRevenueModel> revenueModels;
	protected HashMap<Query, AbstractPrConversionModel> prConversionModels;
	protected HashMap<Query, SimpleSlotToBid> slotToBidModels;
	protected HashMap<Query, AbstractSlotToNumClicks> slotToClicksModels;
	private AbstractBidToCPC bidToCPC;
	private AbstractQueryToNumImp queryToNumImpModel;
	private AbstractBidToPrClick bidToPrClick;
	private AbstractUserModel userModel;
	
	// model, strategy related variables
	protected HashMap<Query, Integer> targetPosition;
	protected HashMap<Query, Integer> oldTargetPosition;
	
	protected BidBundle bidBundle;
	protected BidBundle oldBidBundle;
	
	// constants
	protected int distributionCapacity;
	protected int distributionWindow;
	protected int dailyCapacity;
	
    protected int promotedSlots;
    protected int regularSlots;
    protected int slots;
    
    
 // for debug
	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		
		if (_day > 3) this.printInfo();
		
		// update variables
		
		oldBidBundle = bidBundle;
		oldTargetPosition = targetPosition;
		
		// handle first two days
		
		if (_day <= 1) {
			bidBundle = new BidBundle();
			for (Query query : _querySpace) {
				double bid = .4*revenueModels.get(query).getRevenue()*prConversionModels.get(query).getPrediction(0);
				double dailyLimit = .8*bid*(dailyCapacity/_querySpace.size())/prConversionModels.get(query).getPrediction(0);
				bidBundle.setBid(query, bid);
				bidBundle.setDailyLimit(query, dailyLimit);
			}
			
			
			
			return bidBundle;
		}
		
		// find relevant variables
		
		int unitsSold = 0;
		for (Query query : _querySpace) {
			unitsSold += _salesReport.getConversions(query);
		}
		
		int targetCapacity = (int)Math.max(2*dailyCapacity - unitsSold, dailyCapacity*.5);

		double overcap = Math.max(0, unitsSoldModel.getWindowSold() - distributionCapacity);
		
		HashMap<Query, HashMap<Double, Integer>> item = new HashMap<Query, HashMap<Double, Integer>>();
		HashMap<Query, HashMap<Double, Integer>> bidToSlotSet = new HashMap<Query, HashMap<Double, Integer>>();
		
		for (Query query : _querySpace) {
			HashMap<Double, Integer> bidToClicks = new HashMap<Double, Integer>();
			HashMap<Double, Integer> bidToSlot = new HashMap<Double, Integer>();
			
			Double currentPosition = _queryReport.getPosition(query);
			if (!Double.isNaN(currentPosition)) {
				int position = (int)Math.ceil(currentPosition);
				addBidToClicks(query, position,bidToClicks);
				addBidToSlot(query, position, bidToSlot);
				
				if (currentPosition >= 2) {
					addBidToClicks(query, position - 1, bidToClicks);
					addBidToSlot(query, position - 1, bidToSlot);
				}
				
				if (currentPosition <= slots - 1) {
					addBidToClicks(query, position + 1, bidToClicks);
					addBidToSlot(query, position + 1, bidToClicks);
				}

			}
			else {
				addBidToClicks(query, slots, bidToClicks);
				addBidToSlot(query, slots, bidToSlot);
			}
			
			item.put(query, bidToClicks);
			bidToSlotSet.put(query, bidToSlot);
		}
		
	    // mckp
		
		System.out.println("targetCapacity : "+targetCapacity);
		double[][] profit = new double[_querySpace.size()][];
		Query[] queries = new Query[_querySpace.size()];
		double[][] bids = new double[_querySpace.size()][];
		int[][] sales = new int[_querySpace.size()][];
		
		int i = 0;
		for (Query query : _querySpace) {
			queries[i] = query;
			profit[i] = new double[targetCapacity];
			for (int j = 0; j < targetCapacity; j++)
				profit[i][j] = 0;
			bids[i] = new double[targetCapacity];
			sales[i] = new int[targetCapacity];
			
			for (int j = 0; j < targetCapacity; j++) 
				for (double bid : item.get(query).keySet()) {

					int maxClick = item.get(query).get(bid);
					double prConv = prConversionModels.get(query).getPrediction(overcap);
					
					for (int capacity = 0; capacity/prConv <= maxClick; capacity++) {
						if (capacity > j*prConv) break;
						double tmp = 0;
						if (i > 0) tmp = profit[i - 1][j - capacity];
						tmp += capacity * (revenueModels.get(query).getRevenue() - bidToCPC.getPrediction(query, bid, oldBidBundle)/prConversionModels.get(query).getPrediction(overcap));

						if (tmp >= profit[i][j]) {
							profit[i][j] = tmp;
							bids[i][j] = bid;
							sales[i][j] = capacity;
						}
					}
				}
			i++;
		}
		
		// build bid bundle
		
		double maxProfit = 0;
		int capacity = 0;
		for (int j = 0; j <  targetCapacity; j++)
			if (profit[_querySpace.size()-1][j] > maxProfit) {
				maxProfit = profit[_querySpace.size()-1][j];
				capacity = j;
			}
		
		bidBundle = new BidBundle();
		targetPosition = new HashMap<Query, Integer>();
	
		output.printf("********************\n");
		output.printf("Max profit : %f.\n", maxProfit);
		output.printf("Capacity is : %d.\n", capacity);
		output.printf("Target Capacity : %d. \n", targetCapacity);
		output.printf("********************\n");
		
		while (i > 0) {
			i--;
			double bid = bids[i][capacity];
			if (bid == 0) bid = slotToBidModels.get(queries[i]).getPrediction(slots);
			double clicks = sales[i][capacity]*1.0/prConversionModels.get(queries[i]).getPrediction(overcap);
			//TODO here we are using bid to approx. cpc
			double dailyLimit = Math.max(bid * clicks, bid*2);
			
			bidBundle.setBid(queries[i], bid);
			bidBundle.setDailyLimit(queries[i], dailyLimit);
			
			targetPosition.put(queries[i], bidToSlotSet.get(queries[i]).get(bid));
			
			output.println(queries[i]+" "+bid+" "+sales[i][capacity]+" "+capacity);
			
			capacity -= sales[i][capacity];
		}
		
		System.out.printf("********************\n");
		
		
			
		return bidBundle;
	}

	protected void addBidToClicks(Query query, int position, HashMap<Double, Integer> bidToClicks) {
		double bid = slotToBidModels.get(query).getPrediction(position);
		//int maxClicks = (int) (queryToNumImpModel.getPrediction(query) * bidToPrClick.getPrediction(query, bid, null, oldBidBundle));
		int maxClicks = slotToClicksModels.get(query).getPrediction(position);
		bidToClicks.put(bid, maxClicks);
	}
	
	protected void addBidToSlot(Query query, int position, HashMap<Double, Integer> bidToSlot) {
		double bid = slotToBidModels.get(query).getPrediction(position);
		bidToSlot.put(bid, position);
	}
	
	@Override
	public void initBidder() {
		
		// set constants
		
		distributionCapacity = _advertiserInfo.getDistributionCapacity();
		distributionWindow = _advertiserInfo.getDistributionWindow();
		dailyCapacity = (int) (distributionCapacity*1.3 / distributionWindow);
		
		promotedSlots = _slotInfo.getPromotedSlots();
		regularSlots = _slotInfo.getRegularSlots();
		slots = promotedSlots + regularSlots;
		
		// initialize strategy related variables
		
		bidBundle = null;
		oldBidBundle = null;
		
		targetPosition = null;
		oldTargetPosition = null;
		
		// setup the debug info recorder
		
		try {
			output = new PrintStream(new File("dpdebug.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public Set<AbstractModel> initModels() {
		
		// initialize models
		unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, distributionCapacity, distributionWindow);
		
		revenueModels = new HashMap<Query, AbstractRevenueModel>(); 
		for (Query query : _querySpace) {
			revenueModels.put(query, new RevenueMovingAvg(query, _retailCatalog));
		}
		
		prConversionModels = new HashMap<Query, AbstractPrConversionModel>();
		for (Query query : _querySpace) {
			prConversionModels.put(query, new SimplePrConversion(query, _advertiserInfo, unitsSoldModel));	
		}
		
		slotToBidModels = new HashMap<Query, SimpleSlotToBid>();
		for (Query query : _querySpace) {
			slotToBidModels.put(query, new SimpleSlotToBid(query, _slotInfo));
		}
		
		slotToClicksModels = new HashMap<Query, AbstractSlotToNumClicks>();
		for (Query query : _querySpace) {
			slotToClicksModels.put(query, new StaticSlotToClicks(query, _advertiserInfo, _slotInfo));
		}
/*		userModel = new BasicUserModel();
		queryToNumImpModel = new BasicQueryToNumImp(userModel);
		bidToCPC = new RegressionBidToCPC(_querySpace);
		bidToPrClick = new RegressionBidToPrClick(_querySpace);*/
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// update models
		
		if (_day > 1) {
			unitsSoldModel.update(_salesReport);
			
/*			userModel.updateModel(queryReport, salesReport);
			queryToNumImpModel.updateModel(queryReport, salesReport);
			bidToCPC.updateModel(queryReport, oldBidBundle);
			bidToPrClick.updateModel(queryReport, oldBidBundle);*/
			
			for (Query query : _querySpace) {
				revenueModels.get(query).update(_salesReport, _queryReport);
				prConversionModels.get(query).updateModel(_queryReport, _salesReport);
				slotToBidModels.get(query).updateModel(_queryReport, _salesReport, oldBidBundle);
				if (oldTargetPosition != null && oldTargetPosition.get(query) != null)
					slotToBidModels.get(query).updateByAgent(oldTargetPosition.get(query), _queryReport, oldBidBundle);
				slotToClicksModels.get(query).updateModel(_queryReport, _salesReport, oldBidBundle);
			
			}
			
			
			
		}
	}
	
	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("Window Sold: ").append(unitsSoldModel.getWindowSold()).append("\n");
		buff.append("\t").append("Distribution Cap: ").append(distributionCapacity).append("\n");
		buff.append("\t").append("Yesterday sold: ").append(unitsSoldModel.getLatestSample()).append("\n");
		buff.append("\t").append("Estimated sold: ").append(unitsSoldModel.getEstimate()).append("\n");
		buff.append("\t").append("Manufacturer specialty: ").append(_advertiserInfo.getManufacturerSpecialty()).append("\n");
		buff.append("\t").append("Slots: ").append(slots).append("\n");
		buff.append("****************\n");
		for(Query q : _querySpace){
			buff.append("\t").append("Day: ").append(_day).append("\n");
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(oldBidBundle.getBid(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(oldBidBundle.getDailyLimit(q)).append("\n");
			buff.append("\t").append("TargetPosition: ").append(oldTargetPosition.get(q)).append("\n");
			buff.append("\t").append("TargetClicks: ").append(oldBidBundle.getDailyLimit(q)/oldBidBundle.getBid(q)).append("\n");
			buff.append("\t").append("Average Position:").append(_queryReport.getPosition(q)).append("\n");
			buff.append("\t").append("CPC:").append(_queryReport.getCPC(q)).append("\n");
			if (!Double.isNaN(_queryReport.getPosition(q))) {
				int pos = (int) Math.ceil(_queryReport.getPosition(q));
				buff.append("\t").append("maxClick of this position: ").append(slotToClicksModels.get(q).getPrediction(pos)).append("\n");
				buff.append("\t").append("bid of this position: ").append(slotToBidModels.get(q).getPrediction(pos)).append("\n");
			}
			else {
				buff.append("\t").append("maxClick of this Position: ").append(slotToClicksModels.get(q).getPrediction(slots)).append("\n");
				buff.append("\t").append("bid of this position: ").append(slotToBidModels.get(q).getPrediction(slots)).append("\n");
			}
			buff.append("\t").append("Clicks: ").append(_queryReport.getClicks(q)).append("\n");
			buff.append("\t").append("Conversions: ").append(_salesReport.getConversions(q)).append("\n");
			buff.append("****************\n");
		}
		
		System.out.println(buff);
		output.append(buff);
		output.flush();
	
	}

}
