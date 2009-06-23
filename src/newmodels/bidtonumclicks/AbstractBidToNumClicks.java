/**
 * 
 */
package newmodels.bidtonumclicks;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractBidToNumClicks extends AbstractModel {
	
	protected Query _query;

	public AbstractBidToNumClicks(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport, BidBundle bidBundle);
	
	public abstract int getPrediction(double bid);

	
	public Query getQuery() {
		return _query;
	}
}
