/**
 * 
 */
package models.postoprclick;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class AbstractPosToPrClick extends AbstractModel {
	
	public abstract boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle);

	public abstract double getPrediction(Query query, double position, Ad ad);

	public abstract void setSpecialty(String manufacturer, String component);

	public abstract String toString();
	
}
