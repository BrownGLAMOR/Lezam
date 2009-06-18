package agents;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import newmodels.AbstractModel;
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

	final static boolean USEOVERQUANTITY = false; 	// Do you want the MIP formula to include over capacity?
	final static int DAYSUNTILILKE = 10; 			// How many days until the Bid-Slot bucket model starts to work?
	protected static int NUMOFUSERS = 90000;		// How many users are simulated?
	
	
	// ###################
	// #### Variables ####
	// ###################
	//protected double _day;
	Vector<Double> _possibleBids;
	Vector<Integer> _possibleQuantities;
	protected static int NUMOFQUERIES;
	protected static int DAILYCAPACITY;
	protected static double DISCOUNTER;
	public static Map <Product , Double> _productRevenue;
	protected HashMap<Query, Double> _bids;
	private Hashtable<Integer, Query> _queryIndexing;
	
	private HashMap<Query, AbstractSlotToBidModel> _slotToBidModels_simple;
	private HashMap<Query, AbstractSlotToBidModel> _slotToBidModels_ilke;
	private HashMap<Query, AbstractSlotToPrClick> _slotToPrClickModels;
	private HashMap<Query, AbstractSlotToCPCModel> _slotToCPC;
	private HashMap<Product, HashMap<UserState, AbstractPrConversionModel>> _convModel;
	private AbstractUserModel _userModel;
	
	// ###################
	// #### The Agent ####
	// ###################

	public ILPAgent(){
		System.out.println("Let's start");
		_productRevenue = new HashMap<Product, Double>();
		_bids = new HashMap<Query, Double>();
		_queryIndexing = new Hashtable<Integer, Query>();
	}

	protected void simulationSetup() {}

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
		for(Query query : _querySpace){
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
		}

		_possibleQuantities = new Vector<Integer>(setPossibleQuantities(0, 40, 1));
		System.out.println("q size = " + _possibleQuantities.size());
	}
	
	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {		
		if (_day > 1) {
			updateBids();
			buildMaps(models);
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
	 */
	public void updateBids() {
		
		int qsize = _possibleQuantities.size();
		Integer[] quantities = new Integer[qsize];
		_possibleQuantities.copyInto(quantities);
		
		for (Product p : _retailCatalog) {
			System.out.print(p.toString() + " ## ");
			for (UserState us : UserState.values()) {
				for (int i=0 ; i<31 ; i=i+10) {
					System.out.print(us.toString() + "->" + get_convPr(p, us, i) + "\t");
				}
				System.out.println();
			}
		}
		
		System.out.println("PROBABILITY OF A CLICK");
		for (Query q : _querySpace) {
			System.out.print (q.toString() + " ## ");
			for (int slot=1 ; slot<=_numSlots ; slot++ ) {
				System.out.print(slot + "->" + _slotToPrClickModels.get(q).getPrediction(slot) + "\t");				
			}
			System.out.println();
		}

		try {
			IloCplex cplex = new IloCplex();
			IloIntVar[] overQuantVar = cplex.boolVarArray(qsize);
			IloIntVar[][] slotsVar = new IloIntVar[NUMOFQUERIES][_numSlots];	//TODO consider making the slots real and not discreat
			IloLinearNumExpr exprObj = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				slotsVar[query] = cplex.boolVarArray(_numSlots);

				System.out.print(_queryIndexing.get(query).toString() + " ## ");
				for (int slot=1 ; slot<=_numSlots ; slot++) {
					exprObj.addTerm(slotsVar[query][slot-1], getObjSlotCoef(slot , _queryIndexing.get(query)));
					System.out.print(slot + "->" + getObjSlotCoef(slot , _queryIndexing.get(query)) + "\t");
				}
				System.out.println();
			}
			if (USEOVERQUANTITY) for (int quantity=0 ; quantity < qsize ; quantity++) {
				exprObj.addTerm(overQuantVar[quantity], -getObjOverQuantityCoef(quantities[quantity]));				
			}		
			cplex.addMaximize(exprObj);

			IloLinearNumExpr exprCapacity = cplex.linearNumExpr();
			IloLinearNumExpr exprOverCapLE = cplex.linearNumExpr();
			IloLinearNumExpr exprSlotLE = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				for (int slot=1 ; slot<=_numSlots; slot++) {
					exprCapacity.addTerm(slotsVar[query][slot-1] , getQuantityBoundSlotCoef(slot , _queryIndexing.get(query)));
					exprSlotLE.addTerm(1.0, slotsVar[query][slot-1]);
				}
				cplex.addLe(exprSlotLE, 1);
				exprSlotLE = cplex.linearNumExpr();
			}
			if (USEOVERQUANTITY) {
				for (int quantity=0 ; quantity<quantities.length ; quantity++) {
					exprCapacity.addTerm(overQuantVar[quantity] , -quantities[quantity]);				
					exprOverCapLE.addTerm(1.0, overQuantVar[quantity]);
				}
				cplex.addLe(exprOverCapLE, 1);
			}
			cplex.addLe(exprCapacity, DAILYCAPACITY);
			
			if (cplex.solve()) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				
				for (int query=0 ; query<NUMOFQUERIES ; query++) {
					double[] queryResults = cplex.getValues(slotsVar[query]);
					boolean bidThisQuery = false;
//					System.out.print(_queryIndexing.get(query).toString());
					for (int slot=1 ; slot<=queryResults.length ; slot++) {
						if (queryResults[slot-1] == 1.0) {
							double bid;
							if (_day < DAYSUNTILILKE) {
								bid = (_numSlots - slot + 1) * 0.71 + 0.71;
							} else {
								bid = _slotToBidModels_ilke.get(_queryIndexing.get(query)).getPrediction(slot);
								if ((Double.isInfinite(bid) || Double.isNaN(bid)) || (bid == 0.0)) bid = (_numSlots - slot + 1) * 0.71 + 0.71;
								_bids.put(_queryIndexing.get(query) , bid);  //slot+1 because the first index represents the first slot and so on
							}
							bidThisQuery = true;
							System.out.println(_queryIndexing.get(query).toString() + "\t" + "Slot=" + slot + " bid=" + _bids.get(_queryIndexing.get(query)) + " ObjCoef=" + getObjSlotCoef((double)slot, _queryIndexing.get(query)) + " QCoef=" + getQuantityBoundSlotCoef(slot, _queryIndexing.get(query)));
						}
					}
					if (!bidThisQuery) _bids.put(_queryIndexing.get(query), 0.0);
				}
				if (USEOVERQUANTITY) {
					double[] quantityResults = cplex.getValues(overQuantVar);
					for (int quantity=0 ; quantity<quantityResults.length ; quantity++) {
						if (quantityResults[quantity] == 1.0) System.out.println("OverQuantity=" + quantities[quantity] + " ObjCoef=" + getObjOverQuantityCoef(quantity));
					}
				}
			}
//			System.out.println(cplex.toString());
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
	public double getObjSlotCoef(double slot , Query query) {		
		double result = 0;

//		double slot2cpc = _slotToCPC.get(query).getPrediction(slot);
		double slot2cpc;
		if (_day < DAYSUNTILILKE) slot2cpc = (_numSlots - slot + 1) * 0.71;
		else slot2cpc = _slotToBidModels_ilke.get(query).getPrediction(slot+1); // bid of next slot is the CPC
		if (Double.isNaN(slot2cpc) || Double.isInfinite(slot2cpc) || Double.isInfinite(-slot2cpc) || Double.valueOf(slot2cpc).equals(0.0)) slot2cpc = _regReserveScore+0.01; 
		AbstractSlotToPrClick click_Pr_q = _slotToPrClickModels.get(query);
		
		double click_Pr_q_p_x_s = click_Pr_q.getPrediction(slot); // TODO change this arbitrary constant, and move it inside the loop
		for (Product p : setOfProducts(query)) {
			for (UserState us : setOfUserStates(query)) {
				double p_x_Pr = get_p_x_Pr(p,us);	//TODO change this, I just used the percentage of users
				//System.out.print(p_x_Pr + "-");
				double p_x_convPr = get_convPr(p,us,0);
				
				result += p_x_Pr * click_Pr_q_p_x_s * (p_x_convPr*_productRevenue.get(p) - slot2cpc); 
				if (Double.isNaN(result) || Double.isInfinite(result)) {
					beep();
					System.out.print("\n The is a null result: \n slot2cpc=" + slot2cpc + "\n clickPr=" + click_Pr_q_p_x_s + "\n get_convPr=" + p_x_convPr + "\n p_x_Pr" + p_x_Pr + "\n");
					System.out.print("\007");
				}
			}
		}
		return NUMOFUSERS * result;
	}

	/**
	 * returns the sum of n*Pr(p,x)*Pr(click|q,p,x,s(b)*pi(p,x)
	 * @param bid
	 * @param query
	 * @return a double representing the sum
	 */
	public double getQuantityBoundSlotCoef(double slot, Query query) {
		
		double result = 0;
		
		AbstractSlotToPrClick click_Pr_q = _slotToPrClickModels.get(query);

		double click_Pr_q_p_x_s = click_Pr_q.getPrediction(slot); // TODO change this arbitrary constant
		for (Product p : setOfProducts(query)) {
			for (UserState us : setOfUserStates(query)) {
				double p_x_Pr = get_p_x_Pr(p,us); // TODO find a model
				double p_x_convPr = get_convPr(p,us,0);
				
				result += p_x_Pr * click_Pr_q_p_x_s * p_x_convPr;
			}
		}
		
		return NUMOFUSERS * result / 20;
	}

	/**
	 * the sum of all the products and user states for a given amount of over-capacity
	 * @param quantity (over capacity)
	 * @return a double representing the sum
	 */
	public double getObjOverQuantityCoef(int quantity) {
		
		double result=0;
		
		//Query bestQuery = new Query(_manSpecialty,_compSpecialty);	// TODO find a decent query
		
		for (Product p : _retailCatalog) {
			double revenue = _productRevenue.get(p); 
			for (UserState us : UserState.values()) {//setOfUserStates(bestQuery)) {
				double p_x_convPr = get_convPr(p,us,0);
				for (int i=1 ; i < quantity ; i++) {
					double p_x_Pr = get_p_x_Pr(p,us);
					double p_x_i_convPr = get_convPr(p,us,i);
					
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
	public double getQueryDailySpendLimit(double slot, Query query) {
		
		double result = 0;
		
		AbstractSlotToPrClick click_Pr_q = _slotToPrClickModels.get(query);
		double slot2cpc = _slotToCPC.get(query).getPrediction(slot);

		double click_Pr_q_p_x_s = click_Pr_q.getPrediction(slot); // TODO change this arbitrary constant
		for (Product p : setOfProducts(query)) {
			for (UserState us : setOfUserStates(query)) {
				double p_x_Pr = get_p_x_Pr(p,us);
				
				result += p_x_Pr * click_Pr_q_p_x_s;
			}
		}
		
		return NUMOFUSERS * result * slot2cpc;
	}
	
	// #########################
	// #### Models Updating ####
	// #########################
	
	@Override
	public Set<AbstractModel> initModels() {
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		Set<UserState> userStates = new HashSet<UserState>();
		userStates.add(UserState.F0);
		userStates.add(UserState.F1);
		userStates.add(UserState.F2);
		userStates.add(UserState.IS);
		
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		_slotToBidModels_simple = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToBidModels_ilke = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToPrClickModels = new HashMap<Query, AbstractSlotToPrClick>();
		_convModel = new HashMap<Product,HashMap<UserState, AbstractPrConversionModel>>();
		_slotToCPC = new HashMap<Query, AbstractSlotToCPCModel>();
		AbstractUserModel userModel = new BasicUserModel();
		models.add(userModel);
		_userModel = userModel;
		for(Query query: _querySpace) {
//			AbstractSlotToBidModel slotToBid = new ReallyBadSlotToBid(query);
			AbstractSlotToBidModel slotToBid = new LinearSlotToBid(query,_numSlots);
			models.add(slotToBid);
			_slotToBidModels_ilke.put(query,slotToBid);
			slotToBid = new WrapperIlkePos2Bid(query,_numSlots);
			models.add(slotToBid);
			_slotToBidModels_simple.put(query,slotToBid);
			
			AbstractSlotToPrClick slotToPrClick = new DetBasicSlotToPrClick(query);
			models.add(slotToPrClick);
			_slotToPrClickModels.put(query,slotToPrClick);
			
			AbstractSlotToCPCModel slotToCPC = new LinearSlotToCPC(query, _numSlots);
			models.add(slotToCPC);
			_slotToCPC.put(query, slotToCPC);			
		}
		for (Product p : _retailCatalog) {
			HashMap<UserState, AbstractPrConversionModel> temp = new HashMap<UserState, AbstractPrConversionModel>();
			for (UserState us : userStates) {
				AbstractPrConversionModel convModel = new TrinaryPrConversion(p, us, _lambda, _compSpecialty, _advertiserInfo.getComponentBonus());
				models.add(convModel);
				temp.put(us, convModel);
			}
			_convModel.put(p, temp);
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
			}
			else if(model instanceof AbstractSlotToBidModel) {
				AbstractSlotToBidModel slotToBid = (AbstractSlotToBidModel) model;
				slotToBid.updateModel(queryReport, salesReport, _bids);
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
		//_bidToSlotModels = new HashMap<Query, AbstractBidToSlotModel>();
		_slotToBidModels_simple = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToBidModels_ilke = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToPrClickModels = new HashMap<Query, AbstractSlotToPrClick>();
		for(AbstractModel model : models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				_userModel = userModel;
			}
			else if(model instanceof AbstractSlotToBidModel) {
				AbstractSlotToBidModel slotToBid = (AbstractSlotToBidModel) model;
				if (slotToBid instanceof WrapperIlkePos2Bid) _slotToBidModels_ilke.put(slotToBid.getQuery(), slotToBid);
				else _slotToBidModels_simple.put(slotToBid.getQuery(), slotToBid);	
			}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				_slotToPrClickModels.put(slotToPrClick.getQuery(), slotToPrClick);
			}
			else if(model instanceof AbstractSlotToCPCModel) {
				AbstractSlotToCPCModel slotToCPC = (AbstractSlotToCPCModel) model;
				_slotToCPC.put(slotToCPC.getQuery(), slotToCPC);
			}
			else if(model instanceof AbstractPrConversionModel) {
				AbstractPrConversionModel convPr = (AbstractPrConversionModel) model;
				HashMap<UserState, AbstractPrConversionModel> temp = new HashMap<UserState, AbstractPrConversionModel>();
				temp.putAll((_convModel.get(((TrinaryPrConversion)convPr).getPair().getFirst())));
				temp.put(((TrinaryPrConversion)convPr).getPair().getSecond(), convPr);
				_convModel.put(((TrinaryPrConversion)convPr).getPair().getFirst(), temp);
			}
		}
	}

	
	// ##########################
	// #### Helper Functions ####
	// ##########################
	public Vector<Double> setPossibleBids (double minBid, double maxBid, double interval) {
		Vector<Double> bids = new Vector<Double>();
		if ((minBid < 0) || (maxBid <= minBid)) return bids;
		double bid = minBid;
		while (bid < maxBid) {
			bids.add(bid);
			bid += interval;
		}
		return bids;
	}

	public Vector<Integer> setPossibleQuantities(int minQ, int maxQ, int interval) {
		Vector<Integer> quantity = new Vector<Integer>();
		if ((minQ < 0) || (maxQ <= minQ)) return quantity;
		int q = minQ;
		while (q <= maxQ) {
			quantity.add(q);
			q += interval;
		}
		return quantity;
	}

	private double get_convPr(Product p, UserState us, int i) {
		double result = 0;
		
		if (us.equals(UserState.NS) || us.equals(UserState.T)) return result;

		AbstractPrConversionModel convPr = _convModel.get(p).get(us);
		if (convPr.equals(null) || p.equals(null) || us.equals(null) || (i<_possibleQuantities.get(0) || (i>_possibleQuantities.get(_possibleQuantities.size()-1)))) {
			beep();
			System.out.print("\n CONV PROBLEM--> " + p.getComponent() + p.getManufacturer() + us.toString() + i + "\n");
		}
		result = convPr.getPrediction(i);
		return result;
	}

	private double get_p_x_Pr(Product p, UserState us) {
		double result = 0;
		if (us.equals(UserState.IS) || us.equals(UserState.F0)) result = _userModel.getPrediction(us)/_retailCatalog.size();
		else if (us.equals(UserState.F1)) {
			result = _userModel.getPrediction(us)/6; // TODO 6 is a constant. no good.
		} else if (us.equals(UserState.F2)) {
			result = _userModel.getPrediction(us);			
		}
		return result;
	}
	
	private Vector<UserState> setOfUserStates(Query query) {
		Vector<UserState> us = new Vector<UserState>();
		us.add(UserState.IS);
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO)) us.add(UserState.F0); 
		if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE)) us.add(UserState.F1);
		if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) us.add(UserState.F2);
		return us;
	}

	private Vector<Product> setOfProducts(Query query) {
		Vector<Product> products = new Vector<Product>();
		if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO)) {
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
		} else {
			for (Product p : _retailCatalog) {
				products.add(p);
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
	
	private void beep() {
		Toolkit.getDefaultToolkit().beep();
	}
}