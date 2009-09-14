/**
 * 
 */
package newmodels.bidtoprclick;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractBidToPrClick extends AbstractModel {
	
	public abstract boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle);

	public abstract double getPrediction(Query query, double currentBid, Ad currentAd);

	public abstract void setSpecialty(String manufacturer, String component);

	public abstract String toString();
	
}
