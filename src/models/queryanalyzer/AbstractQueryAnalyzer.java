package models.queryanalyzer;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

public abstract class AbstractQueryAnalyzer extends AbstractModel {
	
	
	public abstract int getOrderPrediction(Query query, String adv);
	
	public abstract int getImpressionsPrediction(Query query, String adv);
	
	public abstract int[] getOrderPrediction(Query query);
	
	public abstract int[] getImpressionsPrediction(Query query);

	public abstract boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle);

}
