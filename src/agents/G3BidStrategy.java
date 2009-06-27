package agents;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class G3BidStrategy extends SSBBidStrategy{
	
	protected int _capacityPerDay;
	protected double _pp; //Product Price
	protected double _revenue;
	protected int _quantity;
	protected double _totalCost;
	public boolean _day3;

//	protected Hashtable<Query, Double> _CPS; //Cost Per Sale
	protected double _CPS; //Cost Per Sale
	protected Hashtable<Query, Double> _CTR;
	protected Hashtable<Query, Double> _convProb;
	protected Hashtable<Query, Double> _bid;

	public G3BidStrategy(Set<Query> querySpace){
		super(querySpace);
		BidBundle bidBundle = new BidBundle();
		Random r = new Random();
//		_CPS = new Hashtable<Query, Double>();
		_day3 = false;
		_CPS = 5.0+r.nextDouble()*1.0;
		_CTR = new Hashtable<Query, Double>();
		_convProb = new Hashtable<Query, Double>();
		_bid = new Hashtable<Query, Double>();
		for (Query q : _querySpace) {
//			_CPS.put(q, 1.0+r.nextDouble()*0.5);
			_convProb.put(q, 0.15+r.nextDouble()*0.1);
			_bid.put(q, _CPS * _convProb.get(q));
			System.out.println ("LoopBID = " + _bid.get(q));
			bidBundle.addQuery(q, _bid.get(q), bidBundle.getAd(q));
		}
	}

	public boolean setData(Query q, double CTR, double convProb) {
		/*if (!(Double.isInfinite(CPS) || Double.isNaN(CPS))) {
			_CPS.put(q, 5.0);
			System.out.println ("CPS 5");
		} else {
			_CPS.put(q, CPS);
			System.out.println ("CPS real");
		}*/
		if (!(Double.isInfinite(CTR) || Double.isNaN(CTR))) _CTR.put(q, CTR);
		if (convProb > 0) _convProb.put(q, convProb);
		return true;
	}

	@Override
	public double getQueryBid(Query q) {
		return _bid.get(q);
	}
	
	public void setPP(double pp) {
		_pp = pp;
		System.out.println("PP = " + _pp);
	}
	
	public void setDistributionCapacity (int capacity, int window) {
		_capacityPerDay = capacity/window;
	}

	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		double distanceFrom = 1.0 - (double)_quantity/(double)_capacityPerDay;
		double increment = 1.0;
		HashMap<String, Double> p = new HashMap<String, Double>();
		//System.out.print("capacity:" + _capacity);
		//System.out.print("quantity:" + _quantity);
		//System.out.print("distance:" + distanceFrom);
		//System.out.println("increment:" + increment);
		p.put("capacity", (double)_capacityPerDay);
		p.put("quantity", (double)_quantity);
		p.put("distance", distanceFrom);
		p.put("increment", increment);
		p.put("dist", (distanceFrom*increment));
		printit(p);
		Random r = new Random();
		if (_totalCost > 0 && _quantity > 0) _CPS = Math.min(9, Math.max((_totalCost/_quantity) + distanceFrom*increment , 1));
		else _CPS = 5.0+r.nextDouble()*1.0;
		if (_day3) for(Query q : _querySpace) {
//			_CPS.put (q, Math.max(0, _CPS.get(q) *  (1 + distanceFrom) + distanceFrom));
			double queryBid;
			if (_convProb.get(q)>0) {
				queryBid = _CPS * _convProb.get(q);
//				System.out.println("yes1=" + queryBid);
			} else {
				queryBid = _CPS * 0.1;
//				System.out.println("yes2=" + queryBid);
			}
			//System.out.println("queryBid:" + queryBid);
			//System.out.println("CPS:" + _CPS.get(q));
			//System.out.println("newCPS:" + newCPS);
			//p.clear();
			//p.put("queryBid", queryBid);
			//p.put("CPS", _CPS.get(q));
			//printit(p);

			if (queryBid > 0.9*_pp) {
				queryBid = 0.9*_pp; //??
//				System.out.println("yes3=" + queryBid);
			}
			_bid.put (q, queryBid);
//			System.out.println ("##(" + q.getComponent() + "," + q.getManufacturer() + ")## Bid on " + queryBid);
			bidBundle.addQuery(q, queryBid, bidBundle.getAd(q));
//			bidBundle.setDailyLimit(q, bidBundle.getQuerySpendLimit(q));
		}
		else for(Query q : _querySpace)
			bidBundle.addQuery(q, _bid.get(q), bidBundle.getAd(q));

		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;	
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
//		buff.append("CampaignSpendLimit: ").append(_campaignSpendLimit).append("\n");
		buff.append("\t").append("CPS: ").append(_CPS).append("\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(_bid.get(q)).append("\n");
//			buff.append("\t").append("CTR: ").append(_CTR.get(q)).append("\n");
			buff.append("\t").append("CP: ").append(_convProb.get(q)).append("\n");
//			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
//			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
//			buff.append("\t").append("Conversion: ").append(getQueryConversion(q)).append("\n");
//			buff.append("\t").append("ReinvestFactor: ").append(getQueryReinvestFactor(q)).append("\n");
//			buff.append("\t").append("ConversionRevenue: ").append(getQueryConversionRevenue(q)).append("\n");
		}
		return buff.toString();
	}

	public void setConvertions(int quantity) {
		_quantity = quantity;
		System.out.println("q:"+quantity);
	}	
	
	public void setCost(double cost) {
		_totalCost = cost;
		System.out.println("TC:"+cost);
	}	

	private void printit(HashMap<String, Double> p) {
		for (Map.Entry<String, Double> s :  p.entrySet()) {
			System.out.print(s.getKey() + ": " + s.getValue() + " || ");
		}
		System.out.println();
	}

}
