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
import agents.mckpmkii.IncItem;
import agents.mckpmkii.Item;
import agents.mckpmkii.ItemComparatorByWeight;

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
		AbstractUserModel userModel = new BasicUserModel();
		models.add(userModel);
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
		}
		buildMaps(models);
		return models;
	}
	
	protected void buildMaps(Set<AbstractModel> models) {
		_bidToSlotModels = new HashMap<Query, AbstractBidToSlotModel>();
		_slotToBidModels = new HashMap<Query, AbstractSlotToBidModel>();
		_slotToPrClickModels = new HashMap<Query, AbstractSlotToPrClick>();
		_slotToNumImptModels = new HashMap<Query, AbstractSlotToNumImp>();
		_slotToNumClicks = new HashMap<Query, AbstractSlotToNumClicks>();
		for(AbstractModel model : models) {
			if(model instanceof AbstractUserModel) {
				AbstractUserModel userModel = (AbstractUserModel) model;
				_userModel = userModel;
			}
			else if(model instanceof AbstractBidToSlotModel) {
				AbstractBidToSlotModel bidToSlot = (AbstractBidToSlotModel) model;
				_bidToSlotModels.put(bidToSlot.getQuery(), bidToSlot);
			}
			else if(model instanceof AbstractSlotToBidModel) {
				AbstractSlotToBidModel slotToBid = (AbstractSlotToBidModel) model;
				_slotToBidModels.put(slotToBid.getQuery(), slotToBid);
			}
			else if(model instanceof AbstractSlotToPrClick) {
				AbstractSlotToPrClick slotToPrClick = (AbstractSlotToPrClick) model;
				_slotToPrClickModels.put(slotToPrClick.getQuery(), slotToPrClick);
			}
			else if(model instanceof AbstractSlotToNumImp) {
				AbstractSlotToNumImp slotToNumImp = (AbstractSlotToNumImp) model;
				_slotToNumImptModels.put(slotToNumImp.getQuery(), slotToNumImp);
			}
			else if(model instanceof AbstractSlotToNumClicks) {
				AbstractSlotToNumClicks slotToNumClicks = (AbstractSlotToNumClicks) model;
				_slotToNumClicks.put(slotToNumClicks.getQuery(), slotToNumClicks);
			}
		}
	}

	@Override
	protected void initBidder() {
		
		_recentBids = new HashMap<Query, Double>();
		_previousBids = new HashMap<Query, Double>();
		_baseConvProbs = new HashMap<Query, Double>();
		_defaultBid = .4;

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
	public BidBundle getBidBundle(Set<AbstractModel> models) {
		BidBundle bidBundle = new BidBundle();
		if(_day >= 2){
			//NEED TO USE THE MODELS WE ARE PASSED!!!
			buildMaps(models);
			HashMap<Query,IncItem[]> incItems = new HashMap<Query,IncItem[]>();

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
					double w = numClicks*CPC; 				//weight = numClicks * CPC 		[cost]
					double v = numClicks*convProb*salesPrice;	//value = numClicks * convProb * USP		[revenue]

					int isID = _queryId.get(q);
					items[s-1] = new Item(q,w,v,bid,isID);	

				}
				System.out.println("Items for " + q);
				IncItem[] iItems = getIncremental(items);
				incItems.put(q,iItems);
			}

			double budget = _capacity/_capWindow;

			HashMap<Query,Item> solution = fillKnapsack(incItems, budget);

			//set bids
			for(Query q : _querySpace){
				Item item = solution.get(q);
				double bid;
				double weight;
				if(item.v() > 0) {
					bid = item.b();
					weight = item.w();
				}
				else {
					bid = 0;
					weight = 0;
				}

				bidBundle.addQuery(q, bid, new Ad(), weight);
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
				bidBundle.addQuery(q, bid, new Ad(), bid*((_capacity/_capWindow)/16.0));
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
	private HashMap<Query,Item> fillKnapsack(HashMap<Query, IncItem[]> incItems, double budget){
		/*
		 * This will hold the index into the 
		 */
		HashMap<Query,Integer> temp = new HashMap<Query, Integer>();
		for(Query q : _querySpace) {
			temp.put(q,-1);
			System.out.println("Num Inc Items " + q + ": " + incItems.get(q).length);
		}
		
		//TODO FOR TESTING
		
		budget *= 5;
		
		while(budget > 0) {
			/*
			 * Move up one slot in the query with the highest efficiency
			 */
			double besteff = -Double.MAX_VALUE;
			Query bestQ = null;
			for(Query q : _querySpace) {
				int currIdx = temp.get(q);
				IncItem[] incItem = incItems.get(q);
				if(currIdx + 1 < incItem.length) {
					double eff = incItem[currIdx + 1].eff();
					if(eff > besteff) {
						besteff = eff;
						bestQ = q;
					}
				}
			}
			temp.put(bestQ, temp.get(bestQ)+1);
			/*
			 * Subtract numConversions = Value/SalesPrice from the budget
			 */
			budget -= incItems.get(bestQ)[temp.get(bestQ)].v()/_salesPrices.get(bestQ);
		}

		HashMap<Query,Item> solution = new HashMap<Query, Item>();

		for(Query q : _querySpace) {
			if(temp.get(q) >= 0) {
				solution.put(q,incItems.get(q)[temp.get(q)].item());
			}
			else {
				solution.put(q, new Item(q,0,0,0,-1));
			}
			System.out.println("Slot for " + q + ": " + temp.get(q));
		}

		return solution;
	}

	/**
	 * Get undominated items
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
			if(lastUndominated.v() < items[i].v()) {
				temp.add(items[i]);
			}
		}

		//items now contain only undominated items
		items = temp.toArray(new Item[0]);

		//remove lp-dominated items
		LinkedList<Item> q = new LinkedList<Item>();
		q.add(new Item(new Query(),0,0,-1,-1));//add item with zero weight and value

		for(int i=0; i<items.length; i++) {
			q.add(items[i]);//has at least 2 items now
			int l = q.size()-1;
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
		return uItems;
	}


	/**
	 * Get incremental items
	 * @param items
	 * @return
	 */
	public static IncItem[] getIncremental(Item[] items) {
		for(int i = 0; i < items.length; i++) {
			System.out.println("\t" + items[i]);
		}
		
		Item[] uItems = getUndominated(items);
		IncItem[] ii = new IncItem[uItems.length];

		if (uItems.length != 0){ //getUndominated can return an empty array
			ii[0] = new IncItem(uItems[0].w(), uItems[0].v(), uItems[0]);
			for(int item=1; item<uItems.length; item++) {
				Item prev = uItems[item-1];
				Item cur = uItems[item];
				ii[item] = new IncItem(cur.w() - prev.w(), cur.v() - prev.v(), cur);
			}
		}

		return ii;
	}


}
