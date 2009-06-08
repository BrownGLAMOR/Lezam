package newmodels.prconv;

import com.sun.java.swing.plaf.gtk.GTKConstants.PositionType;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class TrinaryPrConversion extends AbstractPrConversionModel{

	private double _lambda;
	private double _baselineConv;
	private double _componentBonus;
	public enum GetsBonus {YES,NO,MAYBE};
	private GetsBonus _getBonus;

	public TrinaryPrConversion(Query query, double lambda, String componentSpeciality, double componentBonus) {
		super(query);
		_getBonus = GetsBonus.MAYBE;
		_lambda = lambda;
		_componentBonus = componentBonus;
		String component = query.getComponent();
		if(query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			_baselineConv = .1;
		}
		else {
			if(query.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baselineConv = .2;
				if (componentSpeciality.equals(component)) {
					_getBonus = GetsBonus.YES;
				} else if (component == null) _getBonus = GetsBonus.NO;
			} else if(query.getType() == QueryType.FOCUS_LEVEL_TWO) {
				_baselineConv = .3;
				if (componentSpeciality.equals(component)) _getBonus = GetsBonus.YES;
			} else {
				throw new RuntimeException("Malformed query");
			}	
		}		
	}

	@Override
	public double getPrediction(double overcap) {
		double capdiscount = Math.pow(_lambda,Math.max(overcap, 0));
		double firstTerm = _baselineConv * capdiscount;
		double secondTerm = 1 + _componentBonus;
		double nuo = (firstTerm * secondTerm) / (firstTerm * secondTerm + (1 - firstTerm));
		
		if (_getBonus.equals(GetsBonus.YES)) return nuo;
		if (_getBonus.equals(GetsBonus.NO)) return firstTerm;
		
		return (nuo + 3*firstTerm)/4;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}

}
