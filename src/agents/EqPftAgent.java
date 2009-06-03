package agents;

import java.util.Set;

import newmodels.AbstractModel;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class EqPftAgent extends SimAbstractAgent {
	BidBundle _bidBundle;
	
	@Override
	protected void initBidder() {
		
		_bidBundle = new BidBundle();
		
	}

	@Override
	protected BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Set<AbstractModel> initModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateModels(SalesReport salesReport,
			QueryReport queryReport, Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		
	}


}
