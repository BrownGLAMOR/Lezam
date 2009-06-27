
package newmodels.bidtocpc;

/**
 * @author jberg
 *
 */

import newmodels.AbstractModel;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractBidToCPC extends AbstractModel {
	
	public AbstractBidToCPC() {
	}
	
	/**
	public abstract boolean updateModel(QueryReport queryReport,
									SalesReport salesReport);
	 * @throws REXPMismatchException 
	 * @throws RserveException 
	 * @throws REngineException 
	*/
	
	public abstract double getPrediction(Query query, double bid, BidBundle bidbundle);

	public abstract boolean updateModel(QueryReport queryreport, BidBundle bidbundle);
	
}