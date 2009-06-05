package newmodels.prconv;

import java.util.Map;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.unitssold.AbstractUnitsSoldModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class SimplePrConversion extends AbstractPrConversionModel{
	private double _lambda;
	private double _baselineConv;
	private double _componentBonus;
	private AbstractUnitsSoldModel _unitsSoldModel;

	public SimplePrConversion(Query query, double baselineConv, double lambda, double componentBonus, AbstractUnitsSoldModel unitsSoldModel) {
		super(query);
		this._lambda = lambda;
		this._baselineConv = baselineConv;
		this._componentBonus = componentBonus;
		this._unitsSoldModel = unitsSoldModel;
	}	

	
	public void setPrediction(double overcap) {
		 double newPrediction = _baselineConv*Math.pow(_lambda,Math.max(overcap, 0));
		 newPrediction = (newPrediction*_componentBonus)/(newPrediction*_componentBonus + 1 - newPrediction);
		 _prediction = newPrediction;
	}


	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		
		return true;
	}
	

}
