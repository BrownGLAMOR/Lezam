/**
 * 
 */
package newmodels.bidtoslot;

import java.util.HashMap;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractBidToSlotModel extends AbstractModel {
	
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport);
	
	public abstract double getPrediction(Query query, double bid);

	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, HashMap<Query,Double> lastBids) {
		return false;
	}
}