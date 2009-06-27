package modelers.capacity;

import java.util.HashMap;
import java.util.Set;

import modelers.bidtocpc.BidtoCPC;
import modelers.clickprob.ClickProbabilityModel;
import edu.umich.eecs.tac.props.Query;

/* 
 * other models used: BidtoCPC, ClickProfitability
 */



public class DailyCapacity {

	protected double _cpc;
	protected double _revPerClick; // conversion probability * manufacturing bonus, weighted across queries
	protected double _profPerClick;
	protected double _USB;
	protected final double LAMBDA = 0.995;
	protected final int CAP = 31; // most allowed to oversell in 1 normal day
	
	public double getOptimalDailyCapacity (HashMap<Query,Double> weights, HashMap<Query,Double> bids, 
			int maxCap, Set<Query> querySpace, int soldInLast4Days, String componentSpecialty, 
			String manufacturerSpecialty) {
		_cpc = 0;
		_revPerClick = 0;
		
		BidtoCPC CPCModel = new BidtoCPC(querySpace, bids);
		HashMap<Query,Double> CPCs = CPCModel.getCPCs();
		
		ClickProbabilityModel ClProbMod = new ClickProbabilityModel(querySpace, componentSpecialty, manufacturerSpecialty);
		HashMap<Query,Double> _clickRevs = ClProbMod.getClickRevenue();
		
		for (Query q: querySpace){
			double weight = weights.get(q);
			_cpc += weight * CPCs.get(q);
			_revPerClick += weight * _clickRevs.get(q);
		}
		_profPerClick = _revPerClick - _cpc;
		
		int oversoldInLast4Days = soldInLast4Days - maxCap*4/5; // might want to max this with 0
		
		int deltaOptimal = 0;
		double newProfit = 0;
		double recentNewProfit = 0;
		double oldLambda = 1;
		while (newProfit >= recentNewProfit && deltaOptimal < CAP){
			deltaOptimal++;
			oldLambda *= LAMBDA;
		    double newRevPerClick = _revPerClick * oldLambda;//Math.pow(LAMBDA, deltaOptimal);
		    double newProfitPerClick = newRevPerClick - _cpc;
		    recentNewProfit = newProfit;
		    newProfit = newProfitPerClick * 5 * deltaOptimal - _revPerClick * oversoldInLast4Days;
		}
		deltaOptimal = Math.max(deltaOptimal - 1, 0);

		
		return (.2 + (deltaOptimal/maxCap));
	}
}
