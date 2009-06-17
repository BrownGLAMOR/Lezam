package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * 
 * @author mbarrows
 *
 */

public class XFixedProfitAgent extends SimAbstractAgent {

	protected final double X = 0; //X is the amount of profit an agent will try to make on each query at minimum
	protected String manufacturer;
	protected String component;
	
	protected double getValue(Query query){
		return 0;
	}
	
	@Override
	public void initBidder() {
		manufacturer = _advertiserInfo.getManufacturerSpecialty();
		component = _advertiserInfo.getComponentSpecialty();
	}

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<AbstractModel> initModels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
		// TODO Auto-generated method stub
		
	}
}
