/**
 * 
 */
package agents;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import newmodels.AbstractModel;
import newmodels.bidtoslot.AbstractBidToSlotModel;
import newmodels.bidtoslot.BasicBidToSlot;
import newmodels.slottobid.AbstractSlotToBidModel;
import newmodels.slottobid.BasicSlotToBid;
import newmodels.slottonumclicks.AbstractSlotToNumClicks;
import newmodels.slottonumclicks.BasicSlotToNumClicks;
import newmodels.slottonumimp.AbstractSlotToNumImp;
import newmodels.slottonumimp.BasicSlotToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;
import newmodels.slottoprclick.BasicSlotToPrClick;
import newmodels.usermodel.AbstractUserModel;
import newmodels.usermodel.BasicUserModel;

import props.Misc;
import agents.mckp.IncItem;
import agents.mckp.Item;
import agents.mckp.ItemComparatorByWeight;

import modelers.bidtoposition.sam.PositionBidLinear;
import modelers.positiontoclick.PositionToClicksAverage;
import modelers.unitssold.UnitsSoldModelMeanWindow;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

/**
 * @author jberg
 *
 */
public class MCKPAgentMkII extends SimAbstractAgent {
	
	private int _numUsers = 90000;
	private int _overCap;
	private double _defaultBid;
	private HashMap<Query, Double> _recentBids;
	private HashMap<Query, Double> _previousBids;
	private HashMap<Query, Double> _salesPrices;
	private HashMap<Query, Double> _baseConvProbs;
	private AbstractUserModel _userModel;
	private HashMap<Query, AbstractBidToSlotModel> _bidToSlotModels;
	private HashMap<Query, AbstractSlotToBidModel> _slotToBidModels;
	private HashMap<Query, AbstractSlotToPrClick> _slotToPrClickModels;
	private HashMap<Query, AbstractSlotToNumImp> _slotToNumImptModels;
	private HashMap<Query, AbstractSlotToNumClicks> _slotToNumClicks;
	private Hashtable<Query, Integer> _queryId;



	@Override
	protected Set<AbstractModel> initModels() {
		/*
		 * Order is important because some of our models use other models
		 * so we use a LinkedHashSet
		 */
		Set<AbstractModel> models = new LinkedHashSet<AbstractModel>();
		_bidToSlotModels = new HashMap<Query, AbstractBidToSlotModel>();
		_slotToBidModels = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToPrClickModels = new HashMap<Query, AbstractSlotToPrClick>();
		_slotToNumImptModels = new HashMap<Query, AbstractSlotToNumImp>();
		_slotToNumClicks = new HashMap<Query, AbstractSlotToNumClicks>();
		AbstractUserModel userModel = new BasicUserModel();
		models.add(userModel);
		_userModel = userModel;
		for(Query query: _querySpace) {
			AbstractBidToSlotModel bidToSlot = new BasicBidToSlot(query,false);
			AbstractSlotToBidModel slotToBid = new BasicSlotToBid(query,false);
			AbstractSlotToPrClick slotToPrClick = new BasicSlotToPrClick(query);
			AbstractSlotToNumImp slotToNumImp = new BasicSlotToNumImp(query,userModel);
			AbstractSlotToNumClicks slotToNumClicks = new BasicSlotToNumClicks(query, slotToPrClick, slotToNumImp);
			models.add(bidToSlot);
			models.add(slotToBid);
			models.add(slotToPrClick);
			models.add(slotToNumImp);
			models.add(slotToNumClicks);
			_bidToSlotModels.put(query,bidToSlot);
			_slotToBidModels.put(query,slotToBid);
			_slotToPrClickModels.put(query,slotToPrClick);
			_slotToNumImptModels.put(query,slotToNumImp);
			_slotToNumClicks.put(query,slotToNumClicks);
		}
		return models;
	}

	@Override
	protected void initBidder() {
		
		
		_numUsers = 4000; //I'm not sure how to get this value actually

		_recentBids = new HashMap<Query, Double>();
		_previousBids = new HashMap<Query, Double>();
		_baseConvProbs = new HashMap<Query, Double>();
		_defaultBid = .4;

		_overCap = 18; //This sets how much we attempt to go over budget

		// set revenue prices
		_salesPrices = new HashMap<Query,Double>();
		for(Query q : _querySpace) {
			_recentBids.put(q, _defaultBid);
			_previousBids.put(q, _defaultBid);
			
			String manufacturer = q.getManufacturer();
			if(manufacturer == _manSpecialty) {
				_salesPrices.put(q, 15.0);
			}
			else {
				_salesPrices.put(q, 10.0);
			}
			
			if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				_baseConvProbs.put(q, _piF0);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
				_baseConvProbs.put(q, _piF1);
			}
			else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
				_baseConvProbs.put(q, _piF2);
			}
			else {
				throw new RuntimeException("Malformed query");
			}
			
