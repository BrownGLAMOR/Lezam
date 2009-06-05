package simulator.models;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.bidtocpc.AbstractBidToCPC;
import newmodels.bidtoslot.AbstractBidToSlotModel;

public class PerfectBidToCPC extends AbstractBidToCPC {

	
	private AbstractBidToSlotModel _bidToSlotModel;

	public PerfectBidToCPC(Query query, AbstractBidToSlotModel bidToSlotModel) {
		super(query);
		_bidToSlotModel = bidToSlotModel;
	}

	@Override
	public double getPrediction(double bid) {
		return getPrediction(bid+1);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return _bidToSlotModel.updateModel(queryReport, salesReport);
	}

}
