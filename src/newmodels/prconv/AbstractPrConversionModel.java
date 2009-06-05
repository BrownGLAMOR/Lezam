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

	/**
	 * Takes in the amount of sales over the capacity window
	 */
	public abstract void setPrediction(double overcap);
	

	public double getPrediction() {
		return _prediction;
	}

	public Query getQuery() {
		return _query;
	}
	
}
