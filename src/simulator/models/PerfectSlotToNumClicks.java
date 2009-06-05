package simulator.models;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.slottonumimp.AbstractSlotToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;

public class PerfectSlotToNumClicks extends AbstractSlotToNumClicks {

	private AbstractSlotToPrClick _clickPrModel;
	private AbstractSlotToNumImp _numImpModel;

	public PerfectSlotToNumClicks(AbstractSlotToPrClick clickPrModel, AbstractSlotToNumImp numImpModel, Query query) {
		super(query);
		_clickPrModel = clickPrModel;
		_numImpModel = numImpModel;
	}

	@Override
	public int getPrediction(double slot) {
		return (int) (_clickPrModel.getPrediction(slot)*_numImpModel.getPrediction(slot));
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
