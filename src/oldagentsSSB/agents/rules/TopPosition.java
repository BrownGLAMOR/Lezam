package oldagentsSSB.agents.rules;

import oldagentsSSB.strategies.GenericBidStrategy;
import oldagentsSSB.strategies.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class TopPosition extends StrategyTransformation{	
	protected double _decrease;
	protected String _advertiser;
	protected QueryReport _queryReport; 
	
	public TopPosition(String advertiser, double decrease){
		_advertiser = advertiser;
		_decrease = decrease;
	}
	
	public void updateReport(QueryReport queryReport){_queryReport = queryReport;}
	
	@Override
	protected void transform(Query q, GenericBidStrategy strategy) {
		if(_queryReport != null){
			if(_queryReport.getPosition(q, _advertiser) < 1.1){
				double current = strategy.getProperty(q, SSBBidStrategy.REINVEST_FACTOR);
				if(current > _decrease){
					strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, current-_decrease);
				}
				else {
					//strategy.setProperty(q, SSBBidStrategy.REINVEST_FACTOR, 0);
				}
			}
		}
	}

}
