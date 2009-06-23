/**
 * 
 */
package newmodels.slottobid;

import java.util.HashMap;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractSlotToBidModel extends AbstractModel {
	
	protected Query _query;

	public AbstractSlotToBidModel(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
			SalesReport salesReport,
			BidBundle bidBundle);

	public abstract double getPrediction(double slot);

	public Query getQuery() {
		return _query;
	}
	
}
