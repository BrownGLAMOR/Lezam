package agents.rules;

import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public abstract class AbstractRule {
	
	protected Set<Query> _querySpace;
	
	public AbstractRule(Set<Query> _querySpace) {
		this._querySpace = _querySpace;
	}
	
	public void applyToAll(BidBundle bidBundle, QueryReport queryReport, SalesReport salesReport) {
		applyToSet(_querySpace, bidBundle, queryReport, salesReport);
	}
	
	public void applyToSet(Set<Query> queries, BidBundle bidBundle, QueryReport queryReport, SalesReport salesReport){
		for(Query q : queries){
			apply(q,bidBundle, queryReport, salesReport);
		}
	}
	
	public abstract void apply(Query q, BidBundle bidBundle, QueryReport queryReport, SalesReport salesReport);
	
}
