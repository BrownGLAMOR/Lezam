package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.WEKAEnsembleBidToCPC;
import newmodels.bidtoprclick.AbstractBidToPrClick;
import newmodels.bidtoprclick.WEKAEnsembleBidToPrClick;
import newmodels.cpctobid.AbstractCPCToBid;
import newmodels.cpctobid.BidToCPCInverter;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.GoodConversionPrModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.querytonumimp.BasicQueryToNumImp;
import newmodels.sales.SalesDistributionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.BasicUnitsSoldModel;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;
import agents.AbstractAgent.Predictions;
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
 * @author jberg, spucci, vnarodit
 *
 */
public class G4 extends AbstractAgent {

	private int MAX_TIME_HORIZON = 5;
	private boolean SAFETYBUDGET = true;
	private boolean TARGET = false;
	private boolean BUDGET = false;

	private double _safetyBudget = 800;

	private Random _R = new Random();
	private boolean DEBUG = false;
	private double LAMBDA = .995;
	private HashMap<Query, Double> _salesPrices;
	private HashMap<Query, Double> _baseConvProbs;
	private HashMap<Query, Double> _baseClickProbs;
	private AbstractUserModel _userModel;
	private AbstractQueryToNumImp _queryToNumImpModel;
	private AbstractBidToCPC _bidToCPC;
	private AbstractCPCToBid _CPCToBid;
	private AbstractBidToPrClick _bidToPrClick;
	private AbstractUnitsSoldModel _unitsSold;
	private AbstractConversionModel _convPrModel;
	private SalesDistributionModel _salesDist;
	private BasicTargetModel _targModel;
	private int lagDays = 5;
	private boolean salesDistFlag;

