package agents.rules;

import agents.GenericBidStrategy;
import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class NoImpressions extends StrategyTransformation{	
	protected double _increase;
	protected String _advertiser;
	protected QueryReport _queryReport; 
	
	public NoImpressions(String advertiser, double increase){
		_advertiser = advertiser;
		_increase = increase;
	}
	
	public void updateReport(QueryReport queryReport){_queryReport = queryReport;}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		
		if(_queryReport != null && strategy.getQueryBid(q) > 0.25){//this may be a bad idea, need bidding from yesterday...
			if(Double.isNaN(_queryReport.getPosition(q, _advertiser))){
				double current = strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR);
				strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, current+_increase);
			} else if(_queryReport.getPosition(q, _advertiser) > 2) {
				double current = strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR);
				strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, current+_increase);
			}
		}
	}

}
