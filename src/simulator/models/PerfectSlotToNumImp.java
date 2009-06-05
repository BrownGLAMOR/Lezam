package simulator.models;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.slottonumimp.AbstractSlotToNumImp;

public class PerfectSlotToNumImp extends AbstractSlotToNumImp {

	public PerfectSlotToNumImp(Query query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getPrediction(double slot) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}

}