	public G4() {
		_R.setSeed(124962748);
		salesDistFlag = false;
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
		GoodConversionPrModel convPrModel = new GoodConversionPrModel(_querySpace,basicTargModel);
		AbstractCPCToBid CPCToBid = null;
		try {
			CPCToBid = new BidToCPCInverter(new RConnection(), _querySpace, bidToCPC, .05, 0, 3.0);
		} catch (RserveException e) {
			e.printStackTrace();
		}


		models.add(userModel);
		models.add(queryToNumImp);
		models.add(bidToCPC);
		models.add(bidToPrClick);
		models.add(unitsSold);
		models.add(convPrModel);
		models.add(basicTargModel);
		models.add(CPCToBid);
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
			else if(model instanceof AbstractCPCToBid) {
				AbstractCPCToBid CPCToBid = (AbstractCPCToBid) model;
				_CPCToBid = CPCToBid; 
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

			String component = q.getComponent();
			if(_compSpecialty.equals(component)) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB));
			}
			else if(component == null) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB)*(1/3.0) + _baseConvProbs.get(q)*(2/3.0));
			}
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
		double start = System.currentTimeMillis();
		BidBundle bidBundle = new BidBundle();

		if(SAFETYBUDGET) {
			bidBundle.setCampaignDailySpendLimit(_safetyBudget);
		}

		if(_day > 1) {
			if(!salesDistFlag) {
				SalesDistributionModel salesDist = new SalesDistributionModel(_querySpace);
				_salesDist = salesDist;
				salesDistFlag = true;
			}
			_salesDist.updateModel(_salesReport);
		}

		if(_day > lagDays){
			buildMaps(models);
			double budget = _capacity/_capWindow;
			if(_day < 4) {
				//do nothing
			}
			else {
				budget = Math.max(20,_capacity*(2.0/5.0) - _unitsSold.getWindowSold()/4);
				//				budget = _capacity - _unitsSold.getWindowSold();
				debug("Unit Sold Model Budget "  +budget);
			}

			if(budget > 0) {

				HashMap<Query,Double> convPr = new HashMap<Query, Double>();
				for(Query query : _querySpace) {
					convPr.put(query, _convPrModel.getPrediction(query));
				}

				HashMap<Query,Integer> numImps = new HashMap<Query, Integer>();
				for(Query query : _querySpace) {
					numImps.put(query, _queryToNumImpModel.getPrediction(query, (int)_day));
				}

				HashMap<Query,Double> targCPCs = new HashMap<Query, Double>();
				double initPPS = 8.0;
				double incrementPPS = .05;
				double PPS = initPPS;
				double threshold = 2;
				int numIter = 0;
				while(true) {
					double targetSales = 0.0;
					for(Query q : _querySpace) {
						double targCPC = (_salesPrices.get(q)-PPS)*convPr.get(q);
						targCPCs.put(q, targCPC);
						targetSales += Math.max(0,numImps.get(q)*_bidToPrClick.getPrediction(q, targCPC, null)*convPr.get(q));
					}

					if(Math.abs(targetSales-budget) < threshold) {
						break;
					}

					if(PPS > 15) {
						numIter = -1;
						threshold += 1;
						incrementPPS *= .5;
					}

					PPS += incrementPPS;
					numIter++;
				}

				if(BUDGET) {
					for(Query q : _querySpace) {
						double bid = targCPCs.get(q);
						bidBundle.addQuery(q, bid,null,numImps.get(q)*_bidToPrClick.getPrediction(q, bid, null)*targCPCs.get(q));
					}
				}
				else {
					for(Query q : _querySpace) {
						bidBundle.addQuery(q, targCPCs.get(q), null);
					}
				}

				/*
				 * Pass expected conversions to unit sales model
				 */
				//			double solutionWeight = solutionWeight(budget,solution,allPredictionsMap);
				//			((BasicUnitsSoldModel)_unitsSold).expectedConvsTomorrow((int) solutionWeight);
			}
		}
		else {
			for(Query q : _querySpace){
				double bid = 0.0;
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
				else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
				else
					bid = randDouble(.04,_salesPrices.get(q) * _baseConvProbs.get(q) * _baseClickProbs.get(q) * .8);
				bidBundle.addQuery(q, bid, new Ad(), Double.NaN);
			}
		}
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
		return bidBundle;
	}

	private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap, BidBundle bidBundle) {
		double threshold = 2;
		int maxIters = 10;
		double lastSolWeight = Double.MAX_VALUE;
		double solutionWeight = 0.0;

		/*
		 * As a first estimate use the weight of the solution
		 * with no penalty
		 */
		for(Query q : _querySpace) {
			if(solution.get(q) == null) {
				continue;
			}
			Predictions predictions = allPredictionsMap.get(q).get(solution.get(q).idx());
			double dailyLimit = Double.NaN;
			if(bidBundle != null) {
				dailyLimit  = bidBundle.getDailyLimit(q);
			}
			double clickPr = predictions.getClickPr();
			double numImps = predictions.getNumImp();
			int numClicks = (int) (clickPr * numImps);
			double CPC = predictions.getCPC();
			double convProb = predictions.getConvPr();

			if(Double.isNaN(CPC)) {
				CPC = 0.0;
			}

			if(Double.isNaN(clickPr)) {
				clickPr = 0.0;
			}

			if(Double.isNaN(convProb)) {
				convProb = 0.0;
			}

			if(!Double.isNaN(dailyLimit)) {
				if(numClicks*CPC > dailyLimit) {
					numClicks = (int) (dailyLimit/CPC);
				}
			}

			solutionWeight += numClicks*convProb;
		}

		double originalSolWeight = solutionWeight;

		int numIters = 0;
		while(Math.abs(lastSolWeight-solutionWeight) > threshold) {
			numIters++;
			if(numIters > maxIters) {
				numIters = 0;
				solutionWeight = (_R.nextDouble() + .5) * originalSolWeight; //restart the search
				threshold += 1; //increase the threshold
			}
			lastSolWeight = solutionWeight;
			solutionWeight = 0;
			double penalty;
			double numOverCap = lastSolWeight - budget;
			if(budget < 0) {
				penalty = 0.0;
				int num = 0;
				for(double j = Math.abs(budget)+1; j <= numOverCap; j++) {
					penalty += Math.pow(LAMBDA, j);
					num++;
				}
				penalty /= (num);
			}
			else {
				if(numOverCap <= 0) {
					penalty = 1.0;
				}
				else {
					penalty = budget;
					for(int j = 1; j <= numOverCap; j++) {
						penalty += Math.pow(LAMBDA, j);
					}
					penalty /= (budget + numOverCap);
				}
			}
			if(Double.isNaN(penalty)) {
				penalty = 1.0;
			}
			for(Query q : _querySpace) {
				if(solution.get(q) == null) {
					continue;
				}
				Predictions predictions = allPredictionsMap.get(q).get(solution.get(q).idx());
				double dailyLimit = Double.NaN;
				if(bidBundle != null) {
					dailyLimit  = bidBundle.getDailyLimit(q);
				}
				double clickPr = predictions.getClickPr();
				double numImps = predictions.getNumImp();
				int numClicks = (int) (clickPr * numImps);
				double CPC = predictions.getCPC();
				double convProb = predictions.getConvPr() * penalty;

				if(Double.isNaN(CPC)) {
					CPC = 0.0;
				}

				if(Double.isNaN(clickPr)) {
					clickPr = 0.0;
				}

				if(Double.isNaN(convProb)) {
					convProb = 0.0;
				}

				if(!Double.isNaN(dailyLimit)) {
					if(numClicks*CPC > dailyLimit) {
						numClicks = (int) (dailyLimit/CPC);
					}
				}

				solutionWeight += numClicks*convProb;
			}
		}
		return solutionWeight;
	}

	private double solutionWeight(double budget, HashMap<Query, Item> solution, HashMap<Query, ArrayList<Predictions>> allPredictionsMap) {
		return solutionWeight(budget, solution, allPredictionsMap, null);
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
		return "G4";
	}

	@Override
	public AbstractAgent getCopy() {
		return new G4();
	}
}
