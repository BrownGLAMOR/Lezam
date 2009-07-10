package agents;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.bidtocpc.RegressionBidToCPC;
import newmodels.bidtopos.BucketBidToPositionModel;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.EnsembleBidToPrClick;
import newmodels.bidtoprclick.RegressionBidToPrClick;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.prconv.NewAbstractConversionModel;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.sales.SalesDistributionModel;
import newmodels.slottoprclick.NewAbstractPosToPrClick;
import newmodels.slottoprclick.RegressionPosToPrClick;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.BasicUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import agents.mckp.IncItem;
import agents.mckp.Item;
import agents.mckp.ItemComparatorByWeight;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public class MCKPAgentMkIIBids extends SimAbstractAgent {

	private static final int MAX_TIME_HORIZON = 5;

	private static final boolean MODELCONVPR = false;
	private static final boolean TARGET = false;

	private Random _R = new Random();
	private boolean DEBUG = false;
	private double LAMBDA = .995;
	private int _numUsers = 90000;
	private HashMap<Query, Double> _salesPrices;
	private HashMap<Query, Double> _baseConvProbs;
	private AbstractUserModel _userModel;
	private AbstractQueryToNumImp _queryToNumImpModel;
	private AbstractBidToCPC _bidToCPC;
	private AbstractBidToPrClick _bidToPrClick;
	private AbstractUnitsSoldModel _unitsSold;
	private GoodConversionPrModel _convPrModel;
	private HistoricPrConversionModel _donnieConvPrModel;
	private SalesDistributionModel _salesDist;
	private BasicTargetModel _targModel;
	private Hashtable<Query, Integer> _queryId;
	private LinkedList<Double> bidList;
	private int _capacityInc = 10;
	private int lagDays = 4;


	/*
	 * For error calculations
	 */
	private LinkedList<HashMap<Query, Double>> CPCPredictions, ClickPrPredictions, ConvPrPredictions, DonnieConvPrPredictions, ImpPredictions;
	private double sumCPCError, sumClickPrError, sumConvPrError, sumDonnieConvPrError, sumImpError;
	private int errorDayCounter, prConvSkip = 0, impSkip = 0;

	public MCKPAgentMkIIBids() {
		bidList = new LinkedList<Double>();
		//		double increment = .25;
		double increment  = .02;
		double min = .04;
		double max = 2;
		int tot = (int) Math.ceil((max-min) / increment);
		for(int i = 0; i < tot; i++) {
			bidList.add(min+(i*increment));
		}
		CPCPredictions = new LinkedList<HashMap<Query,Double>>();
		ClickPrPredictions = new LinkedList<HashMap<Query,Double>>();
		ConvPrPredictions = new LinkedList<HashMap<Query,Double>>();
		DonnieConvPrPredictions = new LinkedList<HashMap<Query,Double>>();
		ImpPredictions = new LinkedList<HashMap<Query,Double>>();

		sumCPCError = 0.0;
		sumClickPrError = 0.0;
		sumConvPrError = 0.0;
		sumImpError = 0.0;
		errorDayCounter = 0;
	}



	@Override
	public Set<AbstractModel> initModels() {
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		AbstractUserModel userModel = new BasicUserModel();
		AbstractQueryToNumImp queryToNumImp = new BasicQueryToNumImp(userModel);
		AbstractBidToCPC bidToCPC = new EnsembleBidToCPC(_querySpace, 5, 8, null);
		((EnsembleBidToCPC) bidToCPC).initializeEnsemble();
		AbstractBidToPrClick bidToPrClick = new EnsembleBidToPrClick(_querySpace, 5, 25, null);
		((EnsembleBidToPrClick) bidToPrClick).initializeEnsemble();
		AbstractUnitsSoldModel unitsSold = new BasicUnitsSoldModel(_querySpace,_capacity,_capWindow);
		BasicTargetModel basicTargModel = new BasicTargetModel(_manSpecialty,_compSpecialty);
		GoodConversionPrModel convPrModel = new GoodConversionPrModel(_querySpace,basicTargModel);
		HistoricPrConversionModel convDonniePrModel = new HistoricPrConversionModel(_querySpace,basicTargModel);
		SalesDistributionModel salesDist = new SalesDistributionModel(_querySpace);
		models.add(userModel);
		models.add(queryToNumImp);
		models.add(bidToCPC);
		models.add(bidToPrClick);
		models.add(unitsSold);
		models.add(convPrModel);
		models.add(convDonniePrModel);
		models.add(salesDist);
		models.add(basicTargModel);
		return models;
	}

	protected void buildMaps(Set<AbstractModel> models) {
		for(AbstractModel model : models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				_userModel = userModel;
			}
			else if(model instanceof AbstractQueryToNumImp) {
				AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
				_queryToNumImpModel = queryToNumImp;
			}
			else if(model instanceof AbstractUnitsSoldModel) {
				AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
				_unitsSold = unitsSold;
			}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				_bidToCPC = bidToCPC; 
			}
			else if(model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				_bidToPrClick = bidToPrClick;
			}
			else if(model instanceof GoodConversionPrModel) {
				GoodConversionPrModel convPrModel = (GoodConversionPrModel) model;
				_convPrModel = convPrModel;
			}
			else if(model instanceof HistoricPrConversionModel) {
				HistoricPrConversionModel donnieConvPrModel = (HistoricPrConversionModel) model;
				_donnieConvPrModel = donnieConvPrModel;
			}
			else if(model instanceof SalesDistributionModel) {
				SalesDistributionModel salesDist = (SalesDistributionModel) model;
				_salesDist = salesDist;
			}
			else if(model instanceof BasicTargetModel) {
				BasicTargetModel targModel = (BasicTargetModel) model;
				_targModel = targModel;
			}
			else {
				//				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)"+model);
			}
		}
	}

	@Override
	public void initBidder() {

		_baseConvProbs = new HashMap<Query, Double>();

		// set revenue prices
		_salesPrices = new HashMap<Query,Double>();
		for(Query q : _querySpace) {

			String manufacturer = q.getManufacturer();
			if(_manSpecialty.equals(manufacturer)) {
				_salesPrices.put(q, 10*(_MSB+1));
			}
			else if(manufacturer == null) {
				_salesPrices.put(q, (10*(_MSB+1)) * (1/3.0) + (10)*(2/3.0));
			}
			else {
				_salesPrices.put(q, 10.0);
			}

			if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				_baseConvProbs.put(q, _piF0);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baseConvProbs.put(q, _piF1);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				_baseConvProbs.put(q, _piF2);
			}
			else {
				throw new RuntimeException("Malformed query");
			}

			String component = q.getComponent();
			if(_compSpecialty.equals(component)) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB));
			}
			else if(component == null) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB)*(1/3.0) + _baseConvProbs.get(q)*(2/3.0));
			}
		}
		_queryId = new Hashtable<Query,Integer>();
		int i = 0;
		for(Query q : _querySpace){
			i++;
			_queryId.put(q, i);
		}
	}


	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {

		for(AbstractModel model: _models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				userModel.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractQueryToNumImp) {
				AbstractQueryToNumImp queryToNumImp = (AbstractQueryToNumImp) model;
				queryToNumImp.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractUnitsSoldModel) {
				AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
				unitsSold.update(salesReport);
			}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				bidToCPC.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			}
			else if(model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				bidToPrClick.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			}
			else if(model instanceof NewAbstractConversionModel) {
				NewAbstractConversionModel convPrModel = (NewAbstractConversionModel) model;
				int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
				if(model instanceof GoodConversionPrModel) {
					GoodConversionPrModel adMaxModel = (GoodConversionPrModel) convPrModel;
					adMaxModel.setTimeHorizon(timeHorizon);
				}
				if(model instanceof HistoricPrConversionModel) {
					HistoricPrConversionModel adMaxModel = (HistoricPrConversionModel) convPrModel;
					adMaxModel.setTimeHorizon(timeHorizon);
				}
				convPrModel.updateModel(queryReport, salesReport,_bidBundles.get(_bidBundles.size()-2));
			}
			else if(model instanceof SalesDistributionModel) {
				SalesDistributionModel salesDist = (SalesDistributionModel) model;
				salesDist.updateModel(salesReport);
			}
			else if(model instanceof BasicTargetModel) {
				//Do nothing
			}
			else {
				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)");
			}
		}
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();
		double numIncItemsPerSet = 0;
		if(_day > lagDays){
			buildMaps(models);
			//NEED TO USE THE MODELS WE ARE PASSED!!!

			HashMap<Query,Double> dailyCPCPredictions = new HashMap<Query, Double>();
			HashMap<Query,Double> dailyClickPrPredictions = new HashMap<Query, Double>();
			HashMap<Query,Double> dailyConvPrPredictions = new HashMap<Query, Double>();
			HashMap<Query,Double> dailyDonnieConvPrPredictions = new HashMap<Query, Double>();
			HashMap<Query,Double> dailyImpPredictions = new HashMap<Query, Double>();
			LinkedList<IncItem> allIncItems = new LinkedList<IncItem>();

			//want the queries to be in a guaranteed order - put them in an array
			//index will be used as the id of the query
			for(Query q : _querySpace) {
				LinkedList<Item> itemList = new LinkedList<Item>();
				debug("Query: " + q);
				for(int i = 0; i < bidList.size(); i++) {
					double salesPrice = _salesPrices.get(q);
					double bid = bidList.get(i);
					double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
					double numImps = _queryToNumImpModel.getPrediction(q);
					int numClicks = (int) (clickPr * numImps);
					double CPC = _bidToCPC.getPrediction(q, bid);
					double convProb = _convPrModel.getPrediction(q);

					if(Double.isNaN(clickPr)) {
						System.out.println("I AM A DOUCHE!");
						clickPr = 0.0;
					}

					if(Double.isNaN(convProb)) {
						System.out.println("BRADDMAX IS A DOUCHE!");
						convProb = 0.0;
					}

					debug("\tBid: " + bid);
					debug("\tCPC: " + CPC);
					debug("\tClickPr: " + clickPr);
					debug("\tConv Prob: " + convProb);
					debug("\tNumImps: " + numImps);
					debug("\tNumClicks: " + numClicks);

					double w = numClicks*convProb;				//weight = numClciks * convProv
					double v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]

					int isID = _queryId.get(q);
					itemList.add(new Item(q,w,v,bid,false,isID));

					if(TARGET) {
						if(clickPr != 0 && Double.isNaN(_targModel.getClickPrPrediction(q, clickPr, false))) {
							throw new RuntimeException("ClickPr" + q + "  " + clickPr);
						}
						if(convProb != 0 && Double.isNaN(_targModel.getConvPrPrediction(q, clickPr, convProb, false))) {
							throw new RuntimeException("ConvPr" + q + "  " + clickPr + "  " + convProb);
						}
						if(Double.isNaN(_targModel.getUSPPrediction(q, clickPr, false))) {
							throw new RuntimeException("USP" + q + "  " + clickPr);
						}

						/*
						 * add a targeted version of our bid as well
						 */
						if(clickPr != 0) {
							numClicks *= _targModel.getClickPrPrediction(q, clickPr, false);
							if(convProb != 0) {
								convProb *= _targModel.getConvPrPrediction(q, clickPr, convProb, false);
							}
							salesPrice = _targModel.getUSPPrediction(q, clickPr, false);
						}

						w = numClicks*convProb;				//weight = numClciks * convProv
						v = numClicks*convProb*salesPrice - numClicks*CPC;	//value = revenue - cost	[profit]

						itemList.add(new Item(q,w,v,bid,true,isID));
					}
				}
				debug("Items for " + q);
				Item[] items = itemList.toArray(new Item[0]);
				IncItem[] iItems = getIncremental(items);
				numIncItemsPerSet += iItems.length;
				allIncItems.addAll(Arrays.asList(iItems));
			}
			numIncItemsPerSet /= 16.0;
			debug(numIncItemsPerSet);
			double budget = _capacity/_capWindow;
			if(_day < 4) {
				//do nothing
			}
			else {
				if(_unitsSold != null) {
					debug("Average Budget: " + budget);
					budget = _capacity*(2.0/5.0) - _unitsSold.getWindowSold()/4;
					if(budget < 20) {
						budget = 20;
					}
					debug("Unit Sold Model Budget "  +budget);
				}
			}

			System.out.println("Budget: "+ budget);

			Collections.sort(allIncItems);
			//			Misc.printList(allIncItems,"\n", Output.OPTIMAL);

			//			HashMap<Integer,Item> solution = fillKnapsack(allIncItems, budget);
			HashMap<Integer,Item> solution = fillKnapsackWithCapExt(allIncItems, budget);

			//set bids
			for(Query q : _querySpace) {

				Integer isID = _queryId.get(q);
				double bid;

				if(solution.containsKey(isID)) {
					bid = solution.get(isID).b();
					bid *= randDouble(.97,1.03);  //Mult by rand to avoid users learning patterns.
					//	double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
					//	double numImps = _queryToNumImpModel.getPrediction(q);
					//	int numClicks = (int) (clickPr * numImps);
					//	double CPC = _bidToCPC.getPrediction(q, bid);
					//	bidBundle.addQuery(q, bid, new Ad(), numClicks*CPC);
					if(solution.get(isID).targ()) {
						if(q.getComponent() == null && q.getManufacturer() == null) {
							bidBundle.addQuery(q, bid, new Ad(new Product(_manSpecialty,_compSpecialty)), Double.NaN);
						}
						else if(q.getComponent() == null || q.getManufacturer() == null) {
							if(q.getComponent() == null) {
								bidBundle.addQuery(q, bid, new Ad(new Product(q.getManufacturer(),_compSpecialty)), Double.NaN);
							}
							else {
								bidBundle.addQuery(q, bid, new Ad(new Product(_manSpecialty,q.getComponent())), Double.NaN);
							}
						}
					}
					else {
						bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
					}
				}
				else { 
					/*
					 * We decided that we did not want to be in this query, so we will use it to explore the space
					 */
					//					bid = 0.0;
					//					bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
					if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
						bid = randDouble(.1,.3);
					else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
						bid = randDouble(.25,.75);
					else
						bid = randDouble(.33,1.0);

					//					System.out.println("Exploring " + q + "   bid: " + bid);
					bidBundle.addQuery(q, bid, new Ad(), bid*10);
				}

				dailyCPCPredictions.put(q, _bidToCPC.getPrediction(q, bid));
				dailyClickPrPredictions.put(q, _bidToPrClick.getPrediction(q, bid, new Ad()));
				dailyConvPrPredictions.put(q, _convPrModel.getPrediction(q));
				dailyDonnieConvPrPredictions.put(q, _donnieConvPrModel.getPrediction(q));
				dailyImpPredictions.put(q,(double)_queryToNumImpModel.getPrediction(q));

			}
			((EnsembleBidToCPC) _bidToCPC).updatePredictions(bidBundle);
			((EnsembleBidToPrClick) _bidToPrClick).updatePredictions(bidBundle);
			CPCPredictions.add(dailyCPCPredictions);
			ClickPrPredictions.add(dailyClickPrPredictions);
			ConvPrPredictions.add(dailyConvPrPredictions);
			DonnieConvPrPredictions.add(dailyDonnieConvPrPredictions);
			ImpPredictions.add(dailyImpPredictions);
			/*
			 * Update model error
			 */
			if(_day > lagDays+2) {
				errorDayCounter++;
				debug(errorDayCounter);
				QueryReport queryReport = _queryReports.getLast();
				SalesReport salesReport = _salesReports.getLast();
				System.out.println("Day: " + _day);

				((EnsembleBidToCPC) _bidToCPC).updateError(queryReport, _bidBundles.get(_bidBundles.size()-2));
				((EnsembleBidToCPC) _bidToCPC).createEnsemble();

				((EnsembleBidToPrClick) _bidToPrClick).updateError(queryReport, _bidBundles.get(_bidBundles.size()-2));
				((EnsembleBidToPrClick) _bidToPrClick).createEnsemble();

				/*
				 * CPC Error
				 */
				HashMap<Query, Double> cpcpredictions = CPCPredictions.get(CPCPredictions.size()-3);
				double dailyCPCerror = 0;
				for(Query query : _querySpace) {
					if (Double.isNaN(queryReport.getCPC(query))) {
						//If CPC is NaN it means it is zero, which means our entire prediction is error!
						double error = cpcpredictions.get(query)*cpcpredictions.get(query);
						dailyCPCerror += error;
						sumCPCError += error;
					}
					else {
						double error = (queryReport.getCPC(query) - cpcpredictions.get(query))*(queryReport.getCPC(query) - cpcpredictions.get(query));
						dailyCPCerror += error;
						sumCPCError += error;
					}
				}
				double stddevCPC = Math.sqrt(sumCPCError/(errorDayCounter*16));
				System.out.println("Daily CPC Error: " + Math.sqrt(dailyCPCerror/16));
				System.out.println("CPC  Standard Deviation: " + stddevCPC);

				/*
				 * ClickPr Error
				 */
				HashMap<Query, Double> clickprpredictions = ClickPrPredictions.get(ClickPrPredictions.size()-3);
				double dailyclickprerror = 0;
				for(Query query : _querySpace) {
					double clicks = queryReport.getClicks(query);
					double imps = queryReport.getImpressions(query);
					if (clicks == 0 || imps == 0) {
						double error = clickprpredictions.get(query)*clickprpredictions.get(query);
						dailyclickprerror += error;
						sumClickPrError += error;
					}
					else {
						double error = (clicks/imps - clickprpredictions.get(query))*(clicks/imps- clickprpredictions.get(query));
						dailyclickprerror += error;
						sumClickPrError += error;
					}
				}
				double stddevClickPr = Math.sqrt(sumClickPrError/(errorDayCounter*16));
//				System.out.println("Daily Bid To ClickPr Error: " + Math.sqrt(dailyclickprerror/16));
//				System.out.println("ClickPr Bid To Standard Deviation: " + stddevClickPr);

				/*
				 * ConvPr Error
				 */
				HashMap<Query, Double> convprpredictions = ConvPrPredictions.get(ConvPrPredictions.size()-3);
				double dailyconvprerror = 0;
				int prConvDailySkip = 0;
				for(Query query : _querySpace) {
					double clicks = queryReport.getClicks(query);
					double convs = salesReport.getConversions(query);
					if(clicks == 0 || convs == 0) {
						prConvSkip++;
						prConvDailySkip++;
					}
					else {
						//						System.out.println("Predicted ConvPr: " + convprpredictions.get(query));
						//						System.out.println("Actual ConvPr: " + convs/clicks);
						double error = (convs/clicks - convprpredictions.get(query))*(convs/clicks- convprpredictions.get(query));
						dailyconvprerror += error;
						sumConvPrError += error;
					}
				}
				double stddevConvPr = Math.sqrt(sumConvPrError/(errorDayCounter*16 - prConvSkip));
//				System.out.println("Daily Bid To ConvPr Error: " + Math.sqrt(dailyconvprerror/(16-prConvDailySkip)));
//				System.out.println("ConvPr To Standard Deviation: " + stddevConvPr);
				
				/*
				 * ConvPr Error
				 */
				HashMap<Query, Double> donnieconvprpredictions = DonnieConvPrPredictions.get(ConvPrPredictions.size()-3);
				double donniedailyconvprerror = 0;
				int donnieprConvDailySkip = 0;
				for(Query query : _querySpace) {
					double clicks = queryReport.getClicks(query);
					double convs = salesReport.getConversions(query);
					if(clicks == 0 || convs == 0) {
						donnieprConvDailySkip++;
					}
					else {
						double error = (convs/clicks - donnieconvprpredictions.get(query))*(convs/clicks- donnieconvprpredictions.get(query));
						donniedailyconvprerror += error;
						sumDonnieConvPrError += error;
					}
				}
				double stddevDonnieConvPr = Math.sqrt(sumDonnieConvPrError/(errorDayCounter*16 - prConvSkip));
//				System.out.println("Daily Bid To DonniConvPr Error: " + Math.sqrt(donniedailyconvprerror/(16-donnieprConvDailySkip)));
//				System.out.println("DonnieConvPr To Standard Deviation: " + stddevDonnieConvPr);


				/*
				 * NumImps Error
				 */
				HashMap<Query, Double> numimpspredictions = ImpPredictions.get(ImpPredictions.size()-3);
				double dailyimperror = 0;
				int impDailySkip = 0;
				for(Query query : _querySpace) {
					double imps = queryReport.getImpressions(query);
					if(imps == 0) {
						impSkip++;
						impDailySkip++;
					}
					else {
						//						System.out.println("Predicted Imps: " + numimpspredictions.get(query));
						//						System.out.println("Actual Imps: " + imps);
						double error = (imps - numimpspredictions.get(query))*(imps- numimpspredictions.get(query));
						dailyimperror += error;
						sumImpError += error;
					}
				}
				double stddevImp = Math.sqrt(sumImpError/(errorDayCounter*16 - impSkip));
//				System.out.println("Daily Bid To Num Imps Error: " + Math.sqrt(dailyimperror/(16-impDailySkip)));
//				System.out.println("Num Imps To Standard Deviation: " + stddevImp);


				//				if(_day == 60) {
				//					System.out.println("ClickPr Bid To Standard Deviation: " + stddevClickPr);
				//					System.out.println("CPC  Standard Deviation: " + stddevCPC);
				//					((EnsembleBidToPrClick) _bidToPrClick).printEnsembleMemberSummary();
				//					((EnsembleBidToCPC) _bidToCPC).printEnsembleMemberSummary();
				//				}
			}

		}
		else {
			for(Query q : _querySpace){
				double bid = 0.0;
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.1,.3);
				else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.25,.75);
				else
					bid = randDouble(.33,1.0);
				bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
			}
		}
		return bidBundle;
	}


	/**
	 * Greedily fill the knapsack by selecting incremental items
	 * @param incItems
	 * @param budget
	 * @return
	 */
	private HashMap<Integer,Item> fillKnapsack(LinkedList<IncItem> incItems, double budget){
		HashMap<Integer,Item> solution = new HashMap<Integer, Item>();
		for(IncItem ii: incItems) {
			//lower efficiencies correspond to heavier items, i.e. heavier items from the same item
			//set replace lighter items as we want
			if(budget >= 0) {
				//				debug("adding item " + ii);
				solution.put(ii.item().isID(), ii.item());
				budget -= ii.w();
			}
			else {
				break;
			}
		}
		return solution;
	}

	private HashMap<Integer,Item> fillKnapsackWithCapExt(LinkedList<IncItem> incItems, double budget){
		HashMap<Integer,Item> solution = new HashMap<Integer, Item>();
		LinkedList<IncItem> temp = new LinkedList<IncItem>();

		boolean incremented = false;
		double valueLost = 0;
		double valueGained = 0;
		int knapSackIter = 0;

		for(IncItem ii: incItems) {
			//lower efficiencies correspond to heavier items, i.e. heavier items from the same item
			//set replace lighter items as we want
			//			if(budget >= ii.w()) {
			if(budget >= 0) {
				if (incremented) {
					temp.addLast(ii);
					budget -= ii.w();
					debug("Temporarily adding: " + ii);
					valueGained += ii.v(); //amount gained as a result of extending capacity
				}
				else {
					//					System.out.println("adding item" + ii.item());
					solution.put(ii.item().isID(), ii.item());
					budget -= ii.w();
				}
			}
			else{
				if (incremented) {
					if (valueGained >= valueLost) { //checks to see if it was worth extending our capacity
						while (!temp.isEmpty()){
							IncItem inc = temp.poll();
							//							System.out.println("adding item over capacity " + inc.item());
							solution.put(inc.item().isID(), inc.item());
						}
						valueLost = 0;
						valueGained = 0;
					}
					else {
						debug("Not worth overselling anymore");
						break;
					}
				}

				double avgConvProb = 0; //the average probability of conversion;
				if(MODELCONVPR) {
					for(Query q : _querySpace) {
						avgConvProb += _convPrModel.getPrediction(q) * _salesDist.getPrediction(q);
					}
				}
				else {
					for(Query q : _querySpace) {
						avgConvProb += _baseConvProbs.get(q) * _salesDist.getPrediction(q);
					}
				}

				double avgUSP = 0;
				for(Query q : _querySpace) {
					avgUSP += _salesPrices.get(q) * _salesDist.getPrediction(q);
				}

				for (int i = _capacityInc*knapSackIter+1; i <= _capacityInc*(knapSackIter+1); i++){
					double iD = Math.pow(LAMBDA, i);
					double worseConvProb = avgConvProb*iD; //this is a gross average that lacks detail
					valueLost += (avgConvProb - worseConvProb)*avgUSP*5; //You also lose conversions in the future (for 5 days)
					debug("Adding " + ((avgConvProb - worseConvProb)*avgUSP*5) + " to value lost");
				}
				debug("Total value lost: " + valueLost);
				budget+=_capacityInc;
				incremented = true;
				knapSackIter++;
			}
		}
		return solution;
	}

	/**
	 * Get undominated items
	 * @param items
	 * @return
	 */
	public static Item[] getUndominated(Item[] items) {
		Arrays.sort(items,new ItemComparatorByWeight());
		//remove dominated items (higher weight, lower value)		
		LinkedList<Item> temp = new LinkedList<Item>();
		temp.add(items[0]);
		for(int i=1; i<items.length; i++) {
			Item lastUndominated = temp.get(temp.size()-1); 
			if(lastUndominated.v() < items[i].v()) {
				temp.add(items[i]);
			}
		}

		//items now contain only undominated items
		items = temp.toArray(new Item[0]);

		//remove lp-dominated items
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(new Query(),0,0,-1,false,1));//add item with zero weight and value

		for(int i=0; i<items.length; i++) {
			q.add(items[i]);//has at least 2 items now
			int l = q.size()-1;
			Item li = q.get(l);//last item
			Item nli = q.get(l-1);//next to last
			if(li.w() == nli.w()) {
				if(li.v() > nli.v()) {
					q.remove(l-1);
				}else{
					q.remove(l);
				}
			}
			l = q.size()-1; //reset in case an item was removed
			//while there are at least three elements and ...
			while(l > 1 && (q.get(l-1).v() - q.get(l-2).v())/(q.get(l-1).w() - q.get(l-2).w()) 
					<= (q.get(l).v() - q.get(l-1).v())/(q.get(l).w() - q.get(l-1).w())) {
				q.remove(l-1);
				l--;
			}
		}

		//remove the (0,0) item
		if(q.get(0).w() == 0 && q.get(0).v() == 0) {
			q.remove(0);
		}

		Item[] uItems = (Item[]) q.toArray(new Item[0]);
		return uItems;
	}


	/**
	 * Get incremental items
	 * @param items
	 * @return
	 */
	public IncItem[] getIncremental(Item[] items) {
		debug("PRE INCREMENTAL");
		for(int i = 0; i < items.length; i++) {
			debug("\t" + items[i]);
		}

		Item[] uItems = getUndominated(items);

		debug("UNDOMINATED");
		for(int i = 0; i < uItems.length; i++) {
			debug("\t" + uItems[i]);
		}

		IncItem[] ii = new IncItem[uItems.length];

		if (uItems.length != 0){ //getUndominated can return an empty array
			ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0]);
			for(int item=1; item<uItems.length; item++) {
				Item prev = uItems[item-1];
				Item cur = uItems[item];
				ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur);
			}
		}
		debug("INCREMENTAL");
		for(int i = 0; i < ii.length; i++) {
			debug("\t" + ii[i]);
		}
		return ii;
	}

	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

	public void debug(Object str) {
		if(DEBUG) {
			System.out.println(str);
		}
	}

}
