/**
 * 
 */
package newmodels.prconv;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractConversionModel extends AbstractModel {

	
	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle);
	

	public abstract double getPrediction(Query query, double bid);

}
