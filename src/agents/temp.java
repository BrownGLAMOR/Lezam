package agents;

import java.util.Set;

import newmodels.AbstractModel;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class temp extends SimAbstractAgent {

	protected int counter = 0;
	
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
			    if(_queryReport.getPosition(q)== Double.NaN) queryBid = queryBid*1.3;
			    else if(_queryReport.getPostion(q)== 1) queryBid = _queryReport.getCPC(q);
			    else if(_queryReport.getPosition(q)==2 && _numPromo ==2) queryBid = _queryReport.getCPC(q);
			    else if(counter == 1) queryBid = 1.2;
				//queryBid = 1.2;
				queryBudget = dailyCap;
			}
			else if (q.getType() == QueryType.FOCUS_LEVEL_ONE){
				 if(_queryReport.getPosition(q)== Double.NaN) queryBid = queryBid*1.3;
				  else if(_queryReport.getPostion(q)== 1) queryBid = _queryReport.getCPC(q);
				  else if(_queryReport.getPosition(q)==2 && _numPromo ==2) queryBid = _queryReport.getCPC(q);
				  else if(counter == 1) queryBid = 1.5;
				//queryBid = 1.5;
				if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
				}
				queryBudget = dailyCap;
			}
			else {
				    if(_queryReport.getPosition(q)== Double.NaN) queryBid = queryBid*1.3;
				    else if(_queryReport.getPostion(q)== 1) queryBid = _queryReport.getCPC(q);
				    else if(_queryReport.getPosition(q)==2 && _numPromo ==2) queryBid = _queryReport.getCPC(q);
				    else if(counter == 1) queryBid = 2.3;
				//queryBid = 2.3;
				if (q.getComponent() != null && q.getComponent().equals(_advertiserInfo.getComponentSpecialty())){
					if(counter == 1) queryBid = 3;
				}
				queryBudget = dailyCap;
			}
			bidBundle.addQuery(q, queryBid*.5, null);
			bidBundle.setDailyLimit(q, queryBudget);
			counter ++;
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
		for(Query query : _querySpace)  {
			System.out.println(query);
			System.out.println("\tNum Impressions: " + queryReport.getImpressions(query));
			System.out.println("\tNum Clicks: " + queryReport.getClicks(query));
			System.out.println("\tAverage Position: " + queryReport.getPosition(query));
		}
		//No models used
	}

}
