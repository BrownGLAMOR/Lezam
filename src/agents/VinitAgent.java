package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class VinitAgent extends AbstractAgent {

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initBidder() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<AbstractModel> initModels() {
		return null;
	}
	
	@Override
	public AbstractAgent getCopy() {
		return new VinitAgent();
	}

	@Override
	public String toString() {
		return "VinitAgent";
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
	}

}
