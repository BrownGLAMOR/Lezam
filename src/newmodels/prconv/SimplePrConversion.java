package newmodels.prconv;

import newmodels.unitssold.AbstractUnitsSoldModel;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class SimplePrConversion extends AbstractPrConversionModel{
	private double _lambda;
	private double _baselineConv;
	private double _componentBonus;
	private AbstractUnitsSoldModel _unitsSoldModel;

	public SimplePrConversion(Query query, AdvertiserInfo advertiserInfo, AbstractUnitsSoldModel unitsSoldModel) {
		super(query);
		this._lambda = advertiserInfo.getDistributionCapacityDiscounter();
		
		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO)
			_baselineConv = .1;
		else if(query.getType() == QueryType.FOCUS_LEVEL_ONE)
			_baselineConv = .2;
		else if(query.getType() == QueryType.FOCUS_LEVEL_TWO)
			_baselineConv = .3;
		
		_componentBonus = 1;
		if (query.getComponent() != null && query.getComponent().equals(advertiserInfo.getComponentSpecialty()))
			_componentBonus = advertiserInfo.getComponentBonus();
		
		this._unitsSoldModel = unitsSoldModel;
	}	

	
	public double getPrediction(double overcap) {
		 double newPrediction = _baselineConv*Math.pow(_lambda,Math.max(overcap, 0));
		 newPrediction = (newPrediction*_componentBonus)/(newPrediction*_componentBonus + 1 - newPrediction);
		 _prediction = newPrediction;
		 return _prediction;
	}


	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}
	

}
