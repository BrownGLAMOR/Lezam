package modelers.conversionprob;

import java.util.Hashtable;
import java.util.Set;

import modelers.unitssold.UnitsSoldModel;
import edu.umich.eecs.tac.props.Query;

public class ConversionPrModelNoIS implements ConversionPrModel {
	protected static double LAMBDA = 0.995;
	
	/**
	 * calculates the exact conversion assuming a component specialty bonus
	 * 
	 * @param baselinePr base line conversion pr
	 * @param overstock units oversold
	 * @param csb component specialty bonus
	 * @return true conversion pr
	 */
	public static double conversionPr(double baselinePr, int overstock, double csb){
			return eta(baselinePr*inventory_penalty(overstock), 1+csb);
	}
	
	/**
	 * calculates the exact conversion assuming a no component specialty bonus
	 * 
	 * @param baselinePr base line conversion pr
	 * @param overstock units oversold
	 * @param csb component specialty bonus
	 * @return true conversion pr
	 */
	public static double conversionPr(double baselinePr, int overstock){
		return baselinePr*inventory_penalty(overstock);
	}
	
	protected static double inventory_penalty(int overstock){
		return Math.pow(LAMBDA, overstock);
	}
	
	protected static double eta(double p, double x){
		return (p*x)/(p*x + (1-p));
	}
	
	protected int _distributionCapacity;
	protected UnitsSoldModel _unitsSold;
	private Set<Query> _componentSpecialty;
	private double _componentSpecialtyBonus;
	
	protected Hashtable<Query,Double> _baseLineConversion;
	
	public ConversionPrModelNoIS(int distributionCapacity, UnitsSoldModel unitsSold, Hashtable<Query,Double> baseLineConversion, Set<Query> componentSpecialty, double componentSpecialtyBonus){
		_distributionCapacity = distributionCapacity;
		_unitsSold = unitsSold;
		_baseLineConversion = baseLineConversion;
		_componentSpecialty = componentSpecialty;
		_componentSpecialtyBonus = componentSpecialtyBonus;
	}
	
	//public int getOversold(){return _unitsSold.getWindowSold() > _distributionCapacity ? _unitsSold.getWindowSold() - _distributionCapacity : 0;}
	
	public double getCoversionPr(Query q) {
		assert(_baseLineConversion.containsKey(q)) : "no conversion pr was found for a query";
		
		int overstock = 0;
		if(_unitsSold.getWindowSold() > _distributionCapacity){
			overstock = _unitsSold.getWindowSold() - _distributionCapacity;
		}
		
		double conversionPr = 0;
		if(_componentSpecialty.contains(q))
			conversionPr = conversionPr(_baseLineConversion.get(q), overstock, _componentSpecialtyBonus);
		else 
			conversionPr = conversionPr(_baseLineConversion.get(q), overstock);
		
		//useful for testing
		//System.out.println(q+" Baseline Conversion: " + _baseLineConversion.get(q) + " current conversion: " + conversionPr + " overstock: " + overstock);
		
		return conversionPr;
	}
	
}
