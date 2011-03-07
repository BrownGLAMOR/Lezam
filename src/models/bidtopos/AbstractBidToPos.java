/**
 *
 */
package models.bidtopos;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

/**
 * @author jberg
 */
public abstract class AbstractBidToPos extends AbstractModel {


   public abstract double getPrediction(Query query, double bid);

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);

   public abstract void setNumPromSlots(int numPromSlots);

}