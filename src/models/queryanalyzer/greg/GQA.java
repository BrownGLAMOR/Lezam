package models.queryanalyzer.greg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.queryanalyzer.AbstractQueryAnalyzer;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimator;

public class GQA extends AbstractQueryAnalyzer {

	private HashMap<Query,ArrayList<IEResult>> _allResults;
	private HashMap<Query,ArrayList<int[]>> _allAgentIDs;
	private HashMap<Query,ArrayList<int[][]>> _allImpRanges;
	private Set<Query> _querySpace;
	private HashMap<String,Integer> _advToIdx;
	private ArrayList<String> _advertisers;
	private String _ourAdvertiser;
	public final static int NUM_SLOTS = 5;

	public GQA(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser) {
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
			int myID = 0;
			double myRealAvgPos = 0;
			double mySampledAvgPos = 0; 
			int curindx = 0;
			for(int i = 0; i < _advertisers.size(); i++) {
				double avgPos;
				if(_advertisers.get(i).equals(_ourAdvertiser)) {
					myID = curindx;;
					mySampledAvgPos = queryReport.getPosition(q,_ourAdvertiser);
					//System.out.println(_ourAdvertiser);
					//avgPos = mySampledAvgPos;
					//System.out.println("Sampled avgPos = " + mySampledAvgPos);
					myRealAvgPos = queryReport.getPosition(q);
					avgPos = myRealAvgPos;
					//System.out.println("Real avgPos is " + avgPos);
					
				}
				else {
					avgPos = queryReport.getPosition(q, "adv" + (i+1));
				}

				if(!Double.isNaN(avgPos)) {
					agentIds.add(i);
					allAvgPos.add(avgPos);
					curindx ++;
				}
			}

			double[] allAvgPosArr = new double[allAvgPos.size()];
			
			double[] sampAvgPosArr;
			//System.out.println("myID = " + myID);
			//System.out.print("allAvgPosArr = ");
			for(int i = 0; i < allAvgPosArr.length; i++) {
				//if(i == myIndx){
				//	sampAvgPosArr
				//}
				allAvgPosArr[i] = allAvgPos.get(i);
				//assert(!Double.isNaN(allAvgPosArr[i]));
				//System.out.print(allAvgPosArr[i] + " ");
			}
			//System.out.println();

			//if there is a mystery positoin include it
			//System.out.println(mySampledAvgPos);
			//System.out.println(Double.isNaN(mySampledAvgPos));
			//System.out.println(myRealAvgPos);
			//System.out.println(!Double.isNaN(myRealAvgPos));
		
			//assert(!Double.isNaN(mySampledAvgPos)&&!Double.isNaN(myRealAvgPos));
			int passedIndx = -1;
			double distance = 100000;
			if(Double.isNaN(mySampledAvgPos) && !Double.isNaN(myRealAvgPos)){
				//create the sampled pos array
				
				
				sampAvgPosArr = new double[allAvgPos.size()-1];
				int indx = 0;
				
				for(int i = 0; i < allAvgPosArr.length; ++i){
					
					
					if(i != myID){
						sampAvgPosArr[indx] = allAvgPosArr[i];
						++indx;
						if(allAvgPosArr[myID]>allAvgPosArr[i] && (allAvgPosArr[myID] - allAvgPosArr[i]) <distance){
							distance = allAvgPosArr[myID] - allAvgPosArr[i];
							passedIndx = indx;
						}
					}
				}
				if(allAvgPosArr[myID] == 1.0){
					indx = -1;
				}
				
			}else{
				sampAvgPosArr = new double[allAvgPos.size()];
				if(!Double.isNaN(mySampledAvgPos)){
					
					for(int i = 0; i < allAvgPosArr.length; ++i){
						if(i != myID){
							sampAvgPosArr[i] = allAvgPosArr[i];
						}else{
							sampAvgPosArr[i] = mySampledAvgPos;
						}
					}
				}else{
					for(int i = 0; i < sampAvgPosArr.length; ++i){
						sampAvgPosArr[i] = allAvgPosArr[i];
					}
					
				}
				
			}
			//System.out.print("SampledAvgPos = [");
			//for(int i = 0; i < sampAvgPosArr.length; ++i){
			//	assert(!Double.isNaN(sampAvgPosArr[i]));
			//	System.out.print(sampAvgPosArr[i] + ", ");
			//}
			//System.out.println("]");
			
			
			int[] agentIdsArr = new int[agentIds.size()];
			//System.out.print("agentIdsArr = ");
			for(int i = 0; i < agentIdsArr.length; i++) {
				agentIdsArr[i] = agentIds.get(i);
				//System.out.print(agentIdsArr[i] + " ");
			}
			//System.out.println();

			QAInstance inst = new QAInstance(NUM_SLOTS, allAvgPos.size(), allAvgPosArr, agentIdsArr, _advToIdx.get(_ourAdvertiser), queryReport.getImpressions(q), maxImps.get(q));
			int[] bestOrder= inst.getAvgPosOrder();
			Algo al = new Algo();
			al.isVerbose(false);
			al.setTimeout(1);
			ArrayList<OrderingSolution> sol = null;
			if(sampAvgPosArr.length >0){
				try{
					sol = al.createImpressions(sampAvgPosArr.length, NUM_SLOTS, sampAvgPosArr);
				}catch(Exception e){}
			}
			
			
			//need to fit in the the true average positoin
			
			
			//int myID;
			//double myRealAvgPos,mySampledAvgPos; 
			
			//sol = null;
			
			//System.out.println("sample and all the same? = " +(sampAvgPosArr.length == allAvgPosArr.length));
			if(sol!= null && sol.size() > 0/*&& sampAvgPosArr.length == allAvgPosArr.length&& false*/){
				
				
			
				int bestIndx = 0;
				int mostSol = 0;
				for(int i = 0; i < sol.size(); ++i){
					OrderingSolution curSol = sol.get(i);
					int solNum = curSol._solutions;
					if(mostSol<solNum){
						mostSol = solNum;
						bestIndx = i;
					}
				}
				bestOrder = sol.get(bestIndx)._ordering;
				
				//System.out.print("bestOrder = [");
				//for(int i = 0; i < bestOrder.length; ++i){
				//	System.out.print(bestOrder[i] + ", ");
				//}
				//System.out.println("]");
				//insert the extra value
				//System.out.println("about to check double " + (Double.isNaN(mySampledAvgPos)) + " " + (!Double.isNaN(myRealAvgPos)));
				if(Double.isNaN(mySampledAvgPos) && !Double.isNaN(myRealAvgPos)){
					//System.out.println("im in");
					if(passedIndx != -1){
						int passedPos = 0;
						for(int i = 0; i < bestOrder.length; ++i){
							if(passedIndx == bestOrder[i]){
								
								passedPos = i;
							}
						}
						//passedINdx is the agent
						//passedPos is the index
						
						
						int neworder = passedPos + 1;
						//in ordering the index is the positoin
						int[] tempordering = new int[bestOrder.length+1];
						int indx = 0;
						for(int i = 0; i < bestOrder.length; ++i){
							if(i == neworder){
								tempordering[i] = myID; 
								++indx;
							}
							
							if(myID<=bestOrder[i]){
								tempordering[indx] = bestOrder[i]+1;
							}else{
								tempordering[indx] = bestOrder[i];
							}
							++indx;
						}
						//System.out.print("tempOrdering = [");
						//for(int i = 0; i < tempordering.length; ++i){
							//System.out.print(tempordering[i] + ", ");
						//}
						//System.out.println("]");
						bestOrder = tempordering;
					}else{
						//put it into the first slot
						
						//passedINdx is the agent
						//passedPos is the index
						
						
						//int neworder = passedPos + 1;
						//in ordering the index is the positoin
						int[] tempordering = new int[bestOrder.length+1];
						int indx = 0;
						for(int i = 0; i < bestOrder.length; ++i){
							if(i == 0){
								tempordering[i] = myID; 
								++indx;
							}
							
							if(myID<=bestOrder[i]){
								tempordering[indx] = bestOrder[i]+1;
							}else{
								tempordering[indx] = bestOrder[i];
							}
							++indx;
						}
						//System.out.print("tempOrdering = [");
						//for(int i = 0; i < tempordering.length; ++i){
						//	System.out.print(tempordering[i] + ", ");
						//}
						//System.out.println("]");
						bestOrder = tempordering;
					}
				}
				//bestOrder = inst.getAvgPosOrder();
			}
			//int[] avgPosOrder = inst.getAvgPosOrder();
			
			
			//System.out.print("avgPosOrder = ");
			//for(int i = 0; i < bestOrder.length; ++i){
			//	System.out.print(bestOrder[i] + " ");
			//}
			//System.out.println("\n");
			IEResult bestSol;
			if(queryReport.getImpressions(q) > 0) {
				if(bestOrder.length > 0) {
					ImpressionEstimator ie = new ImpressionEstimator(inst);
					bestSol = ie.search(bestOrder);
					if(bestSol == null || bestSol.getSol() == null) {
						//System.out.println(q);
						int[] imps = new int[bestOrder.length];
						int[] slotimps = new int[NUM_SLOTS];
						bestSol = new IEResult(0, imps, bestOrder, slotimps);
					}
				}
				else {
					int[] imps = new int[bestOrder.length];
					int[] slotimps = new int[NUM_SLOTS];
					bestSol = new IEResult(0, imps, bestOrder, slotimps);
				}
			}
			else {
				int[] imps = new int[bestOrder.length];
				int[] slotimps = new int[NUM_SLOTS];
				bestSol = new IEResult(0, imps, bestOrder, slotimps);
			}
			_allResults.get(q).add(bestSol);
			_allImpRanges.get(q).add(greedyAssign(5,bestSol.getSol().length,bestSol.getOrder(),bestSol.getSol()));
			_allAgentIDs.get(q).add(agentIdsArr);
		}
		return true;
	}


	@Override
	public AbstractModel getCopy() {
		return new GQA(_querySpace,_advertisers,_ourAdvertiser);
	}

	@Override
	public void setAdvertiser(String ourAdv) {
		_ourAdvertiser = ourAdv;
	}
}
