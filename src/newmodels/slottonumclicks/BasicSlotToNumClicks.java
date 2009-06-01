package newmodels.slottonumclicks;

/**
 * @author jberg
 *
 */

import newmodels.slottonumimp.AbstractSlotToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicSlotToNumClicks extends AbstractSlotToNumClicks {

	private AbstractSlotToNumImp _slotToNumImp;
	private AbstractSlotToPrClick _slotToPrClick;

	public BasicSlotToNumClicks(Query query,
								AbstractSlotToPrClick slotToPrClick,
								AbstractSlotToNumImp slotToNumImp) {
		super(query);
		_slotToPrClick = slotToPrClick;
		_slotToNumImp = slotToNumImp;
	}

	@Override
	public int getPrediction(double slot) {
		double clickPr = _slotToPrClick.getPrediction(slot);
		int numImp = _slotToNumImp.getPrediction(slot);
		return (int) (clickPr * numImp);
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing to do
		return true;
	}

}
