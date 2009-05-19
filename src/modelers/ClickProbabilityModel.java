package modelers;

import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

/* 
 * note: currently assumes no informational searchers or promoted slot bonus
 * currently has constants that could possibly change with the server
 */

public class ClickProbabilityModel {

	private final double MSB = 0.5;
	private final double CSB = 0.5;
	private final double F0_CONVERSION_BASELINE = 0.1;
	private final double F1_CONVERSION_BASELINE = 0.2;
	private final double F2_CONVERSION_BASELINE = 0.3;
	
	private HashMap<Query,Double> clickProbability_;
	private HashMap<Query,Double> clickRevenue_;

	private double eta(Query q, String componentSpecialty, String manufacturerSpecialty){
		double p;
		double x;

		if (q.getComponent().equals(componentSpecialty)){
			x = 1 + CSB;
		} else {
			x = 1;
		}

		if (q.getManufacturer().equals(manufacturerSpecialty)){
			x *= (1 + MSB);
		}

		if(q.getType() == QueryType.FOCUS_LEVEL_ZERO){
			p = F0_CONVERSION_BASELINE;
		} else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
			p = F1_CONVERSION_BASELINE;
		} else if(q.getType() == QueryType.FOCUS_LEVEL_TWO){
			p = F2_CONVERSION_BASELINE;
		} else {
			System.out.println("Model ClickProbability: Eta failed on: query type");
			p = F2_CONVERSION_BASELINE; // most common
		}

		return (p*x)/(p*x + (1-p));
	}

	public ClickProbabilityModel(Set<Query> querySpace, String componentSpecialty, String manufacturerSpecialty){
		clickProbability_ = new HashMap<Query,Double>();
		for (Query q: querySpace){
			clickProbability_.put(q, eta(q,componentSpecialty,"nullXnull"));
			clickRevenue_.put(q, eta(q,componentSpecialty,manufacturerSpecialty));
		}
	}
	
	public HashMap<Query,Double> getClickProbability(){
		return clickProbability_;
	}
	
	public HashMap<Query,Double> getClickRevenue(){
		return clickRevenue_;
	}
}
