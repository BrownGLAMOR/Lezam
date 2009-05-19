package modelers;

import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.Query;

/*
 * initial model, assumes Bid = CPC
 */

public class BidtoCPC {

	
	
	protected HashMap<Query,Double> CPCs_;
	
	public BidtoCPC(Set<Query> querySpace, HashMap<Query,Double> bids){
		CPCs_ = new HashMap<Query,Double>();
		for (Query q: querySpace){
			CPCs_.put(q, bids.get(q));
		}
	}
	
	public HashMap<Query,Double> getCPCs(){
		return CPCs_;
	}
}
