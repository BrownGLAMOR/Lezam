/**
 *
 */
package models.postoprclick;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;

/**
 * @author jberg
 */
public abstract class AbstractPosToPrClick extends AbstractModel {

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);

   public abstract double getPrediction(Query query, double position, Ad ad);

   public abstract void setSpecialty(String manufacturer, String component);

   public abstract String toString();

}
