/**
 * 
 */
package models.bidtopos;

import java.util.HashMap;

import models.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractBidToPos extends AbstractModel {
	
	
	public abstract double getPrediction(Query query, double bid);

	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);
	
	public abstract void setNumPromSlots(int numPromSlots);

}