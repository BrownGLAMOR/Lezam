/**
 * 
 */
package modelers;

import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractModel {
	
	public abstract void updateModel(QueryReport queryReport, SalesReport salesReport, Object otherInfo);
	
	public abstract Object getPrediction(Object info);

}
