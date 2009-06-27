package agents;

import java.util.HashMap;
import java.util.Map;

import se.sics.tasim.props.SimulationStatus;
import agents.rules.DistributionCap;
import agents.rules.NoImpressions;
import agents.rules.ReinvestmentCap;
import agents.rules.TopPosition;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class G3Agent extends AbstractAgent {
	protected G3BidStrategy _bidStrategy;
	protected DistributionCap _distributionCap;
	protected ReinvestmentCap _reinvestmentCap;
	protected TopPosition _topPosition;
	protected NoImpressions _noImpressions;

	protected double _campaignSpendLimit;
	
	protected int _day;
	
	public G3Agent(){}
	
	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {
		printAdvertiserInfo();

		_bidStrategy = new G3BidStrategy(_querySpace);
		/*for(Query q : _querySpace) {
			_bidStrategy.setData(q, -1.0, 0.2);
		}*/
		
		_bidStrategy.setDistributionCapacity(_advertiserInfo.getDistributionCapacity(),_advertiserInfo.getDistributionWindow());
		_bidStrategy.setPP(getAvaregeProductPrice());
		
	}
	
	
	@Override
	protected void updateBidStrategy() {
		QueryReport qr = _queryReports.remove();
		SalesReport sr = _salesReports.remove();
		int quantity = 0;
		double cost = 0;
		if (_day < 3) return;
		else for(Query q : _querySpace) {
			//else System.out.println("i'm in!");
			_bidStrategy._day3 = true;
			double CTR = qr.getClicks(q) / (double)qr.getImpressions(q);
			double convProb = (double)sr.getConversions(q) / qr.getClicks(q);
			HashMap<String,Double> p = new HashMap<String,Double>();
			//double CPS = _bidStrategy.getQueryBid(q) / convProb;
			//System.out.println ("ubs_CPS: " + CPS);
			//System.out.println ("ubs_CTR: " + CTR);
			//System.out.println ("ubs_CcP: " + convProb);
			//System.out.print ("ubs_getClicks: " + qr.getClicks(q) + " || ");
			//System.out.print ("ubs_getImpressions: " + qr.getImpressions(q) + " || ");
			//System.out.println ("ubs_getConversions(q): " + sr.getConversions(q));
			p.put("getClicks",(double)qr.getClicks(q));
			p.put("ubs_getImpressions",(double)qr.getImpressions(q));
			p.put("ubs_getConversions(q)", (double)sr.getConversions(q));
//			printit(p);
						
			_bidStrategy.setData(q, CTR, convProb);
			quantity = quantity + sr.getConversions(q);
			cost = cost + qr.getCost(q);
		}
		_bidStrategy.setConvertions (quantity);
		_bidStrategy.setCost(cost);
		
	}
	

	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}
	
	private void printit(HashMap<String, Double> p) {
		for (Map.Entry<String, Double> s :  p.entrySet()) {
			System.out.print(s.getKey() + ": " + s.getValue() + " || ");
		}
		System.out.println();
	}

	protected void handleSimulationStatus(SimulationStatus simulationStatus) {
		//System.out.println ("FOUND IT!!!!! = " + simulationStatus.getCurrentDate() + " ###################################################################");
		_day = simulationStatus.getCurrentDate();
		super.handleSimulationStatus(simulationStatus);
	}


}
