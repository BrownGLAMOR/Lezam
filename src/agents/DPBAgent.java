package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.EnsembleBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.prconv.SimplePrConversion;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.revenue.AbstractRevenueModel;
import newmodels.revenue.RevenueMovingAvg;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class DPBAgent extends SimAbstractAgent {

	// models
	protected NewAbstractConversionModel prConversionModel;
	private AbstractBidToCPC bidToCPCModel;
	private AbstractQueryToNumImp queryToNumImpModel;
	private AbstractBidToPrClick bidToPrClickModel;
	private AbstractUserModel userModel;

	// model, strategy related variables
	protected BidBundle bidBundle = null;
	ArrayList<BidBundle> bidBundleList = new ArrayList<BidBundle>();
	private Random _R = new Random();

	// constants
	protected int distributionCapacity;
	protected int distributionWindow;
	protected int dailyCapacity;

	protected int promotedSlots;
	protected int regularSlots;
	protected int slots;
	
	protected HashMap<Query, Double> revenues;

	protected final int MAX_TIME_HORIZON = 5;

	// for debug
	protected PrintStream output;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		// for jberg's simulator, but should have no effect

		this.buildMaps(models);

		// handle first two days

		if (_day <= 6) {
			bidBundle = new BidBundle();

				for (Query q : _querySpace) {
					double bid;
					if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
						bid = randDouble(.1, .6);
					else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
						bid = randDouble(.25, .75);
					else
						bid = randDouble(.35, 1.0);
					bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
				}
			
			for(Query query : _querySpace) {
				double bid;
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.1,.6);
				else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.25,.75);
				else 
					bid = randDouble(.35,1.0);
					bidBundle.addQuery(query, bid, new Ad(), Double.NaN);
			}
			bidBundleList.add(bidBundle);
			
			return bidBundle;
		}

		// find relevant variables

		int unitsSold = 0;
		for (Query query : _querySpace) {
			unitsSold += _salesReport.getConversions(query);
		}

		// int targetCapacity = (int)Math.max(2*1.5*dailyCapacity - unitsSold,
		// dailyCapacity*.5);
		int targetCapacity = (int) (2 * dailyCapacity);

		HashMap<Query, HashMap<Double, Integer>> item = new HashMap<Query, HashMap<Double, Integer>>();

		for (Query query : _querySpace) {
			HashMap<Double, Integer> bidToClicks = new HashMap<Double, Integer>();

			double bid = .1;
			double maxBid = Math.min(revenues.get(query)* prConversionModel.getPrediction(query) * 2, 5);
			while (bid <= maxBid) {
				double prClicks = bidToPrClickModel.getPrediction(query, bid,
						null);
				double imp = queryToNumImpModel.getPrediction(query);
				bidToClicks.put(bid, (int) (prClicks * imp));
				bid += .1;
			}

			item.put(query, bidToClicks);
		}

		// mckp

		System.out.println("targetCapacity : " + targetCapacity);
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

			for (int j = 0; j < targetCapacity; j++) {
				for (double bid : item.get(query).keySet()) {

					int maxClick = item.get(query).get(bid);
					double prConv = prConversionModel.getPrediction(query);

					for (int capacity = 0; capacity / prConv <= maxClick; capacity++) {
						if (j - capacity < 0) break; 
						double tmp = 0;
						if (i > 0) tmp = profit[i - 1][j - capacity];
						double rev = revenues.get(query);
						double cpc = bidToCPCModel.getPrediction(query, bid);
						tmp += capacity * (rev - cpc / prConv);

						if (tmp >= profit[i][j]) {
							profit[i][j] = tmp;
							bids[i][j] = bid;
							sales[i][j] = capacity;
						}
					}
				}
			}
			i++;
		}
		
		// build bid bundle

		double maxProfit = 0;
		int capacity = 0;
		for (int j = 0; j < targetCapacity; j++)
			if (profit[_querySpace.size() - 1][j] >= maxProfit) {
				maxProfit = profit[_querySpace.size() - 1][j];
				capacity = j;
			}
		
		bidBundle = new BidBundle();

		output.printf("********************\n");
		output.printf("Max profit : %f.\n", maxProfit);
		output.printf("Capacity is : %d.\n", capacity);
		output.printf("Target Capacity : %d. \n", targetCapacity);
		output.printf("********************\n");
		
		while (i > 0) {
			i--;
			double bid = bids[i][capacity];
			double clicks = sales[i][capacity] * 1.0/ prConversionModel.getPrediction(queries[i]);
			double cpc = bidToCPCModel.getPrediction(queries[i], bid);
			// double dailyLimit = Math.max(cpc * (clicks - 1) + bid, bid);
			double dailyLimit = Math.max(bid * clicks, bid);

			bidBundle.setBid(queries[i], bid);
			bidBundle.setDailyLimit(queries[i], dailyLimit);

			output.println(queries[i] + " " + bid + " " + sales[i][capacity]
					+ " "+ profit[i][capacity] + " " + capacity);

			capacity -= sales[i][capacity];
		}
		
		System.out.printf("********************\n");
		this.printInfo();

		bidBundleList.add(bidBundle);
		return bidBundle;
	}

	@Override
	public void initBidder() {

		// set constants

		distributionCapacity = _advertiserInfo.getDistributionCapacity();
		distributionWindow = _advertiserInfo.getDistributionWindow();
		dailyCapacity = (int) (distributionCapacity / distributionWindow);

		promotedSlots = _slotInfo.getPromotedSlots();
		regularSlots = _slotInfo.getRegularSlots();
		slots = promotedSlots + regularSlots;

		// initialize strategy related variables

		bidBundle = null;

		// setup the debug info recorder

		try {
			output = new PrintStream(new File("dpbdebug.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Set<AbstractModel> initModels() {

		// initialize models
		Set<AbstractModel> models = new HashSet<AbstractModel>();

		userModel = new BasicUserModel();
		models.add(userModel);
		queryToNumImpModel = new BasicQueryToNumImp(userModel);
		models.add(queryToNumImpModel);
		bidToCPCModel = new RegressionBidToCPC(_querySpace);
		models.add(bidToCPCModel);
		bidToPrClickModel = new EnsembleBidToPrClick(_querySpace);
		((EnsembleBidToPrClick)bidToPrClickModel).initializeEnsemble();
		models.add(bidToPrClickModel);

		revenues = new HashMap<Query, Double>();
		for(Query query:_querySpace){
			if (query.getType()== QueryType.FOCUS_LEVEL_ZERO)
				revenues.put(query, 10.0 + 5/3);
			if (query.getType()== QueryType.FOCUS_LEVEL_ONE){
				if(_manSpecialty.equals(query.getManufacturer())) revenues.put(query, 15.0);
				else{
					if(query.getManufacturer() != null) revenues.put(query, 10.0);
					else revenues.put(query, 10.0 + 5/3);
				}
			}
			if(query.getType()== QueryType.FOCUS_LEVEL_TWO){
				if(_manSpecialty.equals(query.getManufacturer())) revenues.put(query, 15.0);
				else revenues.put(query, 10.0);
			}
		}
		
		prConversionModel = new GoodConversionPrModel(_querySpace, new BasicTargetModel(_manSpecialty, _compSpecialty));

		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// update models

		if (_day > 1 && _salesReport != null && _queryReport != null) {

			userModel.updateModel(queryReport, salesReport);
			queryToNumImpModel.updateModel(queryReport, salesReport);
			if (bidBundleList.size() > 1) {
				bidToCPCModel.updateModel(queryReport, salesReport, bidBundleList
						.get(bidBundleList.size() - 2));
				bidToPrClickModel.updateModel(queryReport, salesReport, bidBundleList
						.get(bidBundleList.size() - 2));
			}

			int timeHorizon = (int) Math.min(Math.max(1, _day - 1),
					MAX_TIME_HORIZON);
			prConversionModel.setTimeHorizon(timeHorizon);
			prConversionModel.updateModel(queryReport, salesReport, bidBundleList
					.get(bidBundleList.size() - 2));

		}
	}

	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	protected void buildMaps(Set<AbstractModel> models) {
		for (AbstractModel model : models) {
			if (model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				this.userModel = userModel;
			} else if (model instanceof AbstractQueryToNumImp) {
				AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
				this.queryToNumImpModel = queryToNumImp;
			} else if (model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				this.bidToCPCModel = bidToCPC;
			} else if (model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				this.bidToPrClickModel = bidToPrClick;
			} else {
				// throw new
				// RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)"+model);
			}
		}
	}

	protected void printInfo() {
		// print debug info
		StringBuffer buff = new StringBuffer(255);
		buff.append("****************\n");
		buff.append("\t").append("Day: ").append(_day).append("\n");
		buff.append("\t").append("Distribution Cap: ").append(
				distributionCapacity).append("\n");
		buff.append("\t").append("Manufacturer specialty: ").append(
				_advertiserInfo.getManufacturerSpecialty()).append("\n");
		buff.append("\t").append("Slots: ").append(slots).append("\n");
		buff.append("****************\n");
		for (Query q : _querySpace) {
			buff.append("\t").append("Day: ").append(_day).append("\n");
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(bidBundleList.get(bidBundleList.size() - 2).getBid(q))
					.append("\n");
			buff.append("\t").append("SpendLimit: ").append(
					bidBundleList.get(bidBundleList.size() - 2).getDailyLimit(q)).append("\n");
			buff.append("\t").append("Average Position:").append(
					_queryReport.getPosition(q)).append("\n");
			buff.append("\t").append("CPC:").append(_queryReport.getCPC(q))
					.append("\n");
			buff.append("\t").append("Clicks: ").append(
					_queryReport.getClicks(q)).append("\n");
			buff.append("\t").append("Conversions: ").append(
					_salesReport.getConversions(q)).append("\n");
			if (_salesReport.getConversions(q) > 0) {
				buff.append("\t").append("Conversions Pr: ").append(
					_salesReport.getConversions(q)/_queryReport.getClicks(q)).append("\n");}
			else buff.append("\t").append("Conversions Pr: ").append("No clicks").append("\n");
			buff.append("\t").append("Conversions: ").append(prConversionModel.getPrediction(q)).append("\n");
			buff.append("****************\n");
		}

		System.out.println(buff);
		output.append(buff);
		output.flush();

	}

}
