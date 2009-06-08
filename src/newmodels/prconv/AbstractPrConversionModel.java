/**
 * 
 */
package newmodels.prconv;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractPrConversionModel extends AbstractModel {
	
	protected Query _query;
	protected double _prediction;
	
	public AbstractPrConversionModel(Query query) {
		_query = query;
	}
	
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport);
	

	public abstract double getPrediction(double overcap);

	public Query getQuery() {
		return _query;
	}
	
}
