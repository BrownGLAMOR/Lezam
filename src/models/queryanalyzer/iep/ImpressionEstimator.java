package models.queryanalyzer.iep;

import java.util.HashMap;
import java.util.Map;

import models.queryanalyzer.ds.QAInstance;

public class ImpressionEstimator {
	private static int SAMPLING_FACTOR = 30;
	private int _samplingImpressions;

	private int _nodeId;
	private Map<Integer, IESolution> _solutions;
	private int _bestNode;
	private int _bestImprUB;
	private int _combinedObjectiveBound;
	//private int _imprObjectiveBound;
	//private int _slotObjectiveBound;

	private int _advertisers;
	private int _slots;
	private double[] _trueAvgPos;
	private int _ourIndex;
	private int _ourImpressions;
	private int _imprUB;
	private int[] _agentImprUB;
	private int[] _agentImprLB;


	public ImpressionEstimator(QAInstance inst){
		_advertisers = inst.getNumAdvetisers();
		_slots = inst.getNumSlots();
		_trueAvgPos = inst.getAvgPos();
		_ourIndex = inst.getAgentIndex(); //TODO is this ID or Index?
		_ourImpressions = inst.getImpressions();
		_imprUB = inst.getImpressionsUB();
		_samplingImpressions = _imprUB / SAMPLING_FACTOR + 1;
		
		//System.out.println("samp impr: " + _samplingImpressions);
		
		assert _ourImpressions > 0;

		_agentImprLB = new int[_advertisers];
		_agentImprUB = new int[_advertisers];

		for(int i=0; i < _advertisers; i++){
			_agentImprLB[i] = 1;
			_agentImprUB[i] = inst.getImpressionsUB();
		}
		
		if(((_trueAvgPos[_ourIndex] * 1000) % 1000) == 0){
			//_agentImprLB[_ourIndex] = _ourImpressions-_samplingImpressions;
			_agentImprUB[_ourIndex] = _ourImpressions+_samplingImpressions;
		}

	}

	private int getPosition(int advertiserIndex, int[] order){
		assert(order.length > advertiserIndex);
		for(int i=0; i < order.length; i++){
			if(order[i] == advertiserIndex){
				return i;
			}
		}

		assert(false) : "the advertiserIndex value should exist in the array above!";
		return -1;
	}

	private boolean feasibleOrder(int[] order) {
		for(int i=0; i < order.length; i++){
			int startPos = Math.min(i+1,_slots);
			if(startPos < _trueAvgPos[order[i]]){
				return false;
			}
		}
		return true;
	}

	public IEResult search(int[] order){ //TODO incorporate order
		if(!feasibleOrder(order)){
			//System.out.println("order is infeasible"); 
			return null;
		}

		//System.out.println(_ourIndex); 
		//int minSlotImpr = calcMinSlotImpressions(Math.min(getPosition(_ourIndex, order)+1,_slots), (int)_trueAvgPos[_ourIndex], _ourImpressions, _trueAvgPos[_ourIndex]);
		//System.out.println(minSlotImpr); 
		//_agentImprLB[order[0]] = Math.max(_agentImprLB[order[0]], minSlotImpr);

		_nodeId = 0;
		_solutions = new HashMap<Integer, IESolution>();
		_combinedObjectiveBound = _imprUB*_samplingImpressions + 1;
		_bestImprUB = Integer.MAX_VALUE;//_imprUB*_samplingImpressions + 1;

		int[] agentImpr = new int[_advertisers];
		for(int i=0; i < _advertisers; i++){
			agentImpr[i] = 0;
		}
		int[] slotImpr = new int[_slots];
		for(int i=0; i < _slots; i++){
			slotImpr[i] = 0;
		}

		checkImpressions(0, agentImpr, slotImpr, order);

		IESolution sol =  _solutions.get(_bestNode);
		if(sol == null) {
			assert(false);
			return null;
		}
		else {
			return new IEResult(_combinedObjectiveBound, sol._agentImpr, copyArray(order), sol._slotImpr);
		}
	}

