package agents;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.bidtoslot.ReallyBadBidToSlot;
import newmodels.bidtoslot.WrapperIlkeBid2Slot;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.TrinaryPrConversion;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottobid.LinearSlotToBid;
import newmodels.slottobid.ReallyBadSlotToBid;
import newmodels.slottobid.WrapperIlkePos2Bid;
import newmodels.slottocpc.AbstractSlotToCPCModel;
import newmodels.slottocpc.LinearSlotToCPC;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.slottoprclick.DetBasicSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;

import simulator.models.PerfectBidToCPC;
import simulator.models.PerfectBidToPosition;
import simulator.models.PerfectConversionProb;
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
	Set<Double> _possibleBids;
	Set<Integer> _possibleQuantities;	// a vector of all the quantities that we are willing to over sell 
	protected static Map <Product , Double> _productRevenue;	// the revenue from every product, including component specialty
	protected HashMap<Query, Double> _bids;					// a set of our decision for bidding on every query. 0 if no bidding
	protected HashMap<Query, Double> _dailyLimit;		// daily limit per query
	protected HashMap<Integer, Query> _queryIndexing;	// mapping queries into integers - it help with the cplex variable array
	Set<UserState> searchingUserStates;
	int _numSearchingUsers = 0;
	
	/// Models
	private HashMap<Query, AbstractBidToSlotModel> _bidToSlotModels_simple;	// I've made two models for bidding, one is simple
	private HashMap<Query, AbstractBidToSlotModel> _bidToSlotModels_ilke;	// and the second takes some days until it is good enough
	private HashMap<Query, AbstractBidToCPC> _bidToCPCModels;
	private HashMap<Query, AbstractSlotToPrClick> _slotToClickPrModels;
	private HashMap<Query, AbstractSlotToCPCModel> _slotToCPCModels;	// a simple CPC model, but later the agent is using the better slot2bid
	private HashMap<Product, HashMap<UserState, AbstractPrConversionModel>> _convPrModel;	// probability of a conversion
	private AbstractUserModel _userModel;	// the number of users in every state
	
	
	// ###################
	// #### The Agent ####
	// ###################

	public ILPAgent(){
		System.out.println("Let's start");
		_productRevenue = new HashMap<Product, Double>();
		_bids = new HashMap<Query, Double>();
		_dailyLimit = new HashMap<Query, Double>();
		_queryIndexing = new HashMap<Integer, Query>();

		searchingUserStates = new HashSet<UserState>();
		searchingUserStates.add(UserState.F0);
		searchingUserStates.add(UserState.F1);
		searchingUserStates.add(UserState.F2);
		searchingUserStates.add(UserState.IS);
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
			double bid;
			if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
				bid = .7;
			else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE))
				bid = 1.3;
			else 
				bid = 1.7;
			_bids.put(query, bid);
			
			_queryIndexing.put(qvalue, query);
			qvalue++;
			
			_dailyLimit.put(query, Double.MAX_VALUE);
		}

		_possibleBids = new HashSet<Double>(setPossibleBids(0.01, 5, .05));
		_possibleQuantities = new HashSet<Integer>(setPossibleQuantities(0, 40, 1));
	}
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {		
		if (_day > 1) {
			updateBids();
			buildMaps(models);
		}
		
		BidBundle bb = new BidBundle();
		for (Query query:_querySpace) {
			bb.addQuery(query, _bids.get(query), new Ad(), _dailyLimit.get(query)); //TODO add a daily limit
		}
		return bb;
	}
	
	
	// ###############
	// #### CPLEX ####
	// ###############
	/**
	 * Creates the bids using the ILP calculation
	 */
	public void updateBids() {
		
		int qsize = _possibleQuantities.size();
		Integer[] quantitiesSet = new Integer[qsize];
		quantitiesSet = _possibleQuantities.toArray(quantitiesSet).clone();
		int bsize = _possibleBids.size();
		Double[] bidsSet = new Double[qsize];
		bidsSet = _possibleBids.toArray(bidsSet).clone();
		
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
			IloIntVar[][] bidsVar = new IloIntVar[NUMOFQUERIES][_numSlots];	//TODO consider making the slots real and not discreet
			IloLinearNumExpr exprObj = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				bidsVar[query] = cplex.boolVarArray(bsize);

				System.out.print(_queryIndexing.get(query).toString() + " ## ");
				for (int bidIndex=0 ; bidIndex<bsize ; bidIndex++) {
					exprObj.addTerm(bidsVar[query][bidIndex], getObjBidCoef(bidsSet[bidIndex] , _queryIndexing.get(query)));
					System.out.print(bidIndex + "->" + getObjBidCoef(bidsSet[bidIndex] , _queryIndexing.get(query)) + "\t");
				}
				System.out.println();
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
			cplex.addLe(exprCapacity, DAILYCAPACITY);	// the capacity sold minus the over capacity (=epxrCapacity) need to be less than the daily capacity
			System.out.println(exprCapacity.toString());
			if (cplex.solve()) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				
				for (int query=0 ; query<NUMOFQUERIES ; query++) {
					double[] queryResults = cplex.getValues(bidsVar[query]);
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
							_bids.put(_queryIndexing.get(query) , bidsSet[bidIndex]); 
//							bidThisQuery = true;
							System.out.println(_queryIndexing.get(query).toString() + "\t" + "Bid=" + bidsSet[bidIndex] + " ObjCoef=" + getObjBidCoef((double)bidIndex, _queryIndexing.get(query)) + " QCoef=" + getQuantityBoundBidCoef(bidIndex, _queryIndexing.get(query)));
						}
					}
					if (!bidThisQuery) _bids.put(_queryIndexing.get(query), 0.0);
				}
				if (USEOVERQUANTITY) {
					double[] quantityResults = cplex.getValues(overCapVar);
					for (int quantity=0 ; quantity<quantityResults.length ; quantity++) {
						if (quantityResults[quantity] == 1.0) System.out.println("OverQuantity=" + quantitiesSet[quantity] + " ObjCoef=" + getObjOverQuantityCoef(quantity));
					}
				}
			}
			//System.out.println(cplex.toString());
			cplex.end();
		}
		catch (IloException e) {
			System.err.println ("Concert Exception" + e + "' caught");
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
		
		double bid2slot = (_numSlots - Math.floor(bid/0.71));	// if there is no good bid, use this
		if (_bidToSlotModels_simple.get(query) instanceof PerfectBidToPosition) {
			try {
				bid2slot = _bidToSlotModels_simple.get(query).getPrediction(bid);
			}
			catch (Exception e) {
				System.err.println("problem with prediction of perfect bid to position");
//				bid2slot = getObjBidCoef(bid-0.01, query);
			}
		}
		bid2slot = Math.min(_numSlots,Math.max(bid2slot, 1));

		for (UserState us : setOfUserStates(query)) {				
			for (Product p : setOfProducts(query, us)) {
				result += estimateImpressionOutOfTotalSearchingUsers(p,us) * estimateClicks(p,us,query,bid2slot) * (estimateConv(p,us,0)*_productRevenue.get(p) - estimateCPC(query, bid)); 
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
		
		double bid2slot = (_numSlots - Math.floor(bid/0.71));	// if there is no good bid, use this
		if (_bidToSlotModels_simple.get(query) instanceof PerfectBidToPosition) {
			bid2slot = _bidToSlotModels_simple.get(query).getPrediction(bid);
		}
		bid2slot = Math.min(_numSlots,Math.max(bid2slot, 1));

		for (UserState us : setOfUserStates(query)) {
			for (Product p : setOfProducts(query, us)) {
				double p_x_Pr = estimateImpressionOutOfTotalSearchingUsers(p, us); // TODO find a model
				double p_x_convPr = estimateConv(p,us,0);
				double click_Pr_q_p_x_s = estimateClicks(p, us, query, bid2slot);
				
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
				
		for (Product p : _retailCatalog) {
			double revenue = _productRevenue.get(p); 
			for (UserState us : UserState.values()) {//setOfUserStates(bestQuery)) {
				double p_x_convPr = estimateConv(p,us,0);
				for (int i=1 ; i < quantity ; i++) {
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
		
		double bid2slot = _bidToSlotModels_simple.get(query).getPrediction(bid);
		bid2slot = Math.max(_numSlots,Math.min(bid2slot, 1));
		AbstractSlotToPrClick click_Pr_q = _slotToClickPrModels.get(query);
		double slot2cpc = _slotToCPCModels.get(query).getPrediction(bid2slot);

		double click_Pr_q_p_x_s = click_Pr_q.getPrediction(bid2slot); // TODO change this arbitrary constant
		for (UserState us : setOfUserStates(query)) {
			for (Product p : setOfProducts(query, us)) {
				double p_x_Pr = estimateImpressionOutOfTotalSearchingUsers(p,us);
				
				result += p_x_Pr * click_Pr_q_p_x_s;
			}
		}
		
		return _numSearchingUsers * result * slot2cpc;
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
	public double estimateClicks (Product p, UserState us, Query query, double slotOrBid) {
		// this model uses slots and not bids
		
		AbstractSlotToPrClick click_Pr_q = _slotToClickPrModels.get(query);
		double result = click_Pr_q.getPrediction(slotOrBid); // TODO change this arbitrary constant, and move it inside the loop
		
		return result;
	}
	
	public double estimateCPC (Query query, double bid) {
//		double slot2cpc = _slotToCPC.get(query).getPrediction(slot);
		double bid2slot = 0;
		if (_day < DAYSUNTILILKE) bid2slot = _bidToSlotModels_simple.get(query).getPrediction(bid);
		else bid2slot = _bidToSlotModels_ilke.get(query).getPrediction(bid);
		bid2slot = Math.min(_numSlots,Math.max(bid2slot, 1));
		double bid2cpc = _slotToCPCModels.get(query).getPrediction(bid2slot);
		if (_bidToCPCModels.get(query) instanceof PerfectBidToCPC) {
			//bid2cpc = _bidToCPCModels.get(query).getPrediction(bid);
		}
		if (Double.isNaN(bid2cpc) || Double.isInfinite(bid2cpc) || Double.isInfinite(-bid2cpc) || Double.valueOf(bid2cpc).equals(0.0)) bid2cpc = _regReserveScore+0.01; 
		return 0.0;
	}
	
	private double estimateConv(Product p, UserState us, int i) {
		double result = 0;
		
		if (us.equals(UserState.NS) || us.equals(UserState.T) || us.equals(UserState.IS)) return result;

		AbstractPrConversionModel convPr = _convPrModel.get(p).get(us);
		if (convPr.equals(null) || p.equals(null) || us.equals(null) || i<(Integer)(_possibleQuantities.toArray())[0] || (i>(Integer)(_possibleQuantities.toArray()[_possibleQuantities.size()-1]))) {
			beep();
			System.out.print("\n CONV PROBLEM--> " + p.getComponent() + p.getManufacturer() + us.toString() + i + "\n");
		}
		if (i != 0) {
			int x = 1;
			x++;
		}
		try {
			result = convPr.getPrediction(i);
			if (convPr instanceof PerfectConversionProb) {
				if (p.getComponent().equals(_compSpecialty)) result = unEta(result,_CSB);
				result = result*1.2;
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
		_convPrModel = new HashMap<Product,HashMap<UserState, AbstractPrConversionModel>>();
		_slotToCPCModels = new HashMap<Query, AbstractSlotToCPCModel>();

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

			AbstractSlotToPrClick slotToPrClick = new DetBasicSlotToPrClick(query);
			models.add(slotToPrClick);
			_slotToClickPrModels.put(query,slotToPrClick);
			
			AbstractSlotToCPCModel slotToCPC = new LinearSlotToCPC(query, _numSlots);
			models.add(slotToCPC);
			_slotToCPCModels.put(query, slotToCPC);			
		}
		_numSearchingUsers = 0;
		for (Product p : _retailCatalog) {
			HashMap<UserState, AbstractPrConversionModel> temp = new HashMap<UserState, AbstractPrConversionModel>();
			for (UserState us : searchingUserStates) {
				AbstractPrConversionModel convModel = new TrinaryPrConversion(p, us, _lambda, _compSpecialty, _advertiserInfo.getComponentBonus());
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
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				slotToPrClick.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToCPCModel) {
				AbstractSlotToCPCModel slotToCPC = (AbstractSlotToCPCModel) model;
				slotToCPC.updateModel(queryReport, salesReport, _bids);
			}
			else if(model instanceof AbstractPrConversionModel) {
				AbstractPrConversionModel convPr = (AbstractPrConversionModel) model;
				convPr.updateModel(queryReport, salesReport);
			}
		}
	}
	
	protected void buildMaps(Set<AbstractModel> models) {
/*		_bidToSlotModels = new HashMap<Query, AbstractBidToSlotModel>();
		_slotToBidModels_simple = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToBidModels_ilke = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToPrClickModels = new HashMap<Query, AbstractSlotToPrClick>();*/
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
//			else if(model instanceof AbstractSlotToBidModel) {
//				AbstractSlotToBidModel slotToBid = (AbstractSlotToBidModel) model;
//				if (slotToBid instanceof LinearSlotToBid) _bidToSlotModels_simple.put(slotToBid.getQuery(), slotToBid);
//				else _bidToSlotModels_ilke.put(slotToBid.getQuery(), slotToBid);
//			}
			else if(model instanceof AbstractBidToSlotModel) {
			AbstractBidToSlotModel bidToSlot = (AbstractBidToSlotModel) model;
			if (model instanceof PerfectBidToPosition) {
				_bidToSlotModels_simple.put(bidToSlot.getQuery(), bidToSlot);
				_bidToSlotModels_ilke.put(bidToSlot.getQuery(), bidToSlot);
			} else {
				if (bidToSlot instanceof ReallyBadBidToSlot) _bidToSlotModels_simple.put(bidToSlot.getQuery(), bidToSlot);
				else _bidToSlotModels_ilke.put(bidToSlot.getQuery(), bidToSlot);
			}
		}
			else if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				_bidToCPCModels.put(bidToCPC.getQuery(), bidToCPC);
			}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				_slotToClickPrModels.put(slotToPrClick.getQuery(), slotToPrClick);
			}
			else if(model instanceof AbstractSlotToCPCModel) {
				AbstractSlotToCPCModel slotToCPC = (AbstractSlotToCPCModel) model;
				_slotToCPCModels.put(slotToCPC.getQuery(), slotToCPC);
			}
			else if(model instanceof AbstractPrConversionModel) {
				AbstractPrConversionModel convPr = (AbstractPrConversionModel) model;
				if (convPr instanceof PerfectConversionProb) {
					UserState userState = getStateFromQuery(convPr.getQuery());
					for (Product product : setOfProducts(convPr.getQuery(), userState)) {
						HashMap<UserState, AbstractPrConversionModel> temp = new HashMap<UserState, AbstractPrConversionModel>();
						temp.putAll((_convPrModel.get(product)));
						temp.put(userState, convPr);
						_convPrModel.put(product, temp);
					}
				} else {
					Product product = convPr.getPair().getFirst();
					UserState userState = convPr.getPair().getSecond();					
					HashMap<UserState, AbstractPrConversionModel> temp = new HashMap<UserState, AbstractPrConversionModel>();
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

	private Set<UserState> setOfUserStates(Query query) {
		Set<UserState> us = new HashSet<UserState>();
		us.add(UserState.IS);
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) us.add(UserState.F0); 
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) us.add(UserState.F1);
		if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) us.add(UserState.F2);
		return us;
	}

	private Set<Product> setOfProducts(Query query, UserState us) {
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

/*	private boolean equalStates(UserState us, Query q) {
		if (us.equals(UserState.F0) && q.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) return true; 
		if (us.equals(UserState.F1) && q.getType().equals(QueryType.FOCUS_LEVEL_ONE)) return true; 
		if (us.equals(UserState.F2) && q.getType().equals(QueryType.FOCUS_LEVEL_TWO)) return true;
		return false;
	}*/
	
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
	
	private void beep() {
		Toolkit.getDefaultToolkit().beep();
	}
}