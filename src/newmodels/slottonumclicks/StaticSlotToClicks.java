package newmodels.slottonumclicks;

import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
import edu.umich.eecs.tac.props.SlotInfo;

public class StaticSlotToClicks extends AbstractSlotToNumClicks{

	double[] estimate;
	
	double prClick;
	double prConversion;
	double prContinue;
	int promotedSlots;
	int slots;
	double targetEffect;
	double promotedSlotBonus;
	
	final double[] intercept = {9.22499, 7.030839, 6.999393};
	final double[] pos = {-1.37441, -1.124158, -1.011630};

	
	public StaticSlotToClicks() {

	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bidBundle) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getPrediction(double slot) {
		// TODO Auto-generated method stub
		return 0;
	}

}
