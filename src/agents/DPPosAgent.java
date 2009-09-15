package agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import newmodels.AbstractModel;
import newmodels.avgpostoposdist.AvgPosToPosDist;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtopos.AbstractBidToPosModel;
import newmodels.bidtopos.BidToPosInverter;
import newmodels.bidtopos.EnsembleBidToPos;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.EnsembleBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.postocpc.AbstractPosToCPC;
import newmodels.postocpc.EnsemblePosToCPC;
import newmodels.postoprclick.AbstractPosToPrClick;
import newmodels.postoprclick.BasicPosToPrClick;
import newmodels.postoprclick.EnsemblePosToPrClick;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.AbstractConversionModel;
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
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class DPPosAgent extends AbstractAgent {

	// models
	protected AbstractConversionModel prConversionModel;
	private AbstractPosToCPC posToCPCModel;
	private AbstractQueryToNumImp queryToNumImpModel;
	private AbstractPosToPrClick posToPrClickModel;
	private AbstractBidToPosModel _bidToPos;
	private AvgPosToPosDist _avgPosDist;
	private BidToPosInverter _bidToPosInverter;
	private AbstractUserModel userModel;
	protected BasicTargetModel targetModel;

	// model, strategy related variables
	protected BidBundle bidBundle = null;
	ArrayList<BidBundle> bidBundleList = new ArrayList<BidBundle>();
	private Random _R = new Random();

	// constants
	protected int distributionCapacity;
	protected int distributionWindow;
	protected int dailyCapacity;
	
	protected HashMap<Query, Double> revenues;
	protected HashMap<Query, Double> _baselineConv;

	protected final int MAX_TIME_HORIZON = 5;
	
	// control variables
	protected final boolean TARGET = false;
	// adjustment for inaccuracy of model, set it to false if tested under perfect models
	protected final boolean ADJUSTMENT = true; 
	// this algorithm must not do without budget

	private int lagDays = 5;
	
	// for debug
	protected PrintStream output;
	private double _outOfAuction = 6.0;

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {

		// for jberg's simulator, but should have no effect

		this.buildMaps(models);

		// handle first few days

		if (_day <= lagDays) {
			bidBundle = new BidBundle();
			
			for(Query query : _querySpace) {
				double bid;
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.1,.6);
				else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.25,.75);
				else 
					bid = randDouble(.35,1.0);
				
				bidBundle.setBid(query, bid);
				
				if (TARGET) {
					if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
						bidBundle.setAd(query, new Ad(new Product(_manSpecialty, _compSpecialty)));
					if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getComponent() == null)
						bidBundle.setAd(query, new Ad(new Product(query.getManufacturer(), _compSpecialty)));
					if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getManufacturer() == null)
						bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
					if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO) && query.getManufacturer().equals(_manSpecialty)) 
						bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
				}	
			}
			
			bidBundleList.add(bidBundle);
			
			return bidBundle;
		}
		
		if(_day > lagDays + 2) {
			QueryReport queryReport = _queryReports.getLast();
			SalesReport salesReport = _salesReports.getLast();
		}

		// find relevant variables

		int unitsSold = 0;
		for (Query query : _querySpace) {
			unitsSold += _salesReport.getConversions(query);
		}

		int targetCapacity = (int) (2*dailyCapacity);

		HashMap<Query, HashMap<Double, Integer>> item = new HashMap<Query, HashMap<Double, Integer>>();

		for (Query query : _querySpace) {
			HashMap<Double, Integer> posToClicks = new HashMap<Double, Integer>();

			double minPos = 1.0;
			double maxPos = _outOfAuction;
			double increment = .25;
			
			if (ADJUSTMENT) {
				minPos = Math.max(minPos, _bidBundles.getLast().getBid(query) - 2);
				maxPos = Math.min(_bidBundles.getLast().getBid(query) + 2, maxPos);
			}
			
			double pos = minPos;
			while (pos <= maxPos) {
				double prClicks = posToPrClickModel.getPrediction(query, pos, null);
				if (TARGET) prClicks = targetModel.getClickPrPrediction(query, prClicks, false);
				double imp = queryToNumImpModel.getPrediction(query);
				
				double maxClicks = prClicks * imp;
				
				if (ADJUSTMENT) maxClicks = Math.min((_queryReport.getClicks(query)+1)*5, prClicks * imp);
				
				posToClicks.put(pos, (int) (maxClicks));
				pos += increment;
			}

			item.put(query, posToClicks);
		}

		// dp algorithm

		System.out.println("targetCapacity : " + targetCapacity);
		double[][] profit = new double[_querySpace.size()][];
		Query[] queries = new Query[_querySpace.size()];
		double[][] positions = new double[_querySpace.size()][];
		int[][] sales = new int[_querySpace.size()][];

		int i = 0;
		for (Query query : _querySpace) {
			queries[i] = query;
			profit[i] = new double[targetCapacity];
			for (int j = 0; j < targetCapacity; j++)
				profit[i][j] = 0;
			positions[i] = new double[targetCapacity];
			sales[i] = new int[targetCapacity];

			for (int j = 0; j < targetCapacity; j++) {
				for (double pos : item.get(query).keySet()) {

					int maxClick = item.get(query).get(pos);
					double prConv = prConversionModel.getPrediction(query);

					for (int capacity = 0; capacity / prConv <= maxClick; capacity++) {
						if (j - capacity < 0) break; 
						double tmp = 0;
						if (i > 0) tmp = profit[i - 1][j - capacity];
						double rev = revenues.get(query);
						double cpc = posToCPCModel.getPrediction(query, pos);
						
						if (TARGET) {
							double clickPr = posToPrClickModel.getPrediction(query, pos, null);
							if (clickPr <=0 || clickPr >= 1) clickPr = .2;
							prConv = targetModel.getConvPrPrediction(query, clickPr, prConv, 0);
							rev = targetModel.getUSPPrediction(query, clickPr, 0);
						}
						
						tmp += capacity * (rev - cpc / prConv);

						if (tmp >= profit[i][j]) {
							profit[i][j] = tmp;
							positions[i][j] = pos;
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
			double bid = Double.NaN;
			double dailyLimit = Double.NaN;
			if (sales[i][capacity] > 0) {
				double pos = positions[i][capacity];
				bid = _bidToPosInverter.getPrediction(queries[i], pos);
				if(Double.isNaN(bid)) {
					if (queries[i].getType().equals(QueryType.FOCUS_LEVEL_ZERO))
						bid = randDouble(.1, .6);
					else if (queries[i].getType().equals(QueryType.FOCUS_LEVEL_ONE))
						bid = randDouble(.25, .75);
					else
						bid = randDouble(.35, 1.0);
				}
				double prConv = prConversionModel.getPrediction(queries[i]);
				if (Double.isNaN(prConv) || prConv == 0) prConv = _baselineConv.get(queries[i]);
				double clicks = sales[i][capacity] * 1.0/ prConv;
				
				// here we still prefer to set a generous bound
				// empirically, using cpc is too tight
				dailyLimit = bid * clicks;
			}
			else {
				if (queries[i].getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.1, .6);
				else if (queries[i].getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.25, .75);
				else
					bid = randDouble(.35, 1.0);
				dailyLimit = bid * 2;
			}
			
			bidBundle.setBid(queries[i], bid);
			bidBundle.setDailyLimit(queries[i], dailyLimit);

			output.println(queries[i] + " " + bid + " " + sales[i][capacity]
					+ " "+ profit[i][capacity] + " " + capacity);

			capacity -= sales[i][capacity];
		}
		
		if (TARGET)
			for (Query query : _querySpace) {

				if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bidBundle.setAd(query, new Ad(new Product(_manSpecialty,
							_compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)
						&& query.getComponent() == null)
					bidBundle.setAd(query, new Ad(new Product(query
							.getManufacturer(), _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)
						&& query.getManufacturer() == null)
					bidBundle.setAd(query, new Ad(new Product(_manSpecialty,
							query.getComponent())));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)
						&& query.getManufacturer().equals(_manSpecialty))
					bidBundle.setAd(query, new Ad(new Product(_manSpecialty,
							query.getComponent())));

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
		dailyCapacity = (int) (distributionCapacity*1.0 / distributionWindow);

		// initialize strategy related variables
		
		_baselineConv = new HashMap<Query, Double>();
        for(Query q: _querySpace){
        	if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) _baselineConv.put(q, 0.1);
        	if(q.getType() == QueryType.FOCUS_LEVEL_ONE){
        		if(q.getComponent() == _compSpecialty) _baselineConv.put(q, 0.27);
        		else _baselineConv.put(q, 0.2);
        	}
        	if(q.getType()== QueryType.FOCUS_LEVEL_TWO){
        		if(q.getComponent()== _compSpecialty) _baselineConv.put(q, 0.39);
        		else _baselineConv.put(q,0.3);
        	}
        }

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

		targetModel = new BasicTargetModel(_manSpecialty,_compSpecialty);

		prConversionModel = new HistoricPrConversionModel(_querySpace, targetModel);
		models.add(prConversionModel);
		userModel = new BasicUserModel();
		models.add(userModel);
		queryToNumImpModel = new BasicQueryToNumImp(userModel);
		models.add(queryToNumImpModel);
		posToCPCModel = new EnsemblePosToCPC(_querySpace, 12, 30, false, true);
		models.add(posToCPCModel);
		BasicTargetModel basicTargModel = new BasicTargetModel(_manSpecialty,_compSpecialty);
		models.add(basicTargModel);
		posToPrClickModel = new EnsemblePosToPrClick(_querySpace, 12, 30, basicTargModel, false, true);
		models.add(posToPrClickModel);
		AbstractBidToPosModel bidToPos = new EnsembleBidToPos(_querySpace,12,30,false,true);
		BasicPosToPrClick posToPrClickModel = new BasicPosToPrClick(_numPS);
		AvgPosToPosDist avgPosToDistModel = new AvgPosToPosDist(40, _numPS, posToPrClickModel);
		BidToPosInverter bidToPosInverter;
		try {
			bidToPosInverter = new BidToPosInverter(new RConnection(), _querySpace, .1, 0.0, 3.0);
		} catch (RserveException e) {
			throw new RuntimeException("Cannot Access Rserve");
		}
		models.add(bidToPos);
		models.add(avgPosToDistModel);
		models.add(bidToPosInverter);

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

		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// update models

		if (_day > 1 && _salesReport != null && _queryReport != null) {

			userModel.updateModel(queryReport, salesReport);
			queryToNumImpModel.updateModel(queryReport, salesReport);
			if (bidBundleList.size() > 1) {
				posToCPCModel.updateModel(queryReport, salesReport, bidBundleList
						.get(bidBundleList.size() - 2));
				posToPrClickModel.updateModel(queryReport, salesReport, bidBundleList
						.get(bidBundleList.size() - 2));
				
				HashMap<Query,double[]> posDists = new HashMap<Query, double[]>();
				for(Query query : _querySpace) {
					double[] posDist = _avgPosDist.getPrediction(query, queryReport.getRegularImpressions(query), queryReport.getPromotedImpressions(query), queryReport.getPosition(query), queryReport.getClicks(query));
					posDists.put(query, posDist);
				}
				_bidToPos.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2),posDists);
				
				_bidToPosInverter.updateModel(_bidToPos);
			}

			int timeHorizon = (int) Math.min(Math.max(1, _day - 1),
					MAX_TIME_HORIZON);
			((HistoricPrConversionModel) prConversionModel).setTimeHorizon(timeHorizon);
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
			} else if (model instanceof AbstractPosToCPC) {
				AbstractPosToCPC posToCPC = (AbstractPosToCPC) model;
				this.posToCPCModel = posToCPC;
			} else if (model instanceof AbstractPosToPrClick) {
				AbstractPosToPrClick posToPrClick = (AbstractPosToPrClick) model;
				this.posToPrClickModel = posToPrClick;
			}
			else if(model instanceof AbstractBidToPosModel) {
				AbstractBidToPosModel bidToPos = (AbstractBidToPosModel) model;
				_bidToPos = bidToPos;
			}
			else if(model instanceof AvgPosToPosDist) {
				AvgPosToPosDist avgPosDist = (AvgPosToPosDist) model;
				_avgPosDist = avgPosDist;
			}
			else if(model instanceof BidToPosInverter) {
				BidToPosInverter bidToPosInverter = (BidToPosInverter) model;
				_bidToPosInverter = bidToPosInverter;
			}
			else {
				//TODO conversion pr model????
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
		buff.append("****************\n");
		for (Query q : _querySpace) {
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
					_salesReport.getConversions(q)*1.0/_queryReport.getClicks(q)).append("\n");}
			else buff.append("\t").append("Conversions Pr: ").append("No click").append("\n");
			buff.append("\t").append("Predicted Conversion Pr: ").append(prConversionModel.getPrediction(q)).append("\n");
			if (_queryReport.getImpressions(q) > 0) {
				buff.append("\t").append("Click Pr: ").append(
					_queryReport.getClicks(q)*1.0/_queryReport.getImpressions(q)).append("\n");}
			else buff.append("\t").append("Conversions Pr: ").append("No impression").append("\n");
			if (_salesReport.getConversions(q) > 0) {
				buff.append("\t").append("PPS: ").append(
					(_salesReport.getRevenue(q)-_queryReport.getCost(q))/_salesReport.getConversions(q)).append("\n");}
			else buff.append("\t").append("PPS: ").append("No conversion").append("\n");
			if (_queryReport.getClicks(q) > 0) {
				buff.append("\t").append("PPC: ").append(
					(_salesReport.getRevenue(q)-_queryReport.getCost(q))/_queryReport.getClicks(q)).append("\n");}
			else buff.append("\t").append("PPC: ").append("No click").append("\n");
			buff.append("****************\n");
		}

		System.out.println(buff);
		output.append(buff);
		output.flush();

	}

}