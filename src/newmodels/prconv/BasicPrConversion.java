package newmodels.prconv;

/**
 * @author jberg
 *
 */

import agents.rules.Constants;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicPrConversion extends AbstractPrConversionModel {

	private int _capacity;
	private int _lambda;
	private double _baselineConv;

	public BasicPrConversion(Query query, int capacity, int lambda) {
		super(query);
		_capacity = capacity;
		_lambda = lambda;
		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			_baselineConv = .1;
		}
		else if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			_baselineConv = .2;
		}
		else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
			_baselineConv = .3;
		}
		else {
			throw new RuntimeException("Malformed query");
		}
	}

	@Override
	public double getPrediction(double sales) {
		double capdiscount = Math.pow(_lambda,Math.max(sales - _capacity, 0));
		return _baselineConv*capdiscount;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing to do
		return true;
	}

}
