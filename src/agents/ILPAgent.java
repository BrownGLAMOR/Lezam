package agents;

import java.util.Hashtable;
import java.util.Vector;

import agents.rules.SetProperty;
import se.sics.tasim.props.SimulationStatus;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;

public class ILPAgent extends AbstractAgent{

	protected ILPBidStrategy _bidStrategy;
	protected double _day;
	Vector<Double> bids;
	Vector<Integer> quantities;
	
	public ILPAgent(){		System.out.println("AAAAAAA");
};
	protected void simulationSetup() {}

	@Override
	protected BidBundle buildBidBudle() {
//		System.out.println("**********");
//		System.out.println(_bidStrategy);
//		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

	@Override
	protected void initBidder() {
		// TODO Auto-generated method stub
		System.out.println("AAAAAAA");
		_bidStrategy = new ILPBidStrategy(_querySpace);
		
		_bidStrategy.setDefaultProperty(ILPBidStrategy.DAILYCAPACITY, (double)_advertiserInfo.getDistributionCapacity() / _advertiserInfo.getDistributionWindow());
		_bidStrategy.setDefaultProperty(ILPBidStrategy.NUMUSERS, 9000); // TODO CHANGE CONSTANT
		_bidStrategy.setDefaultProperty(ILPBidStrategy.DISCOUNTER, _advertiserInfo.getDistributionCapacityDiscounter());
		
		for (Product p : _retailCatalog) {
			int givesBonus=0;
			if (p.getManufacturer().equals(_advertiserInfo.getManufacturerSpecialty())) givesBonus = 1;
			double profit = _retailCatalog.getSalesProfit(p)*(1+_advertiserInfo.getComponentBonus()*givesBonus);
			System.out.println("product " + p.getComponent() + " " + p.getManufacturer() + " with profit " + profit);
			//_bidStrategy.setProductsRevenue(p, profit);
		}
		
		bids = new Vector<Double>(_bidStrategy.setPossibleBids(0, getAvaregeProductPrice(), 0.01)); //TODO replace getAverageProduct with a lower bid
		quantities = new Vector<Integer>(_bidStrategy.setPossibleQuantities(0, 30, 1)); //TODO replace maximum quantities with a max's offer
		System.out.println("b size = " + bids.size());
		System.out.println("q size = " + quantities.size());
	}

	@Override
	protected void updateBidStrategy() {
		int numOfSlots = _slotInfo.getRegularSlots()+_slotInfo.getPromotedSlots();
		ILPmodelsCalc ilp = new ILPmodelsCalc(bids , quantities , _retailCatalog , _queryReports , numOfSlots);
		_bidStrategy.refreshData(ilp);		
	}

	protected void handleSimulationStatus(SimulationStatus simulationStatus) {
		_day = simulationStatus.getCurrentDate();
		super.handleSimulationStatus(simulationStatus);
	}
}

/*
 * I think there is a need for a small correction in the ILP model, we are summing over all products and queries. The problem is,
 *  that a specific query can constrain the products that the user can look. For example, let's say that we are talking about the 
 *  query <tv, pg>. The users that ask for this query will only buy a tv, so we don't need to sum over all products. What do you think?
 *  Maybe a small indicator that will be equal to 1 only if the products can be purchased through this query?
*/