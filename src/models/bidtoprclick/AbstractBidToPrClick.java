/**
 *
 */
package models.bidtoprclick;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;

/**
 * @author jberg
 */
public abstract class AbstractBidToPrClick extends AbstractModel {

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);

   public abstract double getPrediction(Query query, double currentBid, Ad currentAd);

   public abstract void setSpecialty(String manufacturer, String component);

   public abstract String toString();

}
