package agents;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.BasicBidToPrClick;
import newmodels.bidtoprconv.AbstractBidToPrConv;
import newmodels.bidtoprconv.TrinaryPrConversion;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.bidtoslot.ReallyBadBidToSlot;
import newmodels.bidtoslot.WrapperIlkeBid2Slot;
import newmodels.slottocpc.AbstractSlotToCPCModel;
import newmodels.slottocpc.LinearSlotToCPC;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.slottoprclick.DetBasicSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import simulator.models.PerfectBidToCPC;
import simulator.models.PerfectBidToPosition;
import simulator.models.PerfectBidToPrConv;
import usermodel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class ILPAgent extends SimAbstractAgent{

	// ########################
	// #### Game Decisions ####
	// ########################

	final static boolean USEOVERQUANTITY = true; 	// Do you want the MIP formula to include over capacity?
	final static int DAYSUNTILILKE = 70; 			// How many days until the Bid-Slot bucket model starts to work?
	protected static int NUMOFUSERS = 90000;		// How many users are simulated?
	double _regReserveScore = 0.2;					// I want the bids to be at least the regular reserve score
	
	
	// ###################
	// #### Variables ####
	// ###################
	/// Data from server 
	protected static int NUMOFQUERIES;		// the size of the query space
	protected static int DAILYCAPACITY;		// the daily capacity
	protected static double DISCOUNTER;		// the discounter for the over capacity
	
	/// Agent variables
	protected Set<Double> _possibleBids;
	protected Set<Integer> _possibleQuantities;	// a vector of all the quantities that we are willing to over sell 
	protected static Map <Product , Double> _productRevenue;	// the revenue from every product, including component specialty
	protected HashMap<Query, Double> _bids;					// a set of our decision for bidding on every query. 0 if no bidding
	protected HashMap<Query, Double> _dailyLimit;		// daily limit per query
	protected HashMap<Integer, Query> _queryIndexing;	// mapping queries into integers - it help with the cplex variable array
	protected Set<UserState> searchingUserStates;
	protected int _numSearchingUsers = 0;
	protected HashMap<Query, HashMap<String,Double>> _previousDayData;
	private int _wantedSales;	// how much we want to sell in a specific day
	FileWriter _fstream;
	BufferedWriter _fout;
	
	/// Models Needed
	private HashMap<Query, AbstractBidToPrClick> _bidToClickPrModels; 
	private HashMap<Query, AbstractBidToCPC> _bidToCPCModels;
	private HashMap<Product, HashMap<UserState, AbstractBidToPrConv>> _convPrModel;	// probability of a conversion
	private AbstractUserModel _userModel;	// the number of users in every state
	/// Models extra used
	private HashMap<Query, AbstractBidToSlotModel> _bidToSlotModels_simple;	// I've made two models for bidding, one is simple
	private HashMap<Query, AbstractBidToSlotModel> _bidToSlotModels_ilke;	// and the second takes some days until it is good enough
	private HashMap<Query, AbstractSlotToPrClick> _slotToClickPrModels;
	private HashMap<Query, AbstractSlotToCPCModel> _slotToCPCModels;	// a simple CPC model, but later the agent is using the better slot2bid
	
	
	// ###################
	// #### The Agent ####
	// ###################

	public ILPAgent(){
		System.out.println("Let's start");
		_productRevenue = new HashMap<Product, Double>();
		_bids = new HashMap<Query, Double>();
		_dailyLimit = new HashMap<Query, Double>();
		_queryIndexing = new HashMap<Integer, Query>();
		_previousDayData = new HashMap<Query, HashMap<String,Double>>();

		searchingUserStates = new HashSet<UserState>();
		searchingUserStates.add(UserState.F0);
		searchingUserStates.add(UserState.F1);
		searchingUserStates.add(UserState.F2);
		searchingUserStates.add(UserState.IS);

		try {
			_fstream = new FileWriter("SalesSlotsOut.txt");
			_fout = new BufferedWriter(_fstream);
		}
		catch (Exception e) {
			System.err.println ("problem with opening file - " + e.getMessage());
		}
	}

	@Override
	public void initBidder() {
		System.out.println("Initilizing Bids");
		
		DAILYCAPACITY = _advertiserInfo.getDistributionCapacity() / _advertiserInfo.getDistributionWindow();
		DISCOUNTER = _advertiserInfo.getDistributionCapacityDiscounter();
		NUMOFQUERIES = _querySpace.size();
		
		for (Product p : _retailCatalog) {
			int givesBonus=0;
			if (p.getManufacturer().equals(_advertiserInfo.getManufacturerSpecialty())) givesBonus = 1;
			double profit = _retailCatalog.getSalesProfit(p)*(1+_advertiserInfo.getComponentBonus()*givesBonus);
			System.out.println("product " + p.getComponent() + " " + p.getManufacturer() + " with profit " + profit);
			_productRevenue.put(p, profit);
		}

		int qvalue = 0;
		for(Query query : _querySpace){	// just some reasonable numbers to start with 
			double bid = 0;
//			if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
//				bid = .5;
//			else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE))
//				bid = 0.8;
//			else 
//				bid = 1.2;
			_bids.put(query, bid);
			
			_queryIndexing.put(qvalue, query);
			qvalue++;
			
			_dailyLimit.put(query, Double.MAX_VALUE);
		}

		_possibleBids = new HashSet<Double>(setPossibleBids(0.1, 3.5, .1));
		_possibleQuantities = new HashSet<Integer>(setPossibleQuantities(0, 10, 1));
	}
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {		
		if (_day > 1) {
			try {
				buildMaps(models);
				updateBids();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		BidBundle bb = new BidBundle();
		for (Query query:_querySpace) {
			bb.addQuery(query, _bids.get(query), new Ad()); //TODO add a daily limit
		}
		return bb;
		
	}
	
	
	// ###############
	// #### CPLEX ####
	// ###############
	/**
	 * Creates the bids using the ILP calculation
	 * @throws IOException 
	 */
	public void updateBids() throws IOException {
		
		int missingSales = _wantedSales;
		int qsize = _possibleQuantities.size();
		Integer[] quantitiesSet = new Integer[qsize];
		quantitiesSet = _possibleQuantities.toArray(quantitiesSet).clone();
		int bsize = _possibleBids.size();
		Double[] bidsSet = new Double[bsize];
		bidsSet = _possibleBids.toArray(bidsSet).clone();
		
//		try {
//			for (Query query : _previousDayData.keySet()) {
//				HashMap<String,Double> lastDay = _previousDayData.get(query);
//				_fout.write ("\n" + query.toString());
//				_fout.write ("\n\t Bid :: offered=" + cutDouble(lastDay.get("Bid")));
//				_fout.write ("\n\t Position :: wanted=" + cutDouble(lastDay.get("Position")) + " got=" + cutDouble(_queryReport.getPosition(query)));
//				_fout.write ("\n\t Sales :: wanted=" + cutDouble(lastDay.get("Sales")) + " got=" + cutDouble(_salesReport.getConversions(query)));
//				missingSales -= _salesReport.getConversions(query);
//			}
//			_fout.write ("\n\nDay:" + _day);
//			_fout.flush();
//		}
//		catch (Exception e) {
//			System.err.println("problem with writing to file - " + e.getMessage());
//		}
		
//		System.out.println("PROBABILITY OF A CONVERSION");
//		for (Product p : _retailCatalog) {
//			System.out.print(p.toString() + " ## ");
//			for (UserState us : UserState.values()) {
//				for (int i=0 ; i<31 ; i=i+10) {
//					System.out.print(us.toString() + "->" + get_convPr(p, us, i) + "\t");
//				}
//				System.out.println();
//			}
//		}
//		
//		System.out.println("PROBABILITY OF A CLICK");
//		for (Query q : _querySpace) {
//			System.out.print (q.toString() + " ## ");
//			for (int slot=1 ; slot<=_numSlots ; slot++ ) {
//				System.out.print(slot + "->" + _slotToPrClickModels.get(q).getPrediction(slot) + "\t");				
//			}
//			System.out.println();
//		}

		try {
			IloCplex cplex = new IloCplex();
			IloIntVar[] overCapVar = cplex.boolVarArray(qsize);
			IloIntVar[][] bidsVar = new IloIntVar[NUMOFQUERIES][bsize];
			IloLinearNumExpr exprObj = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				bidsVar[query] = cplex.boolVarArray(bsize);

//				System.out.print(_queryIndexing.get(query).toString() + " ## ");
				for (int bidIndex=0 ; bidIndex<bsize ; bidIndex++) {
					exprObj.addTerm(bidsVar[query][bidIndex], getObjBidCoef(bidsSet[bidIndex] , _queryIndexing.get(query)));
//					System.out.print(bidIndex + "->" + getObjBidCoef(bidsSet[bidIndex] , _queryIndexing.get(query)) + "\t");
				}
//				System.out.println();
			}
			if (USEOVERQUANTITY) for (int quantity=0 ; quantity < qsize ; quantity++) {
				exprObj.addTerm(overCapVar[quantity], -getObjOverQuantityCoef(quantitiesSet[quantity]));				
			}		
			cplex.addMaximize(exprObj);

			IloLinearNumExpr exprCapacity = cplex.linearNumExpr();
			IloLinearNumExpr exprOverCapVarLE = cplex.linearNumExpr();
			IloLinearNumExpr exprBidsVarLE = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				for (int bidIndex=0 ; bidIndex<bsize; bidIndex++) {
					exprCapacity.addTerm(bidsVar[query][bidIndex] , getQuantityBoundBidCoef(bidsSet[bidIndex] , _queryIndexing.get(query)));
					exprBidsVarLE.addTerm(1.0, bidsVar[query][bidIndex]);
				}
				cplex.addLe(exprBidsVarLE, 1);
				exprBidsVarLE = cplex.linearNumExpr();
			}
			if (USEOVERQUANTITY) {
				for (int quantity=0 ; quantity<qsize ; quantity++) {
					exprCapacity.addTerm(overCapVar[quantity] , -quantitiesSet[quantity]);				
					exprOverCapVarLE.addTerm(1.0, overCapVar[quantity]);
				}
				cplex.addLe(exprOverCapVarLE, 1);
			}
			_wantedSales = DAILYCAPACITY + missingSales;
			cplex.addLe(exprCapacity, _wantedSales);	// the capacity sold minus the over capacity (=epxrCapacity) need to be less than the daily capacity
			_fout.write(exprCapacity.toString());
			_fout.write("\n We want to sell " + _wantedSales + "\n");
			cplex.setOut(null);
			if (cplex.solve()) {
//				cplex.output().println("Solution status = " + cplex.getStatus());
//				cplex.output().println("Solution value = " + cplex.getObjValue());
				
				_previousDayData = new HashMap<Query, HashMap<String,Double>>();
				for (int queryIndex=0 ; queryIndex<NUMOFQUERIES ; queryIndex++) {
					HashMap<String,Double> expectedData = new HashMap<String, Double>();
					Query query = _queryIndexing.get(queryIndex);
					double[] queryResults = cplex.getValues(bidsVar[queryIndex]);
					boolean bidThisQuery = false;
//					System.out.print(_queryIndexing.get(query).toString());
					for (int bidIndex=0 ; bidIndex<queryResults.length ; bidIndex++) {
						if (queryResults[bidIndex] == 1.0) {
//							double simpleBid = (_numSlots - bidIndex + 1) * 0.71 + 0.71;	// if there is no good bid, use this
//							double bid;	// the final bid for this slot
//							if (_day < DAYSUNTILILKE) {
//								bid = simpleBid;	// if we don't have enough data, use a simple bid decision
//							} else {
//								bid = _slotToBidModels_ilke.get(_queryIndexing.get(query)).getPrediction(bidIndex);
//								if ((Double.isInfinite(bid) || Double.isNaN(bid)) || (bid == 0.0)) bid = simpleBid;	// if there is no good result from the models, use a simple decision
//							}
							bidThisQuery = true;
							_bids.put(query , bidsSet[bidIndex]); 
							_fout.write (query.toString() + "\t" + "Bid=" + bidsSet[bidIndex] + " ObjCoef=" + getObjBidCoef(bidsSet[bidIndex], query) + " QCoef=" + getQuantityBoundBidCoef(bidsSet[bidIndex], query) + "\n");
							_fout.flush();
							expectedData.put("Bid", bidsSet[bidIndex]);
							expectedData.put("Sales", getQuantityBoundBidCoef(bidsSet[bidIndex], query));
							AbstractBidToSlotModel temp = _bidToSlotModels_simple.get(query);
							double temp_predict = temp.getPrediction(bidsSet[bidIndex]);
							expectedData.put("Position", temp_predict);
//							_fout.write ("\nPosition used the class " + temp.getClass());
						}
					}
					if (!bidThisQuery) _bids.put(_queryIndexing.get(queryIndex), 0.0);
					else {
						_previousDayData.put(query, expectedData);
					}
				}
				if (USEOVERQUANTITY) {
					double[] quantityResults = cplex.getValues(overCapVar);
					for (int quantity=0 ; quantity<quantityResults.length ; quantity++) {
//						if (quantityResults[quantity] == 1.0) System.out.println("OverQuantity=" + quantitiesSet[quantity] + " ObjCoef=" + getObjOverQuantityCoef(quantity));
					}
				}
			}
			//System.out.println(cplex.toString());
			cplex.end();
		}
		catch (IloException e) {
			System.err.println ("Concert Exception" + e + "' caught");
		}
//		catch (Exception e) {
//			System.err.println("problem with writing to file - " + e.getMessage());
//		}
		for (Query q : _querySpace) {
//			_bids.put(q, 3.0);
		}
	}
	
	/**
	 * return the sum of n*Pr(p,x)*Pr(click|q,p,x,s(b))(pi(p,x)*r-cpc(b))
	 * @param bid
	 * @param query
	 * @return a double representing the sum
	 */
	public double getObjBidCoef(double bid , Query query) {		
		
		double result = 0;
		
		for (UserState us : setOfUserStates(query)) {				
			for (Product p : setOfProducts(query, us)) {
				result += estimateImpressionOutOfTotalSearchingUsers(p,us) * estimateClicks(p,us,query,bid) * (estimateConv(p,us,0)*_productRevenue.get(p) - estimateCPC(query, bid)); 
				if (Double.isNaN(result) || Double.isInfinite(result)) {	// if there is a problem, beep and show me where
					beep();
//					System.out.print("\n The is a null result: \n bid2cpc=" + estimateCPC(query, bid) + "\n clickPr=" + estimateClicks(p,us,query,slot) + "\n get_convPr=" + estimateConv(p,us,0) + "\n p_x_Pr" + estimateIn + "\n");
					System.out.print("\007");
				}
			}
		}
		return result;
	}
		
	/**
	 * returns the sum of n*Pr(p,x)*Pr(click|q,p,x,s(b)*pi(p,x)
	 * @param bid
	 * @param query
	 * @return a double representing the sum
	 */
	public double getQuantityBoundBidCoef(double bid, Query query) {
		
		double result = 0;
		
		for (UserState us : setOfUserStates(query)) {
			for (Product p : setOfProducts(query, us)) {
				double p_x_Pr = estimateImpressionOutOfTotalSearchingUsers(p, us); // TODO find a model
				double p_x_convPr = estimateConv(p,us,0);
				double click_Pr_q_p_x_s = estimateClicks(p, us, query, bid);
				
				result += p_x_Pr * click_Pr_q_p_x_s * p_x_convPr;
			}
		}
		
		return result;
	}

	/**
	 * the sum of all the products and user states for a given amount of over-capacity
	 * @param quantity (over capacity)
	 * @return a double representing the sum
	 */
	public double getObjOverQuantityCoef(int quantity) {
		
		double result=0;
				
		for (int i=1 ; i <= quantity ; i++) {
			for (Product p : _retailCatalog) {
				double revenue = _productRevenue.get(p); 
				for (UserState us : UserState.values()) {//setOfUserStates(bestQuery)) {
					double p_x_convPr = estimateConv(p,us,0);
					double p_x_Pr = estimateImpressionOutOfTotalSearchingUsers(p,us);
					double p_x_i_convPr = estimateConv(p,us,i);

					result += p_x_Pr * (p_x_convPr - p_x_i_convPr) * revenue;
				}
			}
		}
		
		return result;
	}

	/**
	 * calculates the number of clicks times the CPC for the slot, to set a daily limit - n*Pr(p,x)*Pr(click)*CPC(slot
	 * @param slot
	 * @param query
	 * @return a double representing the daily limit
	 */
	public double getQueryDailySpendLimit(double bid, Query query) {
		
		double result = 0;
		
		double bid2cpc = _bidToCPCModels.get(query).getPrediction(bid);
		for (UserState us : setOfUserStates(query)) {
			for (Product p : setOfProducts(query, us)) {
				double p_x_Pr = estimateImpressionOutOfTotalSearchingUsers(p, us);
				double click_Pr_q_p_x_s = estimateClicks(p, us, query, bid);
				
				result += p_x_Pr * click_Pr_q_p_x_s;
			}
		}
		
		return result * bid2cpc;
	}
	
	
	// #####################
	// #### Estimations ####  if someone wants a better estimation, just extends this class, and rewrite them. But the MIP calculation suppose to stay the same.
	// #####################

	/**
	 * returns the fraction of impressions out of all the  users, by a user in state {IS,F0,F1,F2} with the given product preference
	 * @param product
	 * @param userState
	 * @return number of impressions
	 */
	public double estimateImpressionOutOfTotalSearchingUsers (Product product, UserState userState) {
		double result = 0;

		result = _userModel.getPrediction(product, userState);

		return result;
	}
	
	/**
	 * click probability
	 * @param p - product preference of the us user
	 * @param us - user state
	 * @param query - for which query 
	 * @param slotOrBid - double number, for doing a bid->click or slot->click
	 * @return percent of users that will click after an impression
	 */
	public double estimateClicks (Product p, UserState us, Query query, double bid) {

		double result = 0;
		
		//result = _userModel.getPrediction(p, us) / _numSearchingUsers;	// and estimate of the percent of this user in the query		
		result = _bidToClickPrModels.get(query).getPrediction(bid);

		return result;
	}
	
	/**
	 * bid to cpc
	 * @param query
	 * @param bid
	 * @return
	 */
	public double estimateCPC (Query query, double bid) {
//		double slot2cpc = _slotToCPC.get(query).getPrediction(slot);
		double bid2slot = 0;
		if (_day < DAYSUNTILILKE) bid2slot = _bidToSlotModels_simple.get(query).getPrediction(bid);
		else bid2slot = _bidToSlotModels_ilke.get(query).getPrediction(bid);
		bid2slot = Math.min(_numSlots,Math.max(bid2slot, 1));
		double result = _slotToCPCModels.get(query).getPrediction(bid2slot);
		if (_bidToCPCModels.get(query) instanceof PerfectBidToCPC) {
			result = _bidToCPCModels.get(query).getPrediction(bid);
		}
		if (Double.isNaN(result) || Double.isInfinite(result) || Double.isInfinite(-result) || Double.valueOf(result).equals(0.0)) result = _regReserveScore+0.01; 
		return result;
	}
	
	/**
	 * estimated conversion probability for each product and user state. It is NOT for each query, but for every user on the agent's "website" 
	 * @param p
	 * @param us
	 * @param i
	 * @return conversion probability
	 */
	private double estimateConv(Product p, UserState us, int i) {
		double result = 0;
		
		if (us.equals(UserState.NS) || us.equals(UserState.T) || us.equals(UserState.IS)) return result;

		AbstractBidToPrConv convPr = _convPrModel.get(p).get(us);
		if (convPr.equals(null) || p.equals(null) || us.equals(null) || i<(Integer)(_possibleQuantities.toArray())[0] || (i>(Integer)(_possibleQuantities.toArray()[_possibleQuantities.size()-1]))) {
			beep();
			System.out.print("\n CONV PROBLEM--> " + p.getComponent() + p.getManufacturer() + us.toString() + i + "\n");
		}
		try {
			result = convPr.getPrediction(i);
			if (convPr instanceof PerfectBidToPrConv) {
				if (p.getComponent().equals(_compSpecialty)) result = unEta(result,_CSB);
				result = result;
				if (p.getComponent().equals(_compSpecialty)) result = eta(result,_CSB);
/*				int numImps = _numImpModel.getPrediction(_query);
				double ISUserDiscount = 1 - numISUsers/numImps;
				double capDiscount = Math.pow(LAMBDA ,amountOverCap);
				double convRate = baselineconv*capDiscount*ISUserDiscount;*/	
			}
		}
		catch (Exception e){
			System.err.print("[" + p + "#" + us + "#" + i);
		}
		return result;
	}

//	private double get_p_x_Pr(Product p, UserState us) {
//		double result = 0;
//		if (us.equals(UserState.IS) || us.equals(UserState.F0)) result = _userModel.getPrediction(us)/_retailCatalog.size();
//		else if (us.equals(UserState.F1)) {
//			result = _userModel.getPrediction(us)/6; // TODO 6 is a constant. no good.
//		} else if (us.equals(UserState.F2)) {
//			result = _userModel.getPrediction(us);			
//		}
//		return result;
//	}
		
	
	// #########################
	// #### Models Updating ####
	// #########################
	
	@Override
	public Set<AbstractModel> initModels() {
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		_bidToSlotModels_simple = new HashMap<Query, AbstractBidToSlotModel>();
		_bidToSlotModels_ilke = new HashMap<Query, AbstractBidToSlotModel>();
		_bidToCPCModels = new HashMap<Query, AbstractBidToCPC>();
		_slotToClickPrModels = new HashMap<Query, AbstractSlotToPrClick>();
		_convPrModel = new HashMap<Product,HashMap<UserState, AbstractBidToPrConv>>();
		_slotToCPCModels = new HashMap<Query, AbstractSlotToCPCModel>();
		_bidToClickPrModels = new HashMap<Query, AbstractBidToPrClick>();

		AbstractUserModel userModel = new BasicUserModel();
		models.add(userModel);
		_userModel = userModel;
		
		for(Query query: _querySpace) {

//			AbstractSlotToBidModel slotToBid = new ReallyBadSlotToBid(query);
//			AbstractSlotToBidModel slotToBid = new LinearSlotToBid(query,_numSlots);

			AbstractBidToSlotModel bidToSlot = new ReallyBadBidToSlot(query);
			_bidToSlotModels_simple.put(query,bidToSlot);
			models.add(bidToSlot);
			bidToSlot = new WrapperIlkeBid2Slot(query,_numSlots);
			_bidToSlotModels_ilke.put(query,bidToSlot);
			models.add(bidToSlot);
			
//			AbstractBidToCPC bidToCPC = new AbstractBidToCPC();
//			models.add(bidToCPC);									//TODO I don't have a model for this yet
//			_bidToCPCModels.put(query,bidToCPC);

			AbstractBidToPrClick bidToPrClick = new BasicBidToPrClick(query);
			models.add(bidToPrClick);
			_bidToClickPrModels.put(query,bidToPrClick);

			AbstractSlotToPrClick slotToPrClick = new DetBasicSlotToPrClick(query);
			models.add(slotToPrClick);
			_slotToClickPrModels.put(query,slotToPrClick);
			
			AbstractSlotToCPCModel slotToCPC = new LinearSlotToCPC(query, _numSlots);
			models.add(slotToCPC);
			_slotToCPCModels.put(query, slotToCPC);			
		}
		_numSearchingUsers = 0;
		for (Product p : _retailCatalog) {
			HashMap<UserState, AbstractBidToPrConv> temp = new HashMap<UserState, AbstractBidToPrConv>();
			for (UserState us : searchingUserStates) {
				AbstractBidToPrConv convModel = new TrinaryPrConversion(p, us, _lambda, _compSpecialty, _advertiserInfo.getComponentBonus());
				models.add(convModel);
				temp.put(us, convModel);

				_numSearchingUsers += _userModel.getPrediction(p, us); // I want to always count how many users are in a searching state. it help the impressions estimator later.
			}
			_convPrModel.put(p, temp);
		}
		
		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport,
			QueryReport queryReport) {

		for(AbstractModel model:_models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				userModel.updateModel(queryReport, salesReport);

				_numSearchingUsers = 0;
				for (Product p : _retailCatalog) {
					for (UserState us : searchingUserStates) {
						_numSearchingUsers += _userModel.getPrediction(p, us); // I want to always count how many users are in a searching state. it help the impressions estimator later.
					}
				}
			}
			else if(model instanceof AbstractBidToSlotModel) {
				AbstractBidToSlotModel bidToSlot = (AbstractBidToSlotModel) model;
				bidToSlot.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				bidToCPC.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				bidToPrClick.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				slotToPrClick.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToCPCModel) {
				AbstractSlotToCPCModel slotToCPC = (AbstractSlotToCPCModel) model;
				slotToCPC.updateModel(queryReport, salesReport, _bids);
			}
			else if(model instanceof AbstractBidToPrConv) {
				AbstractBidToPrConv convPr = (AbstractBidToPrConv) model;
				convPr.updateModel(queryReport, salesReport);
			}
		}
	}
	
	protected void buildMaps(Set<AbstractModel> models) throws Exception {
		_bidToClickPrModels = new HashMap<Query, AbstractBidToPrClick>(); 
		_bidToCPCModels = new HashMap<Query, AbstractBidToCPC>();
//		_convPrModel = new HashMap<Product, HashMap<UserState,AbstractPrConversionModel>>();	// probability of a conversion
		_bidToSlotModels_simple = new HashMap<Query, AbstractBidToSlotModel>();	// I've made two models for bidding, one is simple
		_bidToSlotModels_ilke = new HashMap<Query, AbstractBidToSlotModel>();	// and the second takes some days until it is good enough
		_slotToClickPrModels = new HashMap<Query, AbstractSlotToPrClick>();
//		_slotToCPCModels = new HashMap<Query, AbstractSlotToCPCModel>();
		
//		for (Query q : _querySpace) {
//			System.out.println ("bidding 3 in the last day on query" + q.toString() + " would have gave you a slot of " + _queryReport.getPosition(q));
//		}

		for(AbstractModel model : models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				_userModel = userModel;

				_numSearchingUsers = 0;
				for (Product p : _retailCatalog) {
					for (UserState us : searchingUserStates) {
						_numSearchingUsers += _userModel.getPrediction(p, us); // I want to always count how many users are in a searching state. it help the impressions estimator later.
					}
				}
			}
			else if(model instanceof AbstractBidToSlotModel) {
				AbstractBidToSlotModel bidToSlot = (AbstractBidToSlotModel) model;
				if (model instanceof PerfectBidToPosition) {
					_bidToSlotModels_simple.put(bidToSlot.getQuery(), bidToSlot);
					_bidToSlotModels_ilke.put(bidToSlot.getQuery(), bidToSlot);
					_fout.write ("\n" + bidToSlot.getQuery() + "updating perfect bid->slot " + bidToSlot.getClass());
				} else {
					if (bidToSlot instanceof ReallyBadBidToSlot) _bidToSlotModels_simple.put(bidToSlot.getQuery(), bidToSlot);
					else _bidToSlotModels_ilke.put(bidToSlot.getQuery(), bidToSlot);
					_fout.write("\n" + bidToSlot.getQuery() + "updating regular bid->slot " + bidToSlot.getClass());
				}
//				System.out.println(bidToSlot.getQuery() + " with bid of 3 gives slot of " + bidToSlot.getPrediction(3));
			}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				_bidToCPCModels.put(bidToCPC.getQuery(), bidToCPC);
				_fout.write("\n" + bidToCPC.getQuery() + "updating bid->cpc " + bidToCPC.getClass());
			}
			else if(model instanceof AbstractBidToPrClick) {
				AbstractBidToPrClick bidToPrClick = (AbstractBidToPrClick) model;
				_bidToClickPrModels.put(bidToPrClick.getQuery(), bidToPrClick);
				_fout.write("\n" + bidToPrClick.getQuery() + "updating bid->click " + bidToPrClick.getClass());
		}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				_slotToClickPrModels.put(slotToPrClick.getQuery(), slotToPrClick);
				_fout.write("\n" + slotToPrClick.getQuery() + "updating slot->click " + slotToPrClick.getClass());
			}
			else if(model instanceof AbstractSlotToCPCModel) {
				AbstractSlotToCPCModel slotToCPC = (AbstractSlotToCPCModel) model;
				_slotToCPCModels.put(slotToCPC.getQuery(), slotToCPC);
				_fout.write("\n" + slotToCPC.getQuery() + "updating bid->click " + slotToCPC.getClass());
			}
			else if(model instanceof AbstractBidToPrConv) {
				AbstractBidToPrConv convPr = (AbstractBidToPrConv) model;
				if (convPr instanceof PerfectBidToPrConv) {
//					_fout.write("\n" + convPr.getQuery() + "updating perfect user/product->conversion " + convPr.getClass());
//					UserState userState = getStateFromQuery(convPr.getQuery());
//					for (Product product : setOfProducts(convPr.getQuery(), userState)) {
//						HashMap<UserState, AbstractPrConversionModel> temp = new HashMap<UserState, AbstractPrConversionModel>();
//						if (_convPrModel.containsKey(product)) {
//							temp.putAll((_convPrModel.get(product)));
//						}
//						temp.put(userState, convPr);
//						_convPrModel.put(product, temp);
//					}
				} else {
					_fout.write("\n" + convPr.getQuery() + "updating regular user/product->conversion " + convPr.getClass());
					Product product = convPr.getPair().getFirst();
					UserState userState = convPr.getPair().getSecond();					
					HashMap<UserState, AbstractBidToPrConv> temp = new HashMap<UserState, AbstractBidToPrConv>();
					temp.putAll((_convPrModel.get(product)));
					temp.put(userState, convPr);
					_convPrModel.put(product, temp);
				}
			}
		}
	}

	
	// ##########################
	// #### Helper Functions ####
	// ##########################
	public Set<Double> setPossibleBids (double minBid, double maxBid, double interval) {
		Set<Double> bids = new HashSet<Double>();
		if ((minBid < 0) || (maxBid <= minBid)) return bids;
		double bid = minBid;
		while (bid < maxBid) {
			bids.add(bid);
			bid += interval;
		}
		return bids;
	}

	public Set<Integer> setPossibleQuantities(int minQ, int maxQ, int interval) {
		Set<Integer> quantity = new HashSet<Integer>();
		if ((minQ < 0) || (maxQ <= minQ)) return quantity;
		int q = minQ;
		while (q <= maxQ) {
			quantity.add(q);
			q += interval;
		}
		return quantity;
	}

	protected Set<UserState> setOfUserStates(Query query) {
		Set<UserState> us = new HashSet<UserState>();
		us.add(UserState.IS);
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) us.add(UserState.F0); 
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) us.add(UserState.F1);
		if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) us.add(UserState.F2);
		return us;
	}

	protected Set<Product> setOfProducts(Query query, UserState us) {
		Set<Product> products = new HashSet<Product>();
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO) || us.equals(UserState.IS)) {
			for (Product p : _retailCatalog) {
				products.add(p);
			}
		} else if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
			products.add(new Product(query.getManufacturer(),query.getComponent()));
		} else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) {
			if (query.getManufacturer() == null) {
				for (Product p : _retailCatalog) {
					if (p.getComponent().equals(query.getComponent())) products.add(p);
				}
			} else {
				for (Product p : _retailCatalog) {
					if (p.getManufacturer().equals(query.getManufacturer())) products.add(p);
				}
			}				
		}
		return products;
	}
	
	private UserState getStateFromQuery(Query q) {
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) return UserState.F0; 
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) return UserState.F1; 
		if (q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) return UserState.F2;
		return null;
	}
	
	private Query QueryFromUserAndProduct(Product p, UserState us) {
		Query result = new Query();
		for (Query q : _querySpace) {
			if (getStateFromQuery(q).equals(us)) {
				if (p.getComponent().equals(q.getComponent()) && p.getManufacturer().equals(q.getManufacturer())) {
					result = q;
				}
			}
		}
		return result;
	}
	
//	private Object[] ProductAndUserFromQuery (Query q) {
//		Object[] o = new Object[2];
//		for (Product p : _retailCatalog) {
//										
//		}
//		o[1] = getStateFromQuery(q);
//		return o;
//	}
	
	public double unEta(double result, double x) {
		return (result) / (x-(result*(x-1)));
	}
	
	protected void beep() {
		Toolkit.getDefaultToolkit().beep();
	}
	
	protected double cutDouble (double d) {
		if (Double.isNaN(d)) return d;
		int result = (int)(d*100);
		return (((double)result)/100);
	}
}