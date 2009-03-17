package agents.rules;

import agents.SSBBidStrategy;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

public class NoSlot extends StrategyTransformation{	
	protected double _increase;
	protected String _advertiser;
	protected QueryReport _queryReport; 
	
	public NoSlot(String advertiser, double increase){
		_advertiser = advertiser;
		_increase = increase;
	}
	
	public void updateReport(QueryReport queryReport){_queryReport = queryReport;}
	
	@Override
	protected void transform(Query q, SSBBidStrategy strategy) {
		if(_queryReport != null && strategy.getQuerySpendLimit(q) > 0){
			if(Double.isNaN(_queryReport.getPosition(q, _advertiser))){
				double current = strategy.getQueryReinvestFactor(q);
				strategy.setQueryReinvestFactor(q, current+_increase);
			}
		}
	}

}
