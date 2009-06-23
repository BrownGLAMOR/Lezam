package newmodels.slottobid;

import java.util.HashMap;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class LinearSlotToBid extends AbstractSlotToBidModel {

	int _numOfSlots;
	double _cpcs[];
	double _delta;
	
	public LinearSlotToBid(Query query, int numOfSlots) {
		super(query);
		_cpcs = new double[10*numOfSlots];
		_numOfSlots = numOfSlots;		                  
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getPrediction(double slot) {
		if ((slot >= 0) && (slot <= 5)) return _cpcs[(int)((slot-1)*10)];
		else return 0;
	}

	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		if (queryReport.equals(null)) return false;
		if (Double.isNaN(queryReport.getCPC(_query))) return false;
		int lastSlot = (int)((queryReport.getPosition(_query)-1)*10.0);
		double CPC = queryReport.getCPC(_query);
		double lastBid = 1;
		if (!(bidBundle.getBid(_query) == Double.NaN) || bidBundle.getBid(_query) == 0.0 ) {
			lastBid = bidBundle.getBid(_query);
		} else {
			if (!Double.isNaN(_delta)) lastBid = CPC+_delta;
			else {
				for (int i=0 ; i<_cpcs.length ; i++) {
					lastBid = Math.max(lastBid,_cpcs[i]);
				}
			}
		}
		
		_delta = (lastBid - CPC)/10;
		for (int slot=0 ; slot<10*_numOfSlots ; slot++) {
			_cpcs[slot] = CPC + (lastSlot - slot)*_delta;
			if (Double.isInfinite(_cpcs[slot])) System.out.print("\n infinity problem because cpc=" + CPC + " or lastSlot=" + lastSlot + " or slot=" + slot + " or delta=" + _delta + " or bid=" + lastBid + " or we got the bid=" + bidBundle.getBid(_query) + "\n");
			
		}
		return true;
	}
	
	public static void main(String[] args){
		Query q = new Query("pg","dvd");
		QueryReport qr = new QueryReport();
		qr.addQuery(q, 1000, 0, 100, 30, 3000);
		System.out.println(qr.getCPC(q));
		SalesReport sr = new SalesReport();
		
		BidBundle bidBundle = new BidBundle();
		bidBundle.setBid(q,0.4);
		LinearSlotToBid lstc = new LinearSlotToBid(q,5);
		lstc.updateModel(qr, sr, bidBundle);
		System.out.println(lstc.getPrediction(2.9));
	}

}
