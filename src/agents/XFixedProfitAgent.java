package agents;

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
		
	}
	
	@Override
	public void initBidder() {
		manufacturer = _advertiserInfo.getManufacturer();
	}
}
