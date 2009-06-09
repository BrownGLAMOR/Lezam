package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.AdvertiserInfo;
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
				queryBid = .7;
				queryBudget = dailyCap;
			}
			else if (q.getType() == QueryType.FOCUS_LEVEL_ONE){
				queryBid = 1.0;
				if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
					queryBid = 1.2;
				}
				queryBudget = dailyCap;
			}
			else {
				queryBid = 1.5;
				if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
					queryBid = 2;
				}
				queryBudget = dailyCap;
			}
			bidBundle.addQuery(q, queryBid, null);
			bidBundle.setDailyLimit(q, Double.NaN);
		}
		
		return bidBundle;
	}

	@Override
	public void initBidder() {
			//No initialization necessary
		
	}

	@Override
	protected Set<AbstractModel> initModels() {
		//No initialization necessary
		return null;
	}

	@Override
	protected void updateModels(SalesReport salesReport, QueryReport queryReport) {
		for(Query query : _querySpace)  {
			System.out.println(query);
			System.out.println("\tNum Impressions: " + queryReport.getImpressions(query));
			System.out.println("\tNum Clicks: " + queryReport.getClicks(query));
			System.out.println("\tAverage Position: " + queryReport.getPosition(query));
		}
		//No models used
	}

}
