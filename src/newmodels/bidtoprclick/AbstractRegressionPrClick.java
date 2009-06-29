/**
 * 
 */
package newmodels.bidtoprclick;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractRegressionPrClick extends AbstractModel {
	
	public abstract boolean updateModel(QueryReport queryReport, BidBundle bundle);
	
	public abstract double getPrediction(Query query, double bid, Ad ad, BidBundle bundle);

}
