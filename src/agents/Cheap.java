package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class Cheap extends SimAbstractAgent {

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();
		double distCap = (double) _advertiserInfo.getDistributionCapacity();
		double distWind = (double) _advertiserInfo.getDistributionWindow();
		double dailyCap = distCap/distWind;

		for(Query q : _querySpace) {
			double queryBid;
			double queryBudget;
			if (q.getType() == QueryType.FOCUS_LEVEL_ZERO){
				queryBid = 1.2;
			}
			else if (q.getType() == QueryType.FOCUS_LEVEL_ONE){
				queryBid = 1.5;
				if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
				}
			}
			else {
				queryBid = 2.3;
				if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
					queryBid = 3;
				}
			}
			queryBudget = dailyCap * queryBid;
			bidBundle.addQuery(q, queryBid*.5, null);
			bidBundle.setDailyLimit(q, queryBudget);
		}

		return bidBundle;
	}

	@Override
	public void initBidder() {
			//No initialization necessary
		
	}

	@Override
	public Set<AbstractModel> initModels() {
		//No initialization necessary
		return null;
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport) {
//		for(Query query : _querySpace)  {
//			System.out.println(query);
//			System.out.println("\tNum Impressions: " + queryReport.getImpressions(query));
//			System.out.println("\tNum Clicks: " + queryReport.getClicks(query));
//			System.out.println("\tAverage Position: " + queryReport.getPosition(query));
//		}
		//No models used
	}

}
