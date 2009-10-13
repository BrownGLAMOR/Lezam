package agents;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.cpctobid.AbstractCPCToBid;
import newmodels.cpctobid.BidToCPCInverter;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.BasicUnitsSoldLambdaModel;
import newmodels.unitssold.BasicUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public abstract class RuleBasedAgent extends AbstractAgent {
	protected double _dailyCapacity;
	protected double _dailyQueryCapacity;
	protected double _dailyCapacityLambda;
	protected final int HIGH_CAPACITY = 500;
	protected final int MEDIUM_CAPACITY = 400;
	protected final int LOW_CAPACITY = 300;
	protected final int MAX_TIME_HORIZON = 5;
	protected AbstractUnitsSoldModel _unitsSoldModel; 
	protected HashMap<Query, Double> _baselineConversion;
	protected AbstractConversionModel _conversionPrModel;
	protected BasicTargetModel _targetModel;
	protected HashMap<Query, Double> _baseClickProbs;
	protected HashMap<Query, Double> _salesPrices;
	protected Random _R;
	protected double budgetModifier = 1.2;

	@Override
	public void initBidder() {

		_R = new Random();
//		_R.setSeed(12452748);

		_baselineConversion = new HashMap<Query, Double>();
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
		}
	}

	@Override
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		setDailyQueryCapacity();
		_unitsSoldModel = new BasicUnitsSoldLambdaModel(_querySpace, _capacity, _capWindow,_dailyCapacityLambda);
		_conversionPrModel = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		_targetModel = new BasicTargetModel(_manSpecialty, _compSpecialty);

		models.add(_unitsSoldModel);
		models.add(_conversionPrModel);
		models.add(_targetModel);
		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if(_conversionPrModel instanceof HistoricPrConversionModel) {
			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			((HistoricPrConversionModel) _conversionPrModel).setTimeHorizon(timeHorizon);
		}

		if (_bidBundles.size() > 1 && salesReport != null && queryReport != null) {
			_unitsSoldModel.update(salesReport);
			setDailyQueryCapacity();
			_conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
		}
	}

	protected void buildMaps(Set<AbstractModel> models) {
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
		}
	}

	protected void setDailyQueryCapacity(){
//		_dailyCapacityLambda = 1.4;
		if(_capacity >= HIGH_CAPACITY) {
			_dailyCapacityLambda = 1.15;
		}
		else if(_capacity >= MEDIUM_CAPACITY) {
			_dailyCapacityLambda = 1.25;
		}
		else {
			_dailyCapacityLambda = 1.35;
		}
		if(_day < 5 ){
			_dailyCapacity = _dailyCapacityLambda * (_capacity/((double)_capWindow));
		}
		else {
			//We do the max to ensure some degree of smoothness
			_dailyCapacity = Math.max((_capacity/((double)_capWindow)) * (2/3.0),_dailyCapacityLambda * _capacity - _unitsSoldModel.getWindowSold());
		}
		_dailyQueryCapacity = _dailyCapacity / _querySpace.size();
	}

	protected double getDailySpendingLimit(Query q, double targetCPC) {
		if(_day >= 6 && _conversionPrModel != null) {
			return budgetModifier*targetCPC * _dailyQueryCapacity / _conversionPrModel.getPrediction(q);
		}
		else {
			return budgetModifier*targetCPC * _dailyQueryCapacity / _baselineConversion.get(q);
		}
	}

	protected double getTotalSpendingLimit(BidBundle bundle) {
		double targetCPC = 0;
		double convPr = 0;
		for(Query q : _querySpace) { 
			targetCPC += bundle.getBid(q);
			if(_day >= 6 && _conversionPrModel != null) {
				convPr += _conversionPrModel.getPrediction(q);
			}
			else {
				convPr += _baselineConversion.get(q);
			}
		}
		targetCPC /= _querySpace.size();
		convPr /= _querySpace.size();
		return budgetModifier*targetCPC*_dailyCapacity/convPr;
	}

	protected double getRandomBid(Query q) {
		double bid = 0.0;
		if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
			bid = randDouble(.04,_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9);
		else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
			bid = randDouble(.04,_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9);
		else
			bid = randDouble(.04,_salesPrices.get(q) * _baselineConversion.get(q) * _baseClickProbs.get(q) * .9);
		return bid;
	}

	private double randDouble(double a, double b) {
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

}
