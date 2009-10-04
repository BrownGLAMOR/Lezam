package agents;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.cpctobid.AbstractCPCToBid;
import newmodels.cpctobid.BidToCPCInverter;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.targeting.BasicTargetModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import newmodels.unitssold.UnitsSoldMovingAvg;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
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
	protected AbstractBidToCPC _bidToCPCModel;
	protected AbstractCPCToBid _CPCToBidModel;

	@Override
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		_unitsSoldModel = new UnitsSoldMovingAvg(_querySpace, _capacity, _capWindow);
		_bidToCPCModel = new EnsembleBidToCPC(_querySpace,10,20,true,false);
		_conversionPrModel = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		try {
			_CPCToBidModel = new BidToCPCInverter(new RConnection(), _querySpace, _bidToCPCModel, .05, 0, 3.0);
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		models.add(_unitsSoldModel);
		models.add(_bidToCPCModel);
		models.add(_conversionPrModel);
		models.add(_CPCToBidModel);

		return models;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if(_conversionPrModel instanceof HistoricPrConversionModel) {
			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			((HistoricPrConversionModel) _conversionPrModel).setTimeHorizon(timeHorizon);
		}

		if (_bidBundles.size() > 1 && salesReport != null && queryReport != null) {
			_conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));
			_bidToCPCModel.updateModel(_queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
			_CPCToBidModel.updateModel(_queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
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
			if(model instanceof AbstractBidToCPC) {
				AbstractBidToCPC bidToCPC = (AbstractBidToCPC) model;
				_bidToCPCModel = bidToCPC; 
			}
			if(model instanceof AbstractCPCToBid) {
				AbstractCPCToBid CPCtoBid = (AbstractCPCToBid) model;
				_CPCToBidModel = CPCtoBid; 
			}
		}
	}

	protected void setDailyQueryCapacity(){
		if(_capacity >= HIGH_CAPACITY) {
			_dailyCapacityLambda = 1.7;
		}
		else if(_capacity >= MEDIUM_CAPACITY) {
			_dailyCapacityLambda = 1.5;
		}
		else {
			_dailyCapacityLambda = 1.3;
		}
		_dailyCapacity = _dailyCapacityLambda * (_capacity - _unitsSoldModel.getWindowSold());
		_dailyQueryCapacity = _dailyCapacity / _querySpace.size();
	}

	protected double getDailySpendingLimit(Query q, double targetCPC) {
		if(_conversionPrModel != null) {
			return targetCPC * _dailyQueryCapacity / _conversionPrModel.getPrediction(q);
		}
		else {
			return targetCPC * _dailyQueryCapacity / _baselineConversion.get(q);
		}
	}
}
