/**
 * 
 */
package newmodels.bidtoslot;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractBidToSlotModel extends AbstractModel {
	
	protected Query _query;

	public AbstractBidToSlotModel(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport);
	
	public abstract double getPrediction(double bid);

	public Query getQuery() {
		return _query;
	}
	
}
