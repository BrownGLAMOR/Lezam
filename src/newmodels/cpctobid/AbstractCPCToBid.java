
package newmodels.cpctobid;

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
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractCPCToBid extends AbstractModel {
	
	public abstract double getPrediction(Query query, double cpc);

	public abstract boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle);

	public abstract String toString();

}