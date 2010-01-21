package agents;

import java.util.Set;
import java.util.Random;

import models.AbstractModel;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;
// Slot information
import edu.umich.eecs.tac.props.SlotInfo;
// Ad information
import edu.umich.eecs.tac.props.Ad;
// Auction information
import edu.umich.eecs.tac.props.Auction;
// Bank Information
import edu.umich.eecs.tac.props.BankStatus;



public class VinitAgent extends AbstractAgent
{
	private Random _R = new Random();

	@Override
	public BidBundle getBidBundle(Set<AbstractModel> models)
	{
		BidBundle bidBundle = new BidBundle();
		
		SalesReport salesreport = new SalesReport();
		
		// Create a new ad
		Ad ad = new Ad();
		
		// Create a new auction
		Auction auction = new Auction();
		
		BankStatus bankStatus = new BankStatus();
		
		//SlotInfo slotInfo;
		
		double distCap = (double) _advertiserInfo.getDistributionCapacity();
		double distWind = (double) _advertiserInfo.getDistributionWindow();
		double dailyCap = distCap / distWind;
		double bid;
		int counter = 0;

		
		
		
		
		bidBundle.setCampaignDailySpendLimit(2000.0);		
		
		for (Query q : _querySpace)
		{
			SlotInfo slotInfo = new SlotInfo();
			/*double queryBid;
			double queryBudget;
			if (q.getType() == QueryType.FOCUS_LEVEL_ZERO)
			{
				queryBid = 1.2;
			} else if (q.getType() == QueryType.FOCUS_LEVEL_ONE)
			{
				queryBid = 1.5;
				if (q.getComponent() != null
						&& q.getComponent().equals(
								_advertiserInfo.getComponentSpecialty()))
				{
				}
			} else
			{
				queryBid = 2.3;
				if (q.getComponent() != null
						&& q.getComponent().equals(
								_advertiserInfo.getComponentSpecialty()))
				{
					queryBid = 3;
				}
			}*/
			//bid = randDouble(.25, 1.50);
			bid = 0.70;
			//queryBudget = dailyCap * queryBid;
			//bid = queryBid * .5 * randDouble(.25, 1.25);
			//bidBundle.addQuery(q, bid , ad.getProduct(), 2000);
			
			bidBundle.addQuery(q, bid, (bidBundle.getAd(q)));
			// bidBundle.setDailyLimit(q, queryBudget);
			
			
			//System.out.println("\n\n\n\n\n");
			
			//System.out.println("User" + counter +" is bidding " + bid);
			
			//System.out.println("\n\n\n\n\n");
			
			//counter++;
			
			//System.out.println("\n\n\n\n\n");
			
			//System.out.println("This query q"+q+"is of type:"+q.getType());
			
			//System.out.println("\n\n\n\n\n");
			
			System.out.println("\n\n\n\n\n");
			
			// Information from class Advertiser Info
			/*System.out.println("*** All Advertizer Information ***");
			
			System.out.println("Advertiser address:"+_advertiserInfo.getAdvertiserId());
			
			System.out.println("Advertiser component bonus is:"+_advertiserInfo.getComponentBonus());
			
			System.out.println("Advertiser component specialty is:"+_advertiserInfo.getComponentSpecialty());
			
			System.out.println("Advertiser distribution capacity is:"+_advertiserInfo.getDistributionCapacity());
			
			System.out.println("Advertiser distribution capacity decay rate is:"+_advertiserInfo.getDistributionCapacityDiscounter());
			
			System.out.println("Advertiser distribution window is:"+_advertiserInfo.getDistributionWindow());
			
			System.out.println("Advertiser focus effect for focus level 0 is:"+_advertiserInfo.getFocusEffects(QueryType.FOCUS_LEVEL_ZERO));
			
			System.out.println("Advertiser focus effect for focus level 1 is:"+_advertiserInfo.getFocusEffects(QueryType.FOCUS_LEVEL_ONE));
			
			System.out.println("Advertiser focus effect for focus level 2 is:"+_advertiserInfo.getFocusEffects(QueryType.FOCUS_LEVEL_TWO));
			
			System.out.println("Advertiser manufacturer bonus is:"+_advertiserInfo.getManufacturerBonus());
			
			System.out.println("Advertiser manufacturer specialty is:"+_advertiserInfo.getManufacturerSpecialty());
			
			// Print a blank line for readability
			System.out.println();
						
			// Information from class Advertiser Info
			System.out.println("Auction pricing is:"+auction.getPricing());
			
			System.out.println("Auction query is:"+auction.getQuery());
			
			System.out.println("Auction ranking is:"+auction.getRanking());*/
			
			System.out.println("Counter is:"+counter);
			
			
			
			/*System.out.println("Publisher ID is:"+_advertiserInfo.getManufacturerSpecialty());
			
			System.out.println("Advertiser component bonus is:"+_advertiserInfo.getPublisherId());
			
			System.out.println("Advertiser target effect is:"+_advertiserInfo.getTargetEffect());
			
			// Bank Information
			System.out.println("Bank information is:"+bankStatus.getAccountBalance());
			
			System.out.println("Promoted slot bonus is:"+slotInfo.getPromotedSlotBonus());
			
			System.out.println("The sales report transport name is:"+salesreport.getTransportName());
			
			System.out.println("The conversion for the query is:"+salesreport.getConversions(q));
			
			System.out.println("The revenue for the query is:"+salesreport.getRevenue(q)); 
			
			System.out.println("The ad is targeting: "+ad.getProduct());*/
			
			System.out.println("The current query is:"+q);			
			
			System.out.println("\n\n\n\n\n");
			
			counter++;
			
				
		}

		return bidBundle;
	}

	@Override
	public void initBidder()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Set<AbstractModel> initModels()
	{
		return null;
	}

	@Override
	public AbstractAgent getCopy()
	{
		return new VinitAgent();
	}

	@Override
	public String toString()
	{
		return "VinitAgent";
	}

	@Override
	public void updateModels(SalesReport salesReport, QueryReport queryReport)
	{
	}
	
	private double randDouble(double a, double b)
	{
		double rand = _R.nextDouble();
		return rand * (b - a) + a;
	}

}
