package newmodels.targeting;

import edu.umich.eecs.tac.props.Query;
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
	
	public BasicTargetModel(String manufacturer, String component) {
		_manufacturer = manufacturer;
		_component = component;
	}
	
	protected double getClickPrPrediction(Query query, double clickPr) {
		return 0.0;
	}
	
	protected double getConvPrPrediction(Query query, double clickPr) {
		return 0.0;
	}
	
	protected double getUSPPrediction(Query query, double clickPr) {
		return 0.0;
	}

}