			String component = q.getComponent();
			if(component == _compSpecialty) {
				_baseConvProbs.put(q,eta(_baseConvProbs.get(q),1+_CSB));
			}
		}
		
		
		/*
		 * Not really sure what these are used for, but I will leave them
		 * for now -jberg
		 */
		_queryId = new Hashtable<Query,Integer>();
		int i = 0;
		for(Query q : _querySpace){
			i++;
			_queryId.put(q, i);
		}
	}
	
	
	@Override
	protected void updateModels(SalesReport salesReport,
			QueryReport queryReport,
			Set<AbstractModel> models) {

		for(AbstractModel model:models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				userModel.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractBidToSlotModel) {
				AbstractBidToSlotModel bidToSlot = (AbstractBidToSlotModel) model;
				bidToSlot.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToBidModel) {
				AbstractSlotToBidModel slotToBid = (AbstractSlotToBidModel) model;
				slotToBid.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				slotToPrClick.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToNumImp) {
				AbstractSlotToNumImp slotToNumImp = (AbstractSlotToNumImp) model;
				slotToNumImp.updateModel(queryReport, salesReport);
			}
			else if(model instanceof AbstractSlotToNumClicks) {
				AbstractSlotToNumClicks slotToNumClicks = (AbstractSlotToNumClicks) model;
				slotToNumClicks.updateModel(queryReport, salesReport);
			}
		}
	}
	

	@Override
	protected BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();
		if(_day > 2){

			//generate undominated items and convert them to inc items
			//LinkedList<IncItem> iis = new LinkedList<IncItem>();
			LinkedList<IncItem> allIncItems = new LinkedList<IncItem>();
			//want the queries to be in a guaranteed order - put them in an array
			//index will be used as the id of the query
			for(Query q : _querySpace) {
				double salesPrice = _salesPrices.get(q);
				double convProb = _baseConvProbs.get(q);

				/**
				 * Uses a position click average model and a position bid linear model to 
				 * generate the item set for each query
				 */
				Item[] items = new Item[_numSlots];
				for(int s=1; s<=_numSlots; s++) {//slot
					int numClicks = _slotToNumClicks.get(q).getPrediction(s);
					double bid = _slotToBidModels.get(q).getPrediction(s);
					double CPC = _slotToBidModels.get(q).getPrediction(s+1);

					if (bid == 0) numClicks = 0;
					double w = numClicks*convProb; 				//weight = numClicks * convProb
					double v = numClicks*(salesPrice*convProb - CPC);	//value = numClicks(USP * convProb - CPC)

					int isID = _queryId.get(q);
					items[s-1] = new Item(q,w,v,bid,isID);	
					System.out.println(q + ", slot " + s + ", numCl " + numClicks + ", bid " + bid +
							", weight " + w + ", value " + v);
				}

				IncItem[] iItems = getIncremental(items);
				allIncItems.addAll(Arrays.asList(iItems));
			}

			//pick items greedily is order of decreasing efficiency
			Collections.sort(allIncItems);
			Misc.printList(allIncItems,"\n", true);

			double budget = (_capacity+_overCap)/_capWindow;
			// unitsSold.getWindowSold returns the items sold over the course of the last window, so this is :
			// budget = capacity(1+1/windowLength) - soldInWindow
//			System.out.println("\nCap: " + _capacity + ", Window: " + 
//					_unitsSold.getWindowSold() + ", Budget: " + budget + "\n");


			HashMap<Integer,Item> solution = fillKnapsack(allIncItems, budget);

			//set bids
			for(Query q : _querySpace){

				Integer isID = _queryId.get(q);
				double bid;

				if(solution.containsKey(isID)) {
					bid = solution.get(isID).b();
					_recentBids.put(q, bid);
				}
				else bid = _defaultBid; // TODO this is a hack that was the result of the fact that the item sets were empty

				bidBundle.addQuery(q, bid, new Ad(), bid*_capacity);
			}
		}
		//bid bundle for first two days
		//TODO The values .7, 1.3, and 1.7 are game specific, and then agent would be more 
		//     robust if it had a better estimate for starting defaults
		else {
			for(Query q : _querySpace){
				double bid;
				if (q.getType().equals(QueryType.FOCUS_LEVEL_ZERO))
					bid = .7;
				else if (q.getType().equals(QueryType.FOCUS_LEVEL_ONE))
					bid = 1.3;
				else 
					bid = 1.7;
				bidBundle.addQuery(q, bid, new Ad(), bid*_capacity);
				_recentBids.put(q, bid);
			}
		}

		return bidBundle;
	}


	/**
	 * Greedily fill the knapsack by selecting incremental items
	 * @param incItems
	 * @param budget
	 * @return
	 */
	private HashMap<Integer,Item> fillKnapsack(LinkedList<IncItem> incItems, double budget){
		HashMap<Integer,Item> solution = new HashMap<Integer, Item>();
		LinkedList<IncItem> temp = new LinkedList<IncItem>();

		boolean incremented = false;
		double valueLost = 0;
		double valueGained = 0;
		int knapSackIter = 0;

		for(IncItem ii: incItems) {
			//lower efficiencies correspond to heavier items, i.e. heavier items from the same item
			//set replace lighter items as we want

			if(budget >= ii.w()) {
				if (incremented){
					temp.addLast(ii);
					budget -= ii.w();
					valueGained += ii.v(); //amount gained as a result of extending capacity
				}
				else {
					System.out.println("adding item over capacity" + ii);
					solution.put(ii.item().isID(), ii.item());
					budget -= ii.w();
				}
			}
			else{
				if (incremented){
					if (valueGained >= valueLost){ //checks to see if it was worth extending our capacity
						while (!temp.isEmpty()){
							IncItem inc = temp.removeFirst();
							System.out.println("adding item " + ii);
							solution.put(inc.item().isID(), inc.item());
						}
						valueLost = 0;
						valueGained = 0;
					}
					else break;
				}
				double avgConvProb = .253; //the average probability of conversion;
				/*
				double avgUSP = 0;
				for (Query q : _querySpace){
					avgUSP += _rpc.get(q);
				}
				avgUSP /= 16;
				 */// This can be used later if the values actually change for the sales bonus
				double avgUSP = 11.25;
				for (int i = _overCap*knapSackIter+1; i <= _overCap*(knapSackIter+1); i++){
					double iD = Math.pow(_lambda, i);
					double worseConvProb = avgConvProb*iD; //this is a gross average that lacks detail
					valueLost += (avgConvProb - worseConvProb)*avgUSP;
				}
				budget+=_overCap;
				incremented = true;
				knapSackIter++;
			}
		}
		return solution;
	}

	/**
	 * Get undominated items ...................................................... getUndominated
	 * @param items
	 * @return
	 */
	public static Item[] getUndominated(Item[] items) {
		Arrays.sort(items,new ItemComparatorByWeight());

		//remove dominated items (higher weight, lower value)		
		LinkedList<Item> temp = new LinkedList<Item>();
		temp.add(items[0]);
		for(int i=1; i<items.length; i++) {
			Item lastUndominated = temp.get(temp.size()-1); 
			//Misc.println("prev " + lastUndominated.value() + " current " +  itemSet.get(j).value());
			if(lastUndominated.v() < items[i].v()) {
				//Misc.println("adding undominated item " + itemSet.get(j));
				temp.add(items[i]);
			}
		}

		//items now contain only undominated items
		items = temp.toArray(new Item[0]);
		Misc.printArray("undominated 1", items);

		//remove lp-dominated items
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(new Query(),0,0,-1,-1));//add item with zero weight and value

		for(int i=0; i<items.length; i++) {
			q.add(items[i]);//has at least 2 items now
			int l = q.size()-1;
			//Misc.println("l=" + l);
			Item li = q.get(l);//last item
			Item nli = q.get(l-1);//next to last
			if(li.w() == nli.w()) {
				if(li.v() > nli.v()) {
					q.remove(l-1);
				}else{
					q.remove(l);
				}
			}
			l = q.size()-1; //reset in case an item was removed
			//while there are at least three elements and ...
			while(l > 1 && (q.get(l-1).v() - q.get(l-2).v())/(q.get(l-1).w() - q.get(l-2).w()) 
					<= (q.get(l).v() - q.get(l-1).v())/(q.get(l).w() - q.get(l-1).w())) {
				q.remove(l-1);
				l--;
			}
		}

		//remove the (0,0) item
		if(q.get(0).w() == 0 && q.get(0).v() == 0) {
			q.remove(0);
		}

		Item[] uItems = (Item[]) q.toArray(new Item[0]);
		Misc.printArray("undominated items", uItems);
		return uItems;
	}


	/**
	 * Get incremental items ...................................................... getIncremental
	 * @param items
	 * @return
	 */
	public static IncItem[] getIncremental(Item[] items) {
		Misc.printArray("getIncremental", items);
		//items are still sorted by weight - create incremental items

		Item[] uItems = getUndominated(items);
		IncItem[] ii = new IncItem[uItems.length];

		if (uItems.length != 0){ //getUndominated can return an empty array
			ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0]);
			for(int item=1; item<uItems.length; item++) {
				Item prev = uItems[item-1];
				Item cur = uItems[item];
				ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur);
			}
			Misc.printArray("inc items", ii);
		}

		return ii;
	}


}
