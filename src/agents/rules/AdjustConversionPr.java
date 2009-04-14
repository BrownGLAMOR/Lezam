package agents.rules;

import java.util.Hashtable;
import java.util.Set;

import modelers.UnitsSoldModel;

import agents.GenericBidStrategy;
import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;

public class AdjustConversionPr extends StrategyTransformation{	
	protected int _distributionCapacity;
	protected UnitsSoldModel _unitsSold;
	private Set<Query> _componentSpecialty;
	
	protected Hashtable<Query,Double> _baseLineConversion;
	
	public AdjustConversionPr(int distributionCapacity, UnitsSoldModel unitsSold, Hashtable<Query,Double> baseLineConversion, Set<Query> componentSpecialty){
		_distributionCapacity = distributionCapacity;
		_unitsSold = unitsSold;
		_baseLineConversion = baseLineConversion;
		_componentSpecialty = componentSpecialty;
	}
	public int getOversold(){return _unitsSold.getWindowSold() > _distributionCapacity ? _unitsSold.getWindowSold() - _distributionCapacity : 0;}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		int overstock = 0;
		if(_unitsSold.getWindowSold() > _distributionCapacity){
			overstock = _unitsSold.getWindowSold() - _distributionCapacity;
		}
		
		double conversionPr = conversion_pr(overstock, _baseLineConversion.get(q), _componentSpecialty.contains(q));
		System.out.println(q+" Baseline Conversion: " + _baseLineConversion.get(q) + " current conversion: " + conversionPr + " overstock: " + overstock);
		if(conversionPr < 0.01){
			conversionPr = 0;
		}
		
		strategy.setProperty(q, SSBBidStrategy.CONVERSION_PR, conversionPr);
	}

	double conversion_pr(int overstock, double baseline, boolean bonus){
		if(bonus)
			return eta(baseline*inventory_penalty(overstock), 1+0.5);
		else
			return baseline*inventory_penalty(overstock);
	}
	
	double inventory_penalty(int overstock){
		return Math.pow(0.995, overstock);
	}
	
	double eta(double p, double x){
		return (p*x)/(p*x + (1-p));
	}
}
