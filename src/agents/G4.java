package agents;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;

import java.util.HashMap;
import java.util.Set;
import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class G4 extends RuleBasedAgent{
	protected HashMap<Query, Double> _prClick;
	protected BidBundle _bidBundle;
	protected HashMap<Integer, Query> intToQueryMap;
	protected boolean TARGET = false;
	protected boolean BUDGET = false;
	protected double _PPS;
	protected double _alphaIncPPS;
	protected double _betaIncPPS;
	protected double _alphaDecPPS;
	protected double _betaDecPPS;
	protected double _initPPS;
	private IloCplex _cplex;

	public G4() {
		try {
			IloCplex cplex = new IloCplex();
			//			cplex.setOut(null);
			_cplex = cplex;
		} catch (IloException e) {
			//			e.printStackTrace();
		}
	}


	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		buildMaps(models);
		_bidBundle = new BidBundle();
		if(_day < 2) { 
			for(Query q : _querySpace) {
				double bid = getRandomBid(q);
				_bidBundle.addQuery(q, bid, new Ad(), getDailySpendingLimit(q, bid));
			}
			return _bidBundle;
		}


		try {
			double start = System.currentTimeMillis();
			_cplex.clearModel();

			double[] lb = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
			double[] ub = {2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0};
			IloNumVar[] CPCs = _cplex.numVarArray(16, lb, ub);
			IloNumVar PPS = _cplex.numVar(.1, 15);

			for(int i = 0; i < 16; i++) {
				_cplex.addEq(_cplex.prod(getPrConv(intToQueryMap.get(i)), _cplex.diff(_salesPrices.get(intToQueryMap.get(i)), PPS)),CPCs[i]);
			}

			_cplex.addMinimize(_cplex.abs(_cplex.diff(_cplex.sum(_cplex.prod(clickPr[0],CPCs[0]),
					_cplex.prod(clickPr[1],CPCs[1]),
					_cplex.prod(clickPr[2],CPCs[2]),
					_cplex.prod(clickPr[3],CPCs[3]),
					_cplex.prod(clickPr[4],CPCs[4])), numClicks)));

			int lastNumSols = 0;
			while(_cplex.getSolnPoolNsolns() < 10) {
				_cplex.populate();
				if(lastNumSols == _cplex.getSolnPoolNsolns()) {
					break;
				}
				lastNumSols = _cplex.getSolnPoolNsolns();
			}

			//			System.out.println(_cplex.getSolnPoolNsolns());

			double[] solution = new double[6];

			for(int i = 0; i < solution.length; i++) {
				solution[i] = 0.0;
			}

			for(int i = 0; i < _cplex.getSolnPoolNsolns(); i++) {
				//				_cplex.output().println("Solution value  = " + _cplex.getObjValue(i));
				double[] val = _cplex.getValues(CPCs,i);
				for (int j = 0; j < 5; ++j) {
					//					_cplex.output().println("Column: " + j + " Value = " + val[j]);
					solution[j] = solution[j] + val[j];
				}
			}


			/*
			 * If there we no solutions then the solution array is all zeroes,
			 * we return a 1.0 in the 6th index for this (i.e. out of auction)
			 */
			double totImps = 0.0;
			for(int i = 0; i < solution.length; i++) {
				totImps += solution[i];
			}

			if(totImps == 0.0) {
				double[] ans = new double[6];
				for(int i = 0; i < 6; i++) {
					ans[i] = 0.0;
				}
				ans[5] = 1.0;
				return ans;	
			}

			for(int i = 0; i < 5; i++) {
				solution[i] = solution[i] / (_cplex.getSolnPoolNsolns()*1.0);
			}

			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			//			System.out.println("This took " + (elapsed / 1000) + " seconds");
			return solution;
		}
		catch (IloException e) {
			//			e.printStackTrace();
			return null;
		}





















		if (_day > 1 && _salesReport != null && _queryReport != null) {
			/*
			 * Equate PPS
			 */
			double sum = 0.0;
			for(Query query:_querySpace){
				sum+= _salesReport.getConversions(query);
			}

			if(sum <= _dailyCapacity) {
				_PPS *=  (1-(_alphaDecPPS*Math.abs(sum - _dailyCapacity)  +  _betaDecPPS));
			}
			else {
				_PPS *=  (1+_alphaIncPPS*Math.abs(sum - _dailyCapacity)  +  _betaIncPPS);
			}

			if(Double.isNaN(_PPS) || _PPS <= 0) {
				_PPS = _initPPS;
			}
			if(_PPS > 15.0) {
				_PPS = 15.0;
			}
		}

		for(Query query: _querySpace){
			double targetCPC = getTargetCPC(query);
			_bidBundle.setBid(query, targetCPC+.01);

			if (TARGET) {
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getComponent() == null)
					_bidBundle.setAd(query, new Ad(new Product(query.getManufacturer(), _compSpecialty)));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_ONE) && query.getManufacturer() == null)
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
				if (query.getType().equals(QueryType.FOCUS_LEVEL_TWO) && query.getManufacturer().equals(_manSpecialty)) 
					_bidBundle.setAd(query, new Ad(new Product(_manSpecialty, query.getComponent())));
			}
		}

		if(BUDGET) {
			_bidBundle.setCampaignDailySpendLimit(getTotalSpendingLimit(_bidBundle));
		}

		return _bidBundle;
	}

	@Override
	public void initBidder() {
		super.initBidder();
		setDailyQueryCapacity();

		_PPS = _initPPS;

		_prClick = new HashMap<Query, Double>();
		for (Query query: _querySpace) {
			_prClick.put(query, .01);
		}

		intToQueryMap = new HashMap<Integer, Query>();
		int i = 0;
		for(Query q : _querySpace) {
			intToQueryMap.put(i++, q);
		}

	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		super.updateModels(salesReport, queryReport);
		if (_day > 1 && salesReport != null && queryReport != null) {	
			for (Query query: _querySpace) {
				if (queryReport.getImpressions(query) > 0) _prClick.put(query, queryReport.getClicks(query)*1.0/queryReport.getImpressions(query)); 
			}

		}
	}

	protected double getTargetCPC(Query q){		

		double prConv;
		if(_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);

		double rev = _salesPrices.get(q);

		if (TARGET) {
			double clickPr = _prClick.get(q);
			if (clickPr <=0 || clickPr >= 1) clickPr = .5;
			prConv = _targetModel.getConvPrPrediction(q, clickPr, prConv, 0);
			rev = _targetModel.getUSPPrediction(q, clickPr, 0);
		}

		double CPC = (rev - _PPS)* prConv;

		CPC = Math.max(0.0, Math.min(3.5, CPC));

		return CPC;
	}

	protected double getPrConv(Query q) {
		double prConv;
		if(_day <= 6) prConv = _baselineConversion.get(q);
		else prConv = _conversionPrModel.getPrediction(q);
		return prConv;
	}

	@Override
	public String toString() {
		return "EquatePPS";
	}

	@Override
	public AbstractAgent getCopy() {
		return new G4();
	}

}