	void checkImpressions(int currIndex, int[] agentImpr, int[] slotImpr, int[] order){
		_nodeId++;
		
		
		/*
		 * TODO: for all slot impr, if first slot slotImpr > IMP UB then return
		 * 
		 */
		
		
		//System.out.println(_advertisers); 
		if(currIndex >= _advertisers){
			int imprObjVal = Math.abs(agentImpr[_ourIndex] - _ourImpressions);
			int slotImprObjVal = Math.max(0, slotImpr[0] - _imprUB);
			int slotObjVal = 1;
			for(int i=1; i < slotImpr.length-1; i++){
				//slotObjVal = Math.abs(slotImpr[i] - slotImpr[i+1]);
				if(Math.abs(slotImpr[i] - slotImpr[i+1]) < _samplingImpressions/2){
					slotObjVal += 1;
				} else {
					break;
				}
			}
			int combinedObj = (imprObjVal+slotImprObjVal)/slotObjVal;///_slots;

			//assert(objVal <= ObjBound);
			//System.out.println(_nodeId + " - " + Arrays.toString(agentImpr) + " - " + Arrays.toString(slotImpr) + " - " + combinedObj);
			//System.out.println(_nodeId + " - " + Arrays.toString(agentImpr) + " - " + Arrays.toString(slotImpr) + " - " + combinedObj + " - " + _combinedObjectiveBound);
			if(combinedObj < _combinedObjectiveBound){
				
				_combinedObjectiveBound = combinedObj;
				_solutions.put(_nodeId, new IESolution(agentImpr, slotImpr));
				_bestNode = _nodeId;
				
				if(slotImpr[0] < _bestImprUB){
					_bestImprUB = slotImpr[0];
				}
			}
			else {
				//System.out.println("Throw out: "+_nodeId + " - " + Arrays.toString(agentImpr) + " - " + Arrays.toString(slotImpr) + " - " + combinedObj+" - "+_combinedObjectiveBound);
			}
			return;
		}

		if(slotImpr[0] > _imprUB && slotImpr[0] > _bestImprUB){
			//System.out.println("CUT: " + _nodeId + " - " + Arrays.toString(agentImpr) + " - " + Arrays.toString(slotImpr) + " - " + _imprUB);
			return; //this is infeasible
		}
		
		int currAgent = order[currIndex];

		int bestImpr = calcMinImpressions(slotImpr, currIndex, _trueAvgPos[currAgent]);	

		if(bestImpr > 0){
			if(currAgent == _ourIndex){
				/*
				int objVal = Math.abs(bestImpr - _ourImpressions);
				if(objVal >= _objectiveBound){
					return;
				}
				 */
			}

			agentImpr[currAgent] = bestImpr;
			int[] newSlotImpr = fillSlots(slotImpr, bestImpr);
			checkImpressions(currIndex+1, agentImpr, newSlotImpr, order);
		}
		else {
			int firstSlot = Math.min(currIndex+1, _slots);
			int maxImpr = (firstSlot == 1) ? _agentImprUB[currAgent] : slotImpr[firstSlot-2]-1;

			if(firstSlot == 1){
				for(int i=_agentImprLB[currAgent]; i <= maxImpr; i++){
					if(i % _samplingImpressions == 0 || i == maxImpr || i==_agentImprLB[currAgent]){
						int[] agentImprCopy = copyArray(agentImpr);
						agentImprCopy[currAgent] = i;
						int[] newSlotImpr = fillSlots(slotImpr, i);
						
						checkImpressions(currIndex+1, agentImprCopy,  newSlotImpr, order);
					}
				}
			}
			else {
				for(int i=maxImpr; i >= _agentImprLB[currAgent] ; i--){
					if(i % _samplingImpressions == 0 || i == maxImpr || i==_agentImprLB[currAgent]){
						int[] agentImprCopy = copyArray(agentImpr);
						agentImprCopy[currAgent] = i;
						int[] newSlotImpr = fillSlots(slotImpr, i);
						checkImpressions(currIndex+1, agentImprCopy,  newSlotImpr, order);
					}
				}
			}
		}
	}

