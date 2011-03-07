package models.postobid;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

/**
 * @author jberg
 */
public abstract class AbstractPosToBid extends AbstractModel {

   public abstract double getPrediction(Query query, double pos);

   public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);

}