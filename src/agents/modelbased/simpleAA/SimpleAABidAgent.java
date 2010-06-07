package agents.modelbased.simpleAA;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import models.AbstractModel;
import models.bidtocpc.AbstractBidToCPC;
import models.bidtocpc.EnsembleBidToCPC;
import models.bidtocpc.RegressionBidToCPC;
import models.bidtocpc.WEKAEnsembleBidToCPC;
import models.bidtoprclick.AbstractBidToPrClick;
import models.bidtoprclick.EnsembleBidToPrClick;
import models.bidtoprclick.RegressionBidToPrClick;
import models.bidtoprclick.WEKAEnsembleBidToPrClick;
import models.postoprclick.RegressionPosToPrClick;
import models.prconv.AbstractConversionModel;
import models.prconv.BasicConvPrModel;
import models.prconv.GoodConversionPrModel;
import models.prconv.HistoricPrConversionModel;
import models.querytonumimp.AbstractQueryToNumImp;
import models.querytonumimp.BasicQueryToNumImp;
import models.sales.SalesDistributionModel;
import models.targeting.BasicTargetModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldModel;
import models.unitssold.UnitsSoldMovingAvg;
import models.usermodel.AbstractUserModel;
import models.usermodel.BasicUserModel;
import agents.AbstractAgent;
import agents.AbstractAgent.Predictions;
import edu.brown.cs.aa.algorithms.Decisions;
import edu.brown.cs.aa.algorithms.Solution;
import edu.brown.cs.aa.algorithms.mck.AllDeltasMCKSolver;
import edu.brown.cs.aa.algorithms.mck.DynamicMCKSolver;
import edu.brown.cs.aa.algorithms.mck.ExhaustiveMCKSolver;
import edu.brown.cs.aa.algorithms.mck.MCKSolution;
import edu.brown.cs.aa.algorithms.mck.MCKSolver;
import edu.brown.cs.aa.algorithms.mck.AllDeltasMCKSolver.SolverProperty;
import edu.brown.cs.aa.algorithms.multiday.BenHillClimbingMultiday;
import edu.brown.cs.aa.algorithms.multiday.DPMultiDay;
import edu.brown.cs.aa.algorithms.multiday.MultiDayMCKSolver;
import edu.brown.cs.aa.problem.func.Function;
import edu.brown.cs.aa.problem.Item;
import edu.brown.cs.aa.problem.ItemSet;
import edu.brown.cs.aa.problem.TAC.TACProfitFunction;
import edu.brown.cs.aa.problem.TAC.TACProfitMultiDayFuncion;
import edu.brown.cs.aa.problem.TAC.TACWeightFunction;
import edu.brown.cs.aa.problem.mck.MCKProblem;
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
public class SimpleAABidAgent extends AbstractAgent {


	private static final int MAX_TIME_HORIZON = 5;
	private static final boolean BUDGET = false;
	private static final boolean SAFETYBUDGET = false;

	private double _safetyBudget = 800;

	private Random _R;
	private boolean DEBUG = false;
	private HashMap<Query, Double> _salesPrices;
	private HashMap<Query, Double> _baseConvProbs;
	private HashMap<Query, Double> _baseClickProbs;
	private AbstractUserModel _userModel;
	private AbstractQueryToNumImp _queryToNumImpModel;
	private AbstractBidToCPC _bidToCPC;
	private AbstractBidToPrClick _bidToPrClick;
	private AbstractUnitsSoldModel _unitsSold;
	private AbstractConversionModel _convPrModel;
	private SalesDistributionModel _salesDist;
	private BasicTargetModel _targModel;
	private Hashtable<Query, Integer> _queryId;
	private LinkedList<Double> _bidList;
	private ArrayList<Double> _capList;
	private int lagDays = 4;
	private boolean salesDistFlag;

