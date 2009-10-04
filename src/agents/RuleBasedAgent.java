package agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtocpc.EnsembleBidToCPC;
import newmodels.cpctobid.BidToCPCInverter;
import newmodels.prconv.AbstractConversionModel;
import newmodels.prconv.HistoricPrConversionModel;
import newmodels.targeting.BasicTargetModel;

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
	protected final int MAX_TIME_HORIZON = 5;
	protected HashMap<Query, Double> _baselineConversion;
	protected AbstractConversionModel _conversionPrModel;
	protected AbstractBidToCPC _bidToCPCModel = new EnsembleBidToCPC(_querySpace,5,20,true,true);
	protected BidToCPCInverter _CPCToBidModel;
	
	@Override
	public Set<AbstractModel> initModels() {
		HashSet<AbstractModel> models = new HashSet<AbstractModel>();
		_bidToCPCModel = new EnsembleBidToCPC(_querySpace,5,20,true,true);
		_conversionPrModel = new HistoricPrConversionModel(_querySpace, new BasicTargetModel(_manSpecialty,_compSpecialty));
		try {
			_CPCToBidModel = new BidToCPCInverter(new RConnection(), _querySpace, _bidToCPCModel, .05, 0, 3.0);
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		models.add(_bidToCPCModel);
		models.add(_conversionPrModel);
		models.add(_CPCToBidModel);
		
		return models;
	}
	
	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		if( (salesReport != null) && (queryReport != null)) {

			int timeHorizon = (int) Math.min(Math.max(1,_day - 1), MAX_TIME_HORIZON);
			((HistoricPrConversionModel) _conversionPrModel).setTimeHorizon(timeHorizon);
			_conversionPrModel.updateModel(queryReport, salesReport, _bidBundles.get(_bidBundles.size()-2));

			if (_bidBundles.size() > 1) {
				_bidToCPCModel.updateModel(_queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
				_CPCToBidModel.updateModel(_queryReport, salesReport, _bidBundles.get(_bidBundles.size() - 2));
			}
		}
	}
	
	protected void setDailyQueryCapacity(){
		if(_capacity >= HIGH_CAPACITY) {
			_dailyCapacityLambda = 1.3;
		}
		else if(_capacity >= MEDIUM_CAPACITY) {
			_dailyCapacityLambda = 1.5;
		}
		else {
			_dailyCapacityLambda = 1.7;
		}
		_dailyCapacity = _dailyCapacityLambda * (_capacity / _capWindow);
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
