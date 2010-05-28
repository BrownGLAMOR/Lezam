package models.queryanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimator;

public class GreedyQueryAnalyzer extends AbstractQueryAnalyzer {

	private HashMap<Query,ArrayList<IEResult>> _allResults;
	private HashMap<Query,ArrayList<int[]>> _allAgentIDs;
	private HashMap<Query,ArrayList<int[][]>> _allImpRanges;
	private Set<Query> _querySpace;
	private HashMap<String,Integer> _advToIdx;
	private ArrayList<String> _advertisers;
	private String _ourAdvertiser;
	public final static int NUM_SLOTS = 5;

	public GreedyQueryAnalyzer(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser) {
		_querySpace = querySpace;

		_allResults = new HashMap<Query,ArrayList<IEResult>>();
		_allImpRanges = new HashMap<Query,ArrayList<int[][]>>();
		_allAgentIDs = new HashMap<Query,ArrayList<int[]>>();
		for(Query q : _querySpace) {
			ArrayList<IEResult> resultsList = new ArrayList<IEResult>();
			_allResults.put(q, resultsList);
			
			ArrayList<int[][]> impRanges = new ArrayList<int[][]>();
			_allImpRanges.put(q,impRanges);
			
			ArrayList<int[]> agentIDs = new ArrayList<int[]>();
			_allAgentIDs.put(q, agentIDs);
		}

		_advertisers = advertisers;
		
		_ourAdvertiser = ourAdvertiser;

		_advToIdx = new HashMap<String,Integer>();
		for(int i = 0; i < advertisers.size(); i++) {
			_advToIdx.put(advertisers.get(i), i);
		}
	}

	@Override
	public int getImpressionsPrediction(Query q, String adv) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			int[] agentIDs = _allAgentIDs.get(q).get(_allAgentIDs.get(q).size()-1);
			for(int i = 0; i < agentIDs.length; i++) {
				if(_advToIdx.get(adv) == agentIDs[i]) {
					return _allResults.get(q).get(size-1).getSol()[i];
				}
			}
		}
		return 0;
	}

	@Override
	public int[] getImpressionsPrediction(Query q) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allResults.get(q).get(size-1).getSol();
		}
		return null;
	}

	@Override
	public int getOrderPrediction(Query q, String adv) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			int[] agentIDs = _allAgentIDs.get(q).get(_allAgentIDs.get(q).size()-1);
			for(int i = 0; i < agentIDs.length; i++) {
				if(_advToIdx.get(adv) == agentIDs[i]) {
					return _allResults.get(q).get(size-1).getOrder()[i];
				}
			}
		}
		return -1;
	}

	@Override
	public int[] getOrderPrediction(Query q) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allResults.get(q).get(size-1).getOrder();
		}
		return null;
	}
	
	@Override
	public int[] getImpressionRangePrediction(Query q, String adv) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			int[] agentIDs = _allAgentIDs.get(q).get(_allAgentIDs.get(q).size()-1);
			for(int i = 0; i < agentIDs.length; i++) {
				if(_advToIdx.get(adv) == agentIDs[i]) {
					return _allImpRanges.get(q).get(size-1)[i];
				}
			}
		}
		return null;
	}

	@Override
	public int[][] getImpressionRangePrediction(Query q) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allImpRanges.get(q).get(size-1);
		}
		return null;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle, HashMap<Query,Integer> maxImps) {
		for(Query q : _querySpace) {
			ArrayList<Double> allAvgPos = new ArrayList<Double>();
			ArrayList<Integer> agentIds = new ArrayList<Integer>();
			int agentOffset = 0;
			for(int i = 0; i < _advertisers.size(); i++) {
				double avgPos;
				if(_advertisers.get(i).equals(_ourAdvertiser)) {
					avgPos = queryReport.getPosition(q);
					agentOffset++;
				}
				else {
					avgPos = queryReport.getPosition(q, "adv" + (i+2-agentOffset));
				}
				
				if(!Double.isNaN(avgPos)) {
					agentIds.add(i);
					allAvgPos.add(avgPos);
				}
			}
			
			double[] allAvgPosArr = new double[allAvgPos.size()];
			for(int i = 0; i < allAvgPosArr.length; i++) {
				allAvgPosArr[i] = allAvgPos.get(i);
			}
			
			int[] agentIdsArr = new int[agentIds.size()];
			for(int i = 0; i < agentIdsArr.length; i++) {
				agentIdsArr[i] = agentIds.get(i);
			}
			
			//this should maybe be squashed bid
			QAInstance inst = new QAInstance(NUM_SLOTS, allAvgPos.size(), allAvgPosArr, agentIdsArr, _advToIdx.get(_ourAdvertiser), queryReport.getImpressions(q), maxImps.get(q));

//			System.out.println(inst);

			int[] avgPosOrder = inst.getAvgPosOrder();

			ImpressionEstimator ie = new ImpressionEstimator(inst);

			IEResult bestSol;
			if(avgPosOrder.length > 0) {
				bestSol = ie.search(avgPosOrder);
				if(bestSol.getSol() == null) {
					int[] imps = new int[avgPosOrder.length];
					bestSol = new IEResult(0, imps, avgPosOrder);
				}
			}
			else {
				int[] imps = new int[avgPosOrder.length];
				bestSol = new IEResult(0, imps, avgPosOrder);
			}
			_allResults.get(q).add(bestSol);
			_allImpRanges.get(q).add(greedyAssign(5,bestSol.getSol().length,bestSol.getOrder(),bestSol.getSol()));
			_allAgentIDs.get(q).add(agentIdsArr);
		}
		return true;
	}


	@Override
	public AbstractModel getCopy() {
		return new GreedyQueryAnalyzer(_querySpace,_advertisers,_ourAdvertiser);
	}

	@Override
	public void setAdvertiser(String ourAdv) {
		_ourAdvertiser = ourAdv;
	}
}
