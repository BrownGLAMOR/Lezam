package carleton.strategies;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;

public class PosXBidStrategy extends SSBBidStrategy{
	
	protected int _capacityPerDay;
	protected double _pp; //Product Price
	protected double _revenue;
	protected int _quantity;
	protected int _posX;
	protected int _day;

	protected Hashtable<Query, Double> _CPS; //Cost Per Sale
	protected Hashtable<Query, Double> _CTR;
	protected Hashtable<Query, Double> _convProb;
	protected Hashtable<Query, Double> _bid;
	protected Hashtable<Query, Integer> _position;
	protected Hashtable<Query, Double> _CPC;

	public PosXBidStrategy(Set<Query> querySpace, int pos){
		super(querySpace);
		_CPS = new Hashtable<Query, Double>();
		_CTR = new Hashtable<Query, Double>();
		_convProb = new Hashtable<Query, Double>();
		_CPC = new Hashtable<Query, Double>();
		_bid = new Hashtable<Query, Double>();
		_position = new Hashtable<Query, Integer>();
		Random r = new Random();
		for (Query q : _querySpace) {
			_CPS.put(q, 2.0+r.nextDouble()*0.5);
			_convProb.put(q, 0.15+r.nextDouble()*0.1);
			_bid.put(q, _CPS.get(q) * _convProb.get(q));
			_CPC.put(q, Double.NaN);
			_position.put(q, 0);
		}
		_posX = pos;
	}

	public boolean setData(Query q, double CPC, int pos) {
		boolean flag = true;
		if (CPC>=0 && CPC<=10) {
			_CPC.put(q, CPC);
		}
		else flag = false;
		if (pos >= 0 && pos < 9)
			_position.put(q, pos);
		else flag = false;
		return flag;
	}
	

	@Override
	public double getQueryBid(Query q) {
		return _bid.get(q);
	}
	
	public void setPP(double pp) {
		_pp = pp;
	}
	
	public void setDistributionCapacity (int capacity, int window) {
		_capacityPerDay = capacity/window;
	}

	public BidBundle buildBidBundle(){
		BidBundle bidBundle = new BidBundle();
		HashMap<String, String> p = new HashMap<String, String>();
		//System.out.print("capacity:" + _capacity);
		//System.out.print("quantity:" + _quantity);
		//System.out.print("distance:" + distanceFrom);
		//System.out.println("increment:" + increment);
/*		p.put("capacity", (double)_capacityPerDay);
		p.put("quantity", (double)_quantity);
		p.put("distance", distanceFrom);
		p.put("increment", increment);
		p.put("dist", (distanceFrom*increment));
		printit(p);*/
		_day++;
		
		for(Query q : _querySpace) {
			p.put("Query", q.getComponent() + "-" + q.getManufacturer());
			p.put("OldBid", _bid.get(q).toString());
//			System.out.print("cpc="+_CPC.get(q));
//			if (_CPC.get(q).equals(Double.NaN)) System.out.println(" NNAANN");
//			else System.out.println(" unNNAANN");
			if ((!_CPC.get(q).equals(Double.NaN)) && (_position.get(q) > 0)) {
				p.put("Position", _position.get(q).toString());
				int delta = _position.get(q) - _posX;
				//p.put(q.toString(), Integer.toString(delta));
				if (delta > 0)
					_bid.put(q, _bid.get(q)+0.05);
				else if(delta < 0)
					_bid.put(q, _CPC.get(q)-0.05);
			//System.out.println("CPS:" + _CPS.get(q));
			//System.out.println("newCPS:" + newCPS);
//			p.clear();
			} else
				_bid.put(q, _bid.get(q)+0.05);				
			p.put("NewBid", _bid.get(q).toString());
			printit(p);
				
//			System.out.println ("##(" + q.getComponent() + "," + q.getManufacturer() + ")## Bid on " + queryBid);
			bidBundle.addQuery(q, _bid.get(q), bidBundle.getAd(q));
//			bidBundle.setDailyLimit(q, bidBundle.getQuerySpendLimit(q));
		}
		
		bidBundle.setCampaignDailySpendLimit(_campaignSpendLimit);
		
		return bidBundle;	
	}

	public String toString(){
		StringBuffer buff = new StringBuffer(255);
		buff.append("CampaignSpendLimit: ").append(_campaignSpendLimit).append("\n");
		for(Query q : _querySpace){
			buff.append(q).append("\n");
			buff.append("\t").append("Bid: ").append(_bid.get(q)).append("\n");
/*			buff.append("\t").append("CPS: ").append(_CPS.get(q)).append("\n");
			buff.append("\t").append("CTR: ").append(_CTR.get(q)).append("\n");
			buff.append("\t").append("CP: ").append(_convProb.get(q)).append("\n");
			buff.append("\t").append("SpendLimit: ").append(getQuerySpendLimit(q)).append("\n");
			buff.append("\t").append("Ad: ").append(getQueryAd(q)).append("\n");
			buff.append("\t").append("Conversion: ").append(getQueryConversion(q)).append("\n");
			buff.append("\t").append("ReinvestFactor: ").append(getQueryReinvestFactor(q)).append("\n");
			buff.append("\t").append("ConversionRevenue: ").append(getQueryConversionRevenue(q)).append("\n");*/
		}
		return buff.toString();
	}

	public void setConvertions(int quantity) {
		_quantity = quantity;
		System.out.println("q:"+quantity);
	}	
	
	private void printit(HashMap<String, String> p) {
		for (Map.Entry<String, String> s :  p.entrySet()) {
			System.out.print(s.getKey() + ": " + s.getValue() + " || ");
		}
		System.out.println();
	}

}
