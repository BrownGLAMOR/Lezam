package newmodels.slottocpc;

import java.util.HashMap;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * 
 * @author ml63
 *
 */
public abstract class AbstractSlotToCPCModel extends AbstractModel {

	protected Query _query;

	public AbstractSlotToCPCModel(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport,
									HashMap<Query,Double> lastBid);
	
	public abstract double getPrediction(double slot);
	
	public Query getQuery() {
		return _query;
	}

}
