package models.queryanalyzer;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;

public class CarletonQueryAnalyzer extends AbstractQueryAnalyzer {

	@Override
	public int getImpressionsPrediction(Query query, String adv) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getImpressionsPrediction(Query query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getOrderPrediction(Query query, String adv) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getOrderPrediction(Query query) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
