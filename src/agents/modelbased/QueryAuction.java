package agents.modelbased;

import edu.umich.eecs.tac.props.Query;

public class QueryAuction {
	
	private Query _query;
	private int _day;
	
	public QueryAuction(Query q, int d){
		_query = q;
		_day = d;
	}
	
	public int getDay(){
		return _day;
	}
	
	public Query getQuery(){
		return _query;
	}
	
	@Override
	public String toString(){
		return "Query: "+_query+" Day: "+_day+"";
	}

}
