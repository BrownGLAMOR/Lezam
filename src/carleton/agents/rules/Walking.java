package carleton.agents.rules;

import carleton.strategies.GenericBidStrategy;
import carleton.strategies.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class Walking extends StrategyTransformation{	
	protected String _advertiser;
	protected QueryReport _queryReport; 
	
	public Walking(String advertiser){
		_advertiser = advertiser;
	}
	
	public void updateReport(QueryReport queryReport){_queryReport = queryReport;}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		
		if(_queryReport != null && strategy.getQueryBid(q) > 0.25){//this may be a bad idea, need bidding from yesterday... (cjc)
			double position = _queryReport.getPosition(q, _advertiser);
			double cpc = _queryReport.getCPC(q);
			if(!(Double.isNaN(position) || Double.isNaN(cpc))){
				double current = strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR);
				double currentBid = strategy.getQueryBid(q);
				double y = currentBid/current;
				double distance = Math.abs(currentBid - cpc);
				double rfDistance = (current - (distance/y))/10;
				
				if(Math.random() > 0.5){
					if(position > 1.1){
						strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, current+rfDistance);
					}
				}
				else {
					strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, current-rfDistance);
				}
				
			}
		}
	}
}
