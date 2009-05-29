/**
 * 
 */
package modelers;

import usermodel.UserState;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractUserModel extends AbstractModel {
	
	private Query _query;

	public AbstractUserModel(Query query) {
		_query = query;
	}
	
	public abstract void updateModel(QueryReport queryReport,
									SalesReport salesReport);
	
	public abstract double getPrediction(UserState userState);

}
