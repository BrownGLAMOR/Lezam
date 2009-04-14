package agents;

import java.util.Hashtable;
import java.util.*;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class MCKPBidStrategy {
	protected Set<Query> _querySpace;
	
	
	public MCKPBidStrategy(Set<Query> querySpace){
		_querySpace = querySpace;
	}

	public Set<Query> getQuerySpace(){return _querySpace;}
	
	public BidBundle buildBidBundle(){
				
		for(Query q : _querySpace) {
			double queryBid = getQueryConversionRevenue(q)*getQueryConversion(q)*getQueryReinvestFactor(q);

			bidBundle.addQuery(q, queryBid, getQueryAd(q));
			bidBundle.setDailyLimit(q, getQuerySpendLimit(q));
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		for(Query q : _querySpace){
			buff.append(q).append("\n");
		}
		return buff.toString();
	}
	
	}

	
	
	
	
}
