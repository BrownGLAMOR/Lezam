/**
 *
 */
package models.querytonumimp;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

/**
 * @author jberg
 */
public abstract class AbstractQueryToNumImp extends AbstractModel {

   public abstract boolean updateModel(QueryReport queryReport,
                                       SalesReport salesReport);

   /*
     * Sometimes you may want to give the conv prob model an explicit bid/pos, but USUALLY not
     */
   public abstract int getPrediction(Query query, int day);


}
