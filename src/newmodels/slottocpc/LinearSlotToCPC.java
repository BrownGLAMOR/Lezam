package newmodels.slottocpc;

import java.util.HashMap;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class LinearSlotToCPC extends AbstractSlotToCPCModel {

	int _numOfSlots;
	double _cpcs[];
	
	public LinearSlotToCPC(Query query, int numOfSlots) {
		super(query);
		_cpcs = new double[10*numOfSlots];
		_numOfSlots = numOfSlots;		                  
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getPrediction(double slot) {
		return _cpcs[(int)((slot-1)*10)];
	}

	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, HashMap<Query,Double> lastBid) {
		if (queryReport.equals(null) || lastBid.get(_query) <= 0) return false; 
		int lastSlot = (int)((queryReport.getPosition(_query)-1)*10.0);
		double CPC = queryReport.getCPC(_query);
		double delta = (lastBid.get(_query) - CPC)/10;
		for (int slot=0 ; slot<10*_numOfSlots ; slot++) {
			_cpcs[slot] = CPC + (lastSlot - slot)*delta;
			
		}
		return true;
	}
	
	public static void main(String[] args){
		Query q = new Query("pg","dvd");
		QueryReport qr = new QueryReport();
		qr.addQuery(q, 1000, 0, 100, 30, 3000);
		System.out.println(qr.getCPC(q));
		SalesReport sr = new SalesReport();
		
		HashMap<Query,Double> bids = new HashMap<Query, Double>();
		bids.put(q,0.4);
		LinearSlotToCPC lstc = new LinearSlotToCPC(q,5);
		lstc.updateModel(qr, sr, bids);
		System.out.println(lstc.getPrediction(2.9));
	}

}
