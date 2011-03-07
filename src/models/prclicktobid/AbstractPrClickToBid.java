package models.prclicktobid;

/**
 * @author jberg
 *
 */

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

/**
 * @author jberg
 */
public abstract class AbstractPrClickToBid extends AbstractModel {

   public abstract double getPrediction(Query query, double prclick);

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);

   public abstract String toString();

}