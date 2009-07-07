package newmodels.targeting;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;
import newmodels.AbstractModel;


/**
 * This class returns a multiplier for clickPr, convPr, and USP that represents the change if we target.
 * 
 * For F0 we target our specialty
 * For F1 we target the searched term plus our manufacturer or component
 * For F2 we target underlying product for the query
 */

public class BasicTargetModel extends AbstractModel {
	
	String _manufacturer, _component;
	final double TE = 0.5;
	final double CSB = 0.5;
	final double MSB = 0.5;
	final double PSB = 0.5;
	
	public BasicTargetModel(String manufacturer, String component) {
		_manufacturer = manufacturer;
		_component = component;
	}
	
	private class Tuple {

		double _manufacturerRatio;
		double _componentRatio;
		
		public Tuple(double man, double comp){
			_manufacturerRatio = man;
			_componentRatio = comp;
		}
		
		// get methods without the "get", for easier use
		public double manufacturerRatio(){
			return _manufacturerRatio;
		}
		public double componentRatio(){
			return _componentRatio;
		}
	}
	
	private int toBinary(boolean statement){
		if (statement){
			return 1;
		} else {
			return 0;
		}
	}
	
	// for the current version, factors only includes targeting, not promoted slot bonus
	private double eta(double clickPr,double factors){
		return (clickPr*factors)/(clickPr*factors + 1 - clickPr);
	}
	
	private double newUserRatio(double users, double userMultiplier, double nonuserMultiplier){
		return (users*userMultiplier)/(users*userMultiplier + nonuserMultiplier*(1-users));
	}
	
	private double ratio(double newUsers, double oldUsers, double prUsers, double prOthers){
		return (prUsers*newUsers+prOthers*(1-newUsers))/(prUsers*oldUsers+prOthers*(1-oldUsers));
	}
	
	private double higherClickPr(double clickPr, int promoted){
		return eta(clickPr,(1+TE)*(1+promoted*PSB))/eta(clickPr,(1+promoted*PSB));
	}
	
	private double lowerClickPr(double clickPr, int promoted){
		return eta(clickPr,(1-TE)*(1+promoted*PSB))/eta(clickPr,(1+promoted*PSB));
	}
	
	// ratios of all users without targeting
	private Tuple baseUsers(Query query, double clickPr) {
		double man;
		double comp;
		if (query.getManufacturer() == null && query.getComponent() == null){
			man = 1.0/3.0;
			comp = 1.0/3.0;
		} else if (query.getManufacturer() == null){
			man = 1.0/3.0;
			comp = toBinary(_component.equals(query.getComponent()));
		} else if (query.getComponent() == null){
			man = toBinary(_manufacturer.equals(query.getManufacturer()));
			comp = 1.0/3.0;
		} else {
			man = toBinary(_manufacturer.equals(query.getManufacturer()));
			comp = toBinary(_component.equals(query.getComponent()));
		}
		return new Tuple(man,comp);
	}
	
	private Tuple targetedUsers(Query query, double clickPr, int promoted) {
		Tuple base = baseUsers(query, clickPr);
		double man = base.manufacturerRatio();
		double comp = base.componentRatio();
		
		if (query.getManufacturer() == null){
			man = newUserRatio(man,higherClickPr(clickPr,promoted),lowerClickPr(clickPr,promoted));
		}
		if (query.getComponent() == null){
			comp = newUserRatio(comp,higherClickPr(clickPr,promoted),lowerClickPr(clickPr,promoted));
		}
		
		return new Tuple(man,comp);
	}
	
	protected double getClickPrPrediction(Query query, double clickPr, boolean promoted) {
		double ratio;
		
		if (query.getType() == QueryType.FOCUS_LEVEL_TWO){
			ratio = higherClickPr(clickPr, toBinary(promoted));
		} else {
			ratio = 1.0/3.0*higherClickPr(clickPr,toBinary(promoted)) + 2.0/3.0*lowerClickPr(clickPr,toBinary(promoted));
		}
		
		return ratio;
	}
	
	protected double getConvPrPrediction(Query query, double clickPr, double convPr, boolean promoted) {
		return ratio(targetedUsers(query,clickPr,toBinary(promoted)).componentRatio(),baseUsers(query, clickPr).componentRatio(),eta(convPr,1+CSB),convPr);
	}
	
	protected double getUSPPrediction(Query query, double clickPr, boolean promoted) {
		return ratio(targetedUsers(query,clickPr,toBinary(promoted)).manufacturerRatio(), baseUsers(query, clickPr).manufacturerRatio(), 1+MSB, 1);
	}

}
