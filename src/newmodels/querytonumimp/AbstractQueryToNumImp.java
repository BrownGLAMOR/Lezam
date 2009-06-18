/**
 * 
 */
package newmodels.querytonumimp;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractQueryToNumImp extends AbstractModel {
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport);
	
	public abstract int getPrediction(Query query);

}
