package newmodels.bidtoslot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import modelers.bidtoposition.ilke.BucketBidToPositionModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;


public class WrapperIlkeBid2Slot extends AbstractBidToSlotModel{

	BucketBidToPositionModel ilke;
	
	public WrapperIlkeBid2Slot(Query query, int numOfSlots) {
		super(query);
		Set<Query> queries = new HashSet<Query>();
		queries.add(query);
		ilke = new BucketBidToPositionModel(queries, numOfSlots);
	}

	@Override
	public double getPrediction(double bid) {
		return ilke.getPosition(_query, bid);
	}

/*	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return false;
	}*/
	
	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, HashMap<Query,Double> lastBids) {
	
		double lastBid = lastBids.get(_query);
		if (Double.isInfinite(lastBid) || Double.isNaN(lastBid) || queryReport.equals(null)) return false;
		BidBundle bb = new BidBundle();
		bb.setBid(_query, lastBid);
		ilke.updateBidBundle(bb);
		
		QueryReport oneQueryReport = new QueryReport();
		oneQueryReport.addImpressions(_query, queryReport.getImpressions(_query), queryReport.getPromotedImpressions(_query), queryReport.getAd(_query), (queryReport.getImpressions(_query) + queryReport.getPromotedImpressions(_query))*queryReport.getPosition(_query));
		ilke.updateQueryReport(oneQueryReport);
		
		ilke.train();
		
		return true;
	}
	
	public static void main (String args[]) {
		Query query = new Query("pg","dvd");
		WrapperIlkeBid2Slot w = new WrapperIlkeBid2Slot(query, 5);
		QueryReport queryReport = new QueryReport();
		SalesReport salesReport = new SalesReport();
		HashMap<Query,Double> bids = new HashMap<Query, Double>();

		// day 1
		queryReport.addQuery(query, 1000, 0, 200, 300, 1600); //Average Slot = 1.6 ; CPC = 1.5
		salesReport.addConversions(query, 50);
		salesReport.addRevenue(query, 500);
		bids.put(query, 2.0);
		w.updateModel(queryReport, salesReport, bids); // bid = 2

		// day 2
		queryReport.setClicks(query, 150, 150); // CPC = 1.0
		queryReport.setImpressions(query, 1000, 0);
		queryReport.setPositionSum(query, 2200); // Average Slot = 2.2
		salesReport.setConversions(query, 50);
		salesReport.setRevenue(query, 500);
		bids.put(query, 1.4);
		w.updateModel(queryReport, salesReport, bids); // bid = 1.4

		System.out.println(w.getPrediction(2));
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}
}