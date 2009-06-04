package agents;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import newmodels.AbstractModel;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.bidtoslot.BasicBidToSlot;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.prconv.BasicPrConversion;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottobid.BasicSlotToBid;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.slottonumclicks.BasicSlotToNumClicks;
import newmodels.slottonumimp.AbstractSlotToNumImp;
import newmodels.slottonumimp.BasicSlotToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.slottoprclick.BasicSlotToPrClick;
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

	// ###################
	// #### Variables ####
	// ###################
	protected double _day;
	Vector<Double> _possibleBids;
	Vector<Integer> _possibleQuantities;
	protected static int NUMOFQUERIES;
	protected static int DAILYCAPACITY;
	protected static int NUMOFUSERS;
	protected static double DISCOUNTER;
	public static Map <Product , Double> _productRevenue;
	protected HashMap<Query, Double> _bids;
	private Hashtable<Integer, Query> _queryIndexing;
	
	private HashMap<Query, AbstractBidToSlotModel> _bidToSlotModels;
	private HashMap<Query, AbstractSlotToBidModel> _slotToBidModels;
	private HashMap<Query, AbstractSlotToPrClick> _slotToPrClickModels;
	private HashMap<Query, AbstractSlotToNumImp> _slotToNumImptModels;
	private HashMap<Query, AbstractSlotToNumClicks> _slotToNumClicks;
	private HashMap<Query, AbstractPrConversionModel> _convModel;
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
	protected void initBidder() {
		System.out.println("Initilizing Bids");
		
		DAILYCAPACITY = _advertiserInfo.getDistributionCapacity() / _advertiserInfo.getDistributionWindow();
		NUMOFUSERS =  90000; // TODO CHANGE CONSTANT
		DISCOUNTER = _advertiserInfo.getDistributionCapacityDiscounter();
		
		for (Product p : _retailCatalog) {
			int givesBonus=0;
			if (p.getManufacturer().equals(_advertiserInfo.getManufacturerSpecialty())) givesBonus = 1;
			double profit = _retailCatalog.getSalesProfit(p)*(1+_advertiserInfo.getComponentBonus()*givesBonus);
			System.out.println("product " + p.getComponent() + " " + p.getManufacturer() + " with profit " + profit);
			_productRevenue.put(p, profit);
		}

		for(Query query : _querySpace){
			double bid;
			if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
				bid = .7;
			else if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE))
				bid = 1.3;
			else 
				bid = 1.7;
			_bids.put(query, bid);
		}

		_possibleBids = new Vector<Double>(setPossibleBids(0, 10, 0.01)); //TODO replace 10 with a more reasonable lower bid
		_possibleQuantities = new Vector<Integer>(setPossibleQuantities(0, 30, 1)); //TODO replace maximum quantities with a max's offer
		System.out.println("b size = " + _possibleBids.size());
		System.out.println("q size = " + _possibleQuantities.size());
	}
	
	@Override
	protected BidBundle getBidBundle(Set<AbstractModel> models) {		
		if (_day > 1) {
			updateBids();
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
		//TODO calculate the new bids according to new results coming from the models
		
		//HashMap<Query,Double> newBids = new HashMap<Query, Double>();
		int bsize = _possibleBids.size();
		int qsize = _possibleQuantities.size();
		Double[] bids = new Double[bsize];
		_possibleBids.copyInto(bids);
		Integer[] quantities = new Integer[qsize];
		_possibleQuantities.copyInto(quantities);

		try {
			IloCplex cplex = new IloCplex();
			IloIntVar[] overQuantVar = cplex.boolVarArray(qsize);
//			IloIntVar[] overQuantVar = cplex.intVarArray(quantities.length, 0, 1);
			IloIntVar[][] bidsVar = new IloIntVar[NUMOFQUERIES][bsize];
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				bidsVar[query] = cplex.boolVarArray(bsize);
//				bidsVar[query] = cplex.intVarArray(bids.length, 0, 1);
			}
			IloLinearNumExpr expr = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				for (int bid=0 ; bid<bsize ; bid++) {
					expr.addTerm(bidsVar[query][bid], getBidCoefficient(bids[bid] , _queryIndexing.get(query)));
				}
			}
			for (int quantity=0 ; quantity < qsize ; quantity++) {
				expr.addTerm(overQuantVar[quantity], -getQuantityCoefficient(quantities[quantity]));				
			}
			
			cplex.addMaximize(expr);
			expr.clear();
			IloLinearNumExpr exprOverQ = cplex.linearNumExpr();
			IloLinearNumExpr exprBid = cplex.linearNumExpr();
			for (int query=0 ; query<NUMOFQUERIES ; query++) {
				for (int bid=0 ; bid<bsize ; bid++) {
					expr.addTerm(bidsVar[query][bid] , getQuantityBoundCoef(bids[bid] , _queryIndexing.get(query)));
					exprBid.addTerm(1.0, bidsVar[query][bid]);
				}
				cplex.addLe(exprBid, 1);
			}
			for (int quantity=0 ; quantity<quantities.length ; quantity++) {
				expr.addTerm(overQuantVar[quantity] , -quantities[quantity]);				
				exprOverQ.addTerm(1.0, overQuantVar[quantity]);
			}
			cplex.addLe(exprOverQ, 1);
			cplex.addLe(expr, DAILYCAPACITY);
			
			if (cplex.solve()) {
				cplex.output().println("Solution status = " + cplex.getStatus());
				cplex.output().println("Solution value = " + cplex.getObjValue());
				
				//double[] bidsPerQuery = new double[NUMOFQUERIES];
				for (int query=0 ; query<NUMOFQUERIES ; query++) {
					double[] queryResults = cplex.getValues(bidsVar[query]);
					for (int i=0 ; i<queryResults.length ; i++) {
						//if (queryResults[i] == 1.0) bidsPerQuery[query] = bids[i];
						if (queryResults[i] == 1.0) _bids.put(_queryIndexing.get(query) , bids[i]);
						System.out.print(queryResults[i] + " - ");
					}
					System.out.println();
				}
			}
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
	 * @return a dounle representing the sum
	 */
	public double getBidCoefficient(double bid , Query query) {
		/*HashMap <Pair<Product,UserState> , Double> p_x_Pr;
		HashMap <Pair<Pair<Product,UserState>,Pair<Query,Integer>>,Double> click_Pr_q_p_x_s;
		HashMap <Double,Integer> slotOfBid;
		HashMap <Pair<Product,UserState> , Double> p_x_convPr;
		HashMap <Pair<Pair<Product,UserState>,Integer>,Double> p_x_i_convPr;
		HashMap <Double,Double> cpcOfBid;
		HashMap <Product,Double> revenueP;*/
		
		
		double result = 0;
		//Pair<Query,Integer> q_s = new Pair<Query,Integer>(query,slotOfBid.get(bid));
		//double cpcBid = cpcOfBid.get(bid);
		//AbstractBidToSlotModel slotOfBid_q = ;
		double slotOfBid = _bidToSlotModels.get(query).getPrediction(bid);
		AbstractPrConversionModel convPr = _convModel.get(query);
		
		for (Product p : _retailCatalog) {
			for (UserState userFocus:UserState.values()) {
				double p_x_Pr = 0.5; // TODO change this arbitrary constant
				double click_Pr_q_p_x_s = 0.5; // TODO change this arbitrary constant
				// find all models
				/*Pair<Product , Integer> p_x = new Pair<Product , Integer>(p,userFocus);
				Pair<Pair<Product,Integer>,Pair<Query,Integer>> q_p_x_s = new Pair<Pair<Product,Integer>,Pair<Query,Integer>>(p_x,q_s);
				result += NUMOFUSERS * p_x_Pr.get(p_x) * click_Pr_q_p_x_s.get(q_p_x_s) * (p_x_convPr.get(p_x)*revenueP.get(p) - cpcBid);*/
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
	public double getQuantityBoundCoef(double bid, Query query) {
		
		double result = 0;
		
		AbstractPrConversionModel convPr = _convModel.get(query);

		for (Product p : _retailCatalog) {
			for (UserState us : UserState.values()) {
				double p_x_Pr = 0.5; // TODO change this arbitrary constant
				double click_Pr_q_p_x_s = 0.5; // TODO change this arbitrary constant
				double p_x_convPr = convPr.getPrediction(0);
				
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
	public double getQuantityCoefficient(int quantity) {
		
		double result=0;
		
		Query bestQuery = new Query(_manSpecialty,_compSpecialty);	// TODO find a decent query
		AbstractPrConversionModel convPr = _convModel.get(bestQuery); 
		
		for (Product p : _retailCatalog) {
			double revenue = _productRevenue.get(p); 
			for (UserState x : UserState.values()) {
				double p_x_convPr = convPr.getPrediction(0); // TODO find a better model, one that uses p and x
				for (int i=1 ; i < quantity ; i++) {
					double p_x_i_convPr = convPr.getPrediction(i); // TODO find a better model, one that uses p and x
					double p_x_Pr = 0.5; // TODO find a model
					
					result += p_x_Pr * (p_x_convPr - p_x_i_convPr) * revenue;
				}
			}
		}
		return result;
	}

	
	// #########################
	// #### Models Updating ####
	// #########################
	
	@Override
	protected Set<AbstractModel> initModels() {
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		_bidToSlotModels = new HashMap<Query, AbstractBidToSlotModel>();
		_slotToBidModels = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToPrClickModels = new HashMap<Query, AbstractSlotToPrClick>();
		_slotToNumImptModels = new HashMap<Query, AbstractSlotToNumImp>();
		_slotToNumClicks = new HashMap<Query, AbstractSlotToNumClicks>();
		_convModel = new HashMap<Query, AbstractPrConversionModel>();
		AbstractUserModel userModel = new BasicUserModel();
		models.add(userModel);
		_userModel = userModel;
		for(Query query: _querySpace) {
			AbstractBidToSlotModel bidToSlot = new BasicBidToSlot(query,false);
			AbstractSlotToBidModel slotToBid = new BasicSlotToBid(query,false);
			AbstractSlotToPrClick slotToPrClick = new BasicSlotToPrClick(query);
			AbstractSlotToNumImp slotToNumImp = new BasicSlotToNumImp(query,userModel);
			AbstractSlotToNumClicks slotToNumClicks = new BasicSlotToNumClicks(query, slotToPrClick, slotToNumImp);
			AbstractPrConversionModel convModel = new BasicPrConversion(query,DAILYCAPACITY,(int)_lambda);
			models.add(bidToSlot);
			models.add(slotToBid);
			models.add(slotToPrClick);
			models.add(slotToNumImp);
			models.add(slotToNumClicks);
			models.add(convModel);
			_bidToSlotModels.put(query,bidToSlot);
			_slotToBidModels.put(query,slotToBid);
			_slotToPrClickModels.put(query,slotToPrClick);
			_slotToNumImptModels.put(query,slotToNumImp);
			_slotToNumClicks.put(query,slotToNumClicks);
			_convModel.put(query,convModel);
		}
		return models;
	}

	@Override
	protected void updateModels(SalesReport salesReport,
			QueryReport queryReport,
			Set<AbstractModel> models) {

		for(AbstractModel model:models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				userModel.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractBidToSlotModel) {
				AbstractBidToSlotModel bidToSlot = (AbstractBidToSlotModel) model;
				bidToSlot.updateModel(queryReport, salesReport);
			}
//			else if(model instanceof AbstractSlotToBidModel) {
//				AbstractSlotToBidModel slotToBid = (AbstractSlotToBidModel) model;
//				slotToBid.updateModel(queryReport, salesReport);
//			}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				slotToPrClick.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToNumImp) {
				AbstractSlotToNumImp slotToNumImp = (AbstractSlotToNumImp) model;
				slotToNumImp.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToNumClicks) {
				AbstractSlotToNumClicks slotToNumClicks = (AbstractSlotToNumClicks) model;
				slotToNumClicks.updateModel(queryReport, salesReport);
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
		while (q < maxQ) {
			quantity.add(q);
			q += interval;
		}
		return quantity;
	}

}