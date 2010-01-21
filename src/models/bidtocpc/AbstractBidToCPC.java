
package models.bidtocpc;

/**
 * @author jberg
 *
 */

import models.AbstractModel;

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
public abstract class AbstractBidToCPC extends AbstractModel {
	
	public abstract double getPrediction(Query query, double bid);

	public abstract boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle);

	public abstract String toString();

}