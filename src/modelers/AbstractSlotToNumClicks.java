/**
 * 
 */
package modelers;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractSlotToNumClicks extends AbstractModel {
	
	private Query _query;

	public AbstractSlotToNumClicks(Query query) {
		_query = query;
	}
	
	public abstract void updateModel(QueryReport queryReport,
									SalesReport salesReport);
	
	public abstract int getPrediction(double bid);

}
