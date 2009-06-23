package simulator.models;


/**
 * @author jberg
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import simulator.AgentBidPair;
import simulator.SimAgent;
import usermodel.UserState;

import newmodels.AbstractModel;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectClickProb extends AbstractSlotToPrClick {

	public PerfectClickProb(Query query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getPrediction(double bid) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}

}