	public SimpleAABidAgent() {
		//		_R.setSeed(124962748);
		_R = new Random(61686);
		_bidList = new LinkedList<Double>();
		//TODO: Change this back to 0.05
		double bidIncrement  = .05;
		double bidMin = .04;
		double bidMax = 1.65;
		int tot = (int) Math.ceil((bidMax-bidMin) / bidIncrement);
		for(int i = 0; i < tot; i++) {
			_bidList.add(bidMin+(i*bidIncrement));
		}
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
		AbstractUnitsSoldModel unitsSold = new BasicUnitsSoldModel(_querySpace,_capacity,_capWindow);
		BasicTargetModel basicTargModel = new BasicTargetModel(_manSpecialty,_compSpecialty);
		AbstractBidToCPC bidToCPC = new WEKAEnsembleBidToCPC(_querySpace, 10, 10, true, false);
		AbstractBidToPrClick bidToPrClick = new WEKAEnsembleBidToPrClick(_querySpace, 10, 10, basicTargModel, true, true);
		BasicConvPrModel convPrModel = new BasicConvPrModel(userModel, _querySpace, _baseConvProbs);
		models.add(userModel);
		models.add(queryToNumImp);
		models.add(bidToCPC);
		models.add(bidToPrClick);
		models.add(unitsSold);
		models.add(convPrModel);
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
			else if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				_convPrModel = convPrModel;
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
		salesDistFlag = false;
		_baseConvProbs = new HashMap<Query, Double>();
		_baseClickProbs = new HashMap<Query, Double>();

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

			/*
			 * These are the MAX e_q^a (they are randomly generated), which is our clickPr for being in slot 1!
			 * 
			 * Taken from the spec
			 */

			if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				_baseClickProbs.put(q, .3);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baseClickProbs.put(q, .4);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				_baseClickProbs.put(q, .5);
			}
			else {
				throw new RuntimeException("Malformed query");
			}

		}

		_queryId = new Hashtable<Query,Integer>();
		int i = 0;
		for(Query q : _querySpace){
			_queryId.put(q, i);
			i++;
		}

		_capList = new ArrayList<Double>();
		double maxCap = _capacity;
		for(i = 1; i <= maxCap; i+= 10) {
			_capList.add(1.0*i);
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
			else if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
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
			else if(model instanceof BasicTargetModel) {
				//Do nothing
			}
			else {
				//				throw new RuntimeException("Unhandled Model (you probably would have gotten a null pointer later)");
			}
		}
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();
		if(SAFETYBUDGET) {
			bidBundle.setCampaignDailySpendLimit(_safetyBudget);
		}

		buildMaps(models);

		if(_day > 1) {
			if(!salesDistFlag) {
				SalesDistributionModel salesDist = new SalesDistributionModel(_querySpace);
				_salesDist = salesDist;
				salesDistFlag = true;
			}
			_salesDist.updateModel(_salesReport);
		}


		if(_day > lagDays) {

			ArrayList<Integer> soldArrayTMP = ((BasicUnitsSoldModel) _unitsSold).getSalesArray();
			ArrayList<Integer> soldArray = getCopy(soldArrayTMP);

			Integer expectedConvsYesterday = ((BasicUnitsSoldModel) _unitsSold).getExpectedConvsTomorrow();
			if(expectedConvsYesterday == null) {
				expectedConvsYesterday = 0;
				int counter2 = 0;
				for(int j = 0; j < 5 && j < soldArray.size(); j++) {
					expectedConvsYesterday += soldArray.get(soldArray.size()-1-j);
					counter2++;
				}
				expectedConvsYesterday = (int)(expectedConvsYesterday / (double)counter2);
			}
			soldArray.add(expectedConvsYesterday);
			/*
			 * We only want the last 4 days
			 */
			while (soldArray.size() < 4) {
				soldArray.add(0,(int) (_capacity/((double)_capWindow)));
			}

			while(soldArray.size() > 4) {
				soldArray.remove(0);
			}

			double remainingCap;
			if(_day < 4) {
				remainingCap = _capacity/((double)_capWindow);
			}
			else {
				remainingCap = _capacity - _unitsSold.getWindowSold();
			}

			Set<ItemSet> itemSets = new LinkedHashSet<ItemSet>();
			String output = "";
			output += ((int) _day) + "\n";
			output += soldArray + "\n";
			output += _capacity + "\n";
			output += remainingCap + "\n";
			output += _querySpace.size() + "\n\n";

			output += _lambda + "\n";
			output += _CSB + "\n\n";
			int setID = 0;
			for(Query q : _querySpace) {
				output += _bidList.size() + "\n";
				ArrayList<Item> items = new ArrayList<Item>();
				double minClickPr = 0;
				double minCPC = 0;

				double specialty;
				String component = q.getComponent();
				if(_compSpecialty.equals(component)) {
					specialty = 1.0;
				}
				else if(component == null) {
					specialty = 1.0/3.0;
				}
				else {
					specialty = 0.0;
				}

				double convProb = _convPrModel.getPrediction(q);
				if(Double.isNaN(convProb)) {
					convProb = 0.0;
				}

				output += convProb + " " + _salesPrices.get(q) + " " + specialty + "\n";
				for(int i = 0; i < _bidList.size(); i++) {
					double bid = _bidList.get(i);
					double clickPr = _bidToPrClick.getPrediction(q, bid, new Ad());
					double numImps = _queryToNumImpModel.getPrediction(q,(int) (_day+1));
					double CPC = _bidToCPC.getPrediction(q, bid);

					if(Double.isNaN(CPC)) {
						CPC = 0.0;
					}

					if(Double.isNaN(clickPr)) {
						clickPr = 0.0;
					}

					/*
					 * Click probability should always be increasing
					 * with our current models
					 */
					if(clickPr < minClickPr) {
						clickPr = minClickPr;
					}
					else {
						minClickPr = clickPr;
					}

					/*
					 * CPC should always be increasing
					 * with our current models
					 */
					if(CPC < minCPC) {
						CPC = minCPC;
					}
					else {
						minCPC = CPC;
					}

					double numClicks = clickPr*numImps;

					TACWeightFunction weightFunc = new TACWeightFunction(numClicks, convProb, remainingCap, _lambda, specialty, _CSB);
					TACProfitFunction profitFunc = new TACProfitFunction(_salesPrices.get(q), numClicks, CPC, weightFunc);
					output += numClicks + " " + CPC + "\n";
					Item item = new Item(profitFunc,weightFunc,i,setID);
					items.add(item);
				}
				itemSets.add(new ItemSet(items,setID));
				setID++;
				output += "\n";
			}
			
//			System.out.println(output);
			

//			FileOutputStream fout;		
//
//			try
//			{
//				// Open an output stream
//				fout = new FileOutputStream ("TAC.68616.61686.1425." +_day + ".inst");
//
//				// Print a line of text
//				new PrintStream(fout).print(output);
//
//				// Close our output stream
//				fout.close();		
//			}
//			// Catches any error conditions
//			catch (IOException e)
//			{
//				System.err.println ("Unable to write to file");
//				System.exit(-1);
//			}

//			TACProfitMultiDayFuncion multiDayProfitFunc = new TACProfitMultiDayFuncion(soldArray,_capacity,remainingCap,_lambda,(int) _day,15);
//			MCKProblem problem = new MCKProblem(itemSets, remainingCap,multiDayProfitFunc);
			//			System.out.println(problem);

			/*
			 * TODO
			 * Change Solver
			 * 
			 * _capacity is the nominal capacity
			 * 
			 */
			MCKProblem problem = new MCKProblem(itemSets, _capacity, _capacity * 2.0);
			System.out.println("Capacity: " + _capacity);
			MCKProblem[] problems = new MCKProblem[58 - (int)_day];
			for (int i = 0; i < problems.length; i++)
				problems[i] = problem;
			
			double[] sales = new double[4];
			for (int i = 0; i < sales.length; i++)
				sales[i] = soldArray.get(i);
			
			Solution solution;
			if (problems.length == 0)
				solution = new MCKSolution(problem);
			else
			{
//				MultiDayMCKSolver solver = new DPMultiDay(50);
				MultiDayMCKSolver solver = new BenHillClimbingMultiday(20);
				solution = solver.solve(problems, sales).get(0);
			}
			
			
//			MCKSolver solver = new DynamicMCKSolver();
//			Solution solution = solver.solve(problem);
			
			Decisions decisions = solution.getDecisions();

			Iterator<ItemSet> iterator = itemSets.iterator();
			for(Query q : _querySpace) {
				ItemSet items = iterator.next();
				Collection<Item> itemSet = items.getItems();
				int bidIdx = -1;
				int counter = 0;
				for(Item item : itemSet) {
					if(solution.isTakingItem(item) && 1 - decisions.getAmountTaken(item) < .05) { //take the item if we took at least 95% of it
						bidIdx = counter;
						break;
					}
					counter++;
				}

				if(bidIdx >= 0) {
					bidBundle.addQuery(q, _bidList.get(bidIdx), new Ad());
				}
				else {
					double bid = randDouble(.04,_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .7);
					bidBundle.addQuery(q, bid, new Ad(), bid*5);
				}
			}

			/*
			 * Pass expected conversions to unit sales model
			 */
			double solutionWeight = solution.getCurrentWeight();
			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
		}
		else {
			for(Query q : _querySpace){
				if(_compSpecialty.equals(q.getComponent()) || _manSpecialty.equals(q.getManufacturer())) {
					double bid = randDouble(_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .35, _salesPrices.get(q) * getConversionPrWithPenalty(q,1.0)* _baseClickProbs.get(q) * .65);
					bidBundle.addQuery(q, bid, new Ad(), Double.MAX_VALUE);
				}
				else {
					double bid = randDouble(.04,_salesPrices.get(q) * getConversionPrWithPenalty(q,1.0) * _baseClickProbs.get(q) * .65);
					bidBundle.addQuery(q, bid, new Ad(), bid*10);
				}
			}
			bidBundle.setCampaignDailySpendLimit(800);
		}
		System.out.println(bidBundle);
		return bidBundle;
	}
	
	private ArrayList<Integer> getCopy(ArrayList<Integer> soldArrayTMP) {
		ArrayList<Integer> soldArray = new ArrayList<Integer>(soldArrayTMP.size());
		for(int i = 0; i < soldArrayTMP.size(); i++) {
			soldArray.add(soldArrayTMP.get(i));
		}
		return soldArray;
	}

	public double getConversionPrWithPenalty(Query q, double penalty) {
		double convPr;
		String component = q.getComponent();
		double pred = _convPrModel.getPrediction(q);
		if(_compSpecialty.equals(component)) {
			convPr = eta(pred*penalty,1+_CSB);
		}
		else if(component == null) {
			convPr = eta(pred*penalty,1+_CSB) * (1/3.0) + pred*penalty*(2/3.0);
		}
		else {
			convPr = pred*penalty;
		}
		return convPr;
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

	@Override
	public String toString() {
		return "SimpleAABidAgent";
	}

	@Override
	public AbstractAgent getCopy() {
		return new SimpleAABidAgent();
	}
}