	private int calcMinImpressions(int[] slotImpr, int currIndex, double trueAvgPos) {
		int firstSlot = Math.min(currIndex+1, _slots);
		//int lastSlot = firstSlot;

		double approxAvgPos = firstSlot;
		if(approxAvgPos == trueAvgPos){
			return -1;
		}

		//cout << na << " - " << trueAvgPos << " - " << slotImpr << endl;

		int fullSlots = firstSlot;
		//forall(i in 2..firstSlot) by (-i){
		for(int i=firstSlot; i > 1; i--){
			int impTmp = 0;
			int wImpTmp = 0;
			for(int j=firstSlot; j >= i; j--){
				impTmp += slotImpr[j-2]-slotImpr[j-1];
				wImpTmp += j*(slotImpr[j-2]-slotImpr[j-1]);
			}
			approxAvgPos = wImpTmp / (double)impTmp;
			//cout << approxAvgPos << endl;

			if(approxAvgPos < trueAvgPos){
				break;
			}

			fullSlots = i;
		}
		int finalSlot = fullSlots - 1; 
		int fullSlotImp = 0;
		int wImp = 0;

		for(int i=fullSlots; i <= firstSlot; i++){
			fullSlotImp += slotImpr[i-2]-slotImpr[i-1];
			wImp += i*(slotImpr[i-2]-slotImpr[i-1]);
		}

		if(fullSlotImp <= 0){
			//cout << fullSlots..firstSlot << " - " << finalSlot << " - " << wImp << endl;
			//assert(false);
			//TODO this is bad and it happens... so this is an ugly hack to fix it,
			fullSlotImp = 1;
			wImp = firstSlot;
		}

		//cout << fullSlots..firstSlot << " - " << finalSlot << " - " << wImp << endl;
		//cout << fullSlotImp*trueAvgPos/(float)scale << " - " << trueAvgPos/(float)scale << endl;
		//cout << ((wImp - fullSlotImp*trueAvgPos/(float)scale)/(trueAvgPos/(float)scale - finalSlot)) << endl;
		//int finalSlotImp = (int)ceil((wImp - fullSlotImp*trueAvgPos)/(trueAvgPos - finalSlot));
		int finalSlotImp = (int)((wImp - fullSlotImp*trueAvgPos)/(trueAvgPos - finalSlot));
		if(finalSlotImp < 1){
			finalSlotImp = 1;
		}
		//cout << finalSlotImp << endl;

		//cout << fullSlots..firstSlot << " - " << finalSlot << " - " << wImp << " - " << fullSlotImp << " - " << finalSlotImp << endl;


		return fullSlotImp + finalSlotImp;
	}	

	private int[] fillSlots(int[] slotImpr, int newImpr) {
		int[] newSlotImpr = copyArray(slotImpr);

		int firstSlot = 0;
		while(firstSlot < slotImpr.length-1){
			if(slotImpr[firstSlot] > 0){
				firstSlot++;
			}
			else {
				break;
			}
		}

		int remainingImp = newImpr;
		for(int s=firstSlot; s >= 0; s--) {
			if(s == 0){
				newSlotImpr[s] += remainingImp;
			}
			else {
				int r = slotImpr[s-1] - slotImpr[s];
				assert(r >= 0);
				if(r < remainingImp){
					remainingImp -= r;
					newSlotImpr[s] += r;
				} else {
					newSlotImpr[s] += remainingImp;
					break;
				}
			}
		}

		return newSlotImpr;
	}

	private int calcMinSlotImpressions(int startSlot, int endSlot, int impressions, double avgPos) {
		//System.out.println(startSlot + " " + endSlot);
		assert(startSlot >= endSlot);
		if(startSlot == endSlot){
			return impressions;
		}
		else {
			int minSlotImpr = (int)((avgPos*impressions - endSlot*impressions) / (startSlot - endSlot));
			return minSlotImpr;
		}
	}

	private int[] copyArray(int[] arr){
		int[] newArr = new int[arr.length];
		for(int i=0; i < arr.length; i++){
			newArr[i] = arr[i];
		}
		return newArr;
	}

	private class IESolution {
		public int[] _agentImpr;
		public int[] _slotImpr;

		public IESolution(int[] agentImpr, int[] slotImpr){
			_agentImpr = agentImpr;
			_slotImpr = slotImpr;
		}
	}
}
