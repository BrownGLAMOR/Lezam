package simulator.models;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;

public class PerfectBidToCPC extends AbstractBidToCPC {

	
	public PerfectBidToCPC(Query query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getPrediction(double bid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}

}
