/**
 * 
 */
package newmodels.bidtoposdist;

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
public abstract class AbstractBidToPosDistModel extends AbstractModel {
	
	
	public abstract double[] getPrediction(Query query, double bid);

	public abstract boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle,
			HashMap<Query, double[]> posDist);
}