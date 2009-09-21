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
public abstract class AbstractConversionModel extends AbstractModel {

	
	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bundle);
	
	/*
	 * Sometimes you may want to give the conv prob model an explicit bid/pos, but USUALLY not
	 */
	public abstract double getPrediction(Query query);
	
	public abstract double getPredictionWithBid(Query query, double bid);
	
	public abstract double getPredictionWithPos(Query query, double pos);

}
