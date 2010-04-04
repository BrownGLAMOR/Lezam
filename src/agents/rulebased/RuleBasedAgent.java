package agents.rulebased;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import agents.AbstractAgent;

import models.AbstractModel;
import models.cpctobid.AbstractCPCToBid;
import models.cpctobid.WEKACPCToBid;
import models.prconv.AbstractConversionModel;
import models.prconv.HistoricPrConversionModel;
import models.targeting.BasicTargetModel;
import models.unitssold.AbstractUnitsSoldModel;
import models.unitssold.BasicUnitsSoldLambdaModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public abstract class RuleBasedAgent extends AbstractAgent {
	protected double _dailyCapacity;
	protected final int HIGH_CAPACITY = 500;
	protected final int MEDIUM_CAPACITY = 400;
	protected final int LOW_CAPACITY = 300;
	protected final int MAX_TIME_HORIZON = 5;
	protected AbstractUnitsSoldModel _unitsSoldModel; 
	protected AbstractConversionModel _conversionPrModel;
	protected BasicTargetModel _targetModel;
	protected AbstractCPCToBid _CPCToBid;
	protected Random _R;
	protected long seed = 61686;
	protected boolean SEEDED = true;

	protected HashMap<Query, Double> _baselineConversion;
	protected HashMap<Query, Double> _baseClickProbs;
	protected HashMap<Query, Double> _salesPrices;
	protected HashMap<Query, Double> _salesDistribution;

	protected double _lambdaCapLow;
	protected double _lambdaCapMed;
	protected double _lambdaCapHigh;
	protected double _lambdaCapModifier;
	protected double _lambdaBudgetLow;
	protected double _lambdaBudgetMed;
	protected double _lambdaBudgetHigh;
	protected double _lambdaBudgetModifier;
	protected double _dailyCapMin;
	protected double _dailyCapMax;

	/*
	 * 1/21/10
	 * 
	 * I made several methods final in this class that should be
	 * virtual.  I did this for testing, so if you want to take
	 * them out just let me know.
	 * 
	 * jberg
	 * 
	 */

	@Override
	public void initBidder() {

		_R = new Random();

		if(SEEDED) {
			_R.setSeed(seed);
		}

		_baselineConversion = new HashMap<Query, Double>();
		_baseClickProbs = new HashMap<Query, Double>();
		_salesDistribution = new HashMap<Query, Double>();

		if(_capacity == 300) {
			_lambdaCapModifier = _lambdaCapLow;
			_lambdaBudgetModifier = _lambdaBudgetLow;
		}
		else if(_capacity == 400) {
			_lambdaCapModifier = _lambdaCapMed;
			_lambdaBudgetModifier = _lambdaBudgetMed;
		}
		else {
			_lambdaCapModifier = _lambdaCapHigh;
			_lambdaBudgetModifier = _lambdaBudgetHigh;
		}

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
				_baselineConversion.put(q, _piF0);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baselineConversion.put(q, _piF1);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				_baselineConversion.put(q, _piF2);
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
				_baselineConversion.put(q,eta(_baselineConversion.get(q),1+_CSB));
			}
			else if(component == null) {
				_baselineConversion.put(q,eta(_baselineConversion.get(q),1+_CSB)*(1/3.0) + _baselineConversion.get(q)*(2/3.0));
			}


			_salesDistribution.put(q, 1.0/_querySpace.size());
		}

		setDailyQueryCapacity();
	}

	@Override
	public final Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		_unitsSoldModel = new BasicUnitsSoldLambdaModel(_querySpace, _capacity, _capWindow,1.0);
		_conversionPrModel = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		_targetModel = new BasicTargetModel(_manSpecialty, _compSpecialty);
		_CPCToBid = new WEKACPCToBid(3,1,20);

		models.add(_unitsSoldModel);
		models.add(_conversionPrModel);
		models.add(_targetModel);
		models.add(_CPCToBid);
		return models;
	}

	@Override
	public final void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if(_conversionPrModel instanceof HistoricPrConversionModel) {
			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			((HistoricPrConversionModel) _conversionPrModel).setTimeHorizon(timeHorizon);
		}

		if (_bidBundles.size() > 1 && salesReport != null && queryReport != null) {
			_unitsSoldModel.update(salesReport);
			setDailyQueryCapacity();
			_conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			_CPCToBid.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
		}
	}

	protected final void buildMaps(Set<AbstractModel> models) {
		for(AbstractModel model : models) {
			if(model instanceof AbstractUnitsSoldModel) {
				AbstractUnitsSoldModel unitsSold = (AbstractUnitsSoldModel) model;
				_unitsSoldModel = unitsSold;
			}
			else if(model instanceof AbstractConversionModel) {
				AbstractConversionModel convPrModel = (AbstractConversionModel) model;
				_conversionPrModel = convPrModel;
			}
			if(model instanceof BasicTargetModel) {
				BasicTargetModel targModel = (BasicTargetModel) model;
				_targetModel = targModel; 
			}
			if(model instanceof AbstractCPCToBid) {
				AbstractCPCToBid CPCToBid = (AbstractCPCToBid) model;
				_CPCToBid = CPCToBid; 
			}
		}
	}

	protected final void setDailyQueryCapacity(){
		if(_day < 5 ){
			_dailyCapacity = (_capacity/((double)_capWindow));
		}
		else {
			double avgCap = (_capacity/((double)_capWindow));
			_dailyCapacity = Math.max(avgCap*_dailyCapMin,Math.min(avgCap*_dailyCapMax, _capacity - _unitsSoldModel.getWindowSold())) * _lambdaCapModifier;
//			_dailyCapacity = Math.max(avgCap*_dailyCapMin,Math.min(avgCap*_dailyCapMax, _capacity * _lambdaCapModifier- _unitsSoldModel.getWindowSold()));
		}
	}

	protected final double getDailySpendingLimit(Query q, double targetCPC) {
		if(_day >= 6 && _conversionPrModel != null) {
			return ((targetCPC * _salesDistribution.get(q)*_dailyCapacity) / _conversionPrModel.getPrediction(q))*_lambdaBudgetModifier;
		}
		else {
			return ((targetCPC * _salesDistribution.get(q)*_dailyCapacity) / _baselineConversion.get(q))*_lambdaBudgetModifier;
		}
	}

	protected final double getTotalSpendingLimit(BidBundle bundle) {
		double targetCPC = 0;
		double convPr = 0;
		int numQueries = 0;
		for(Query q : _querySpace) {
			if(!Double.isNaN(bundle.getBid(q)) && bundle.getBid(q) > 0) {
				targetCPC += bundle.getBid(q);
				if(_day >= 6 && _conversionPrModel != null) {
					convPr += _conversionPrModel.getPrediction(q);
				}
				else {
					convPr += _baselineConversion.get(q);
				}
				numQueries++;
			}
		}
		targetCPC /= numQueries;
		convPr /= numQueries;
		return (targetCPC*(_dailyCapacity/convPr))*_lambdaBudgetModifier;
	}

	protected final double getBidFromCPC(Query query, double cpc) {
		if(_day >= 6) {
			return _CPCToBid.getPrediction(query, cpc);
		}
		else {
			return cpc + .01;
		}
	}

	protected final double getRandomBid(Query q) {
		return randDouble(.04,_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9);
	}

	private final double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

}
