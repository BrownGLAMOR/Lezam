/**
 * 
 */
package newmodels.slottonumimp;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractSlotToNumImp extends AbstractModel {
	
	protected Query _query;

	public AbstractSlotToNumImp(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport);
	
	public abstract int getPrediction(double slot);

	public Query getQuery() {
		return _query;
	}
	
}
