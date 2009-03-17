package agents.rules;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class TopSlot extends StrategyTransformation{	
	protected double _decrease;
	protected String _advertiser;
	protected QueryReport _queryReport; 
	
	public TopSlot(String advertiser, double decrease){
		_advertiser = advertiser;
		_decrease = decrease;
	}
	
	public void updateReport(QueryReport queryReport){_queryReport = queryReport;}
	
	@Override
	protected void transform(Query q, SSBBidStrategy strategy) {
		if(_queryReport != null){
			if(_queryReport.getPosition(q, _advertiser) < 1.1){
				double current = strategy.getQueryReinvestFactor(q);
				strategy.setQueryReinvestFactor(q, current-_decrease);
			}
		}
	}

}
