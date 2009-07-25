/**
 * 
 */
package newmodels.prconv;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public abstract class NewAbstractConversionModel extends AbstractModel {

	
	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle);
	

	public abstract double getPrediction(Query query, double bid);


	public void setTimeHorizon(int timeHorizon) {
		// TODO Auto-generated method stub
		
	}

}
