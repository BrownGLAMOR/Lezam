package models.queryanalyzer.ds;
import java.util.Arrays;


public class QAInstance {
	private int _slots;
	private int _advetisers;
	private double[] _avgPos;
	private int[] _agentIds;
	private int _agentIndex;
	private int _impressions;
	private int _impressionsUB;
	
	public QAInstance(int slots, int advetisers, double[] avgPos, int[] agentIds, int agentIndex, int impressions, int impressionsUB){
		assert(avgPos.length == advetisers);
		assert(agentIds.length == advetisers);
		assert(advetisers > agentIndex && agentIndex >= 0);
		_slots = slots;
		_advetisers = advetisers;
		_avgPos = avgPos;
		_agentIds = agentIds;
		_agentIndex = agentIndex;
		_impressions = impressions;
		_impressionsUB = impressionsUB;
	}
	
	public int getNumSlots() {return _slots;}
	public int getNumAdvetisers() {return _advetisers;}
	public double[] getAvgPos() {return _avgPos;}
	public int[] getAgentIds() {return _agentIds;}
	public int getAgentIndex() {return _agentIndex;}
	public int getImpressions() {return _impressions;}
	public int getImpressionsUB() {return _impressionsUB;}
	
	public int[] getBidOrder(QAData data){
		double[] bids = new double[_advetisers];
		int[] bidOrder = new int[_advetisers];
		for(int i=0; i < _advetisers; i++){
			bids[i] = data._agentInfo[_agentIds[i]-1].bid;
			bidOrder[i] = i;
		}
		
		sortListsDecending(bidOrder, bids);
		
		//System.out.println("Bid order "+Arrays.toString(bidOrder));
		//System.out.println("Bid value "+Arrays.toString(bids));
		
		return bidOrder;
	}
	
	public int[] getAvgPosOrder() {
		double[] pos = new double[_advetisers];
		int[] posOrder = new int[_advetisers];
		for(int i=0; i < _advetisers; i++){
			pos[i] = -_avgPos[i];
			posOrder[i] = i;
		}
		
		sortListsDecending(posOrder, pos);
		
		//System.out.println("Pos order "+Arrays.toString(posOrder));
		//System.out.println("Pos value "+Arrays.toString(pos));
		
		//not nessissary in general, but makes results consistent with the comet model
		sortTiesAccending(posOrder, pos);
		
		//System.out.println("Pos order (break ties) "+Arrays.toString(posOrder));
		//System.out.println("Pos value (break ties) "+Arrays.toString(pos));
		
		
		return posOrder;
	}
	
	public int[] getAvgPosOrder(QAData data) {
		double[] pos = new double[_advetisers];
		int[] posOrder = new int[_advetisers];
		for(int i=0; i < _advetisers; i++){
			pos[i] = -_avgPos[i];
			posOrder[i] = i;
		}
		
		sortListsDecending(posOrder, pos);
		
		//System.out.println("Pos order "+Arrays.toString(posOrder));
		//System.out.println("Pos value "+Arrays.toString(pos));
		
		//not nessissary in general, but makes results consistent with the comet model
		sortTiesAccending(posOrder, pos);
		
		//System.out.println("Pos order (break ties) "+Arrays.toString(posOrder));
		//System.out.println("Pos value (break ties) "+Arrays.toString(pos));
		
		
		return posOrder;
	}
	
	public int[] getTrueImpressions(QAData data) {
		int[] impressions = new int[_advetisers];
		for(int i=0; i < _advetisers; i++){
			impressions[i] = data._agentInfo[_agentIds[i]-1].impressions;
		}
		
		return impressions;
	}
	
	private void sortListsDecending(int[] ids, double[] vals){
		assert(ids.length == vals.length);
		int length = ids.length;
		
		for(int i=0; i < length; i++){
			for(int j=i+1; j < length; j++){
				if(vals[i] < vals[j]){
					double tempVal = vals[i];
					int tempId = ids[i];
					
					vals[i] = vals[j];
					ids[i] = ids[j];
					
					vals[j] = tempVal;
					ids[j] = tempId;
				}
			}
		}
	}
	
	/**
	 * sorts ties by accending agent id
	 * This has 0 impact on the algorithm, it was added just to have identical results 
	 * with the comet model
	 * @param ids
	 * @param vals
	 */
	private void sortTiesAccending(int[] ids, double[] vals){
		assert(ids.length == vals.length);
		int length = ids.length;
		
		for(int i=0; i < length; i++){
			for(int j=i+1; j < length; j++){
				if(vals[i] == vals[j] && ids[i] > ids[j]){
					double tempVal = vals[i];
					int tempId = ids[i];
					
					vals[i] = vals[j];
					ids[i] = ids[j];
					
					vals[j] = tempVal;
					ids[j] = tempId;
				}
			}
		}
	}
	
	
	public String toString() {
		String temp = "";
		temp += "Slots: "+_slots+"\n";
		temp += "Advertisers: "+_advetisers+"\n";
		temp += Arrays.toString(_avgPos)+"\n";
		temp += "index: "+_agentIndex+"\n";
		temp += "impressions: "+_impressions+"\n";
		return temp;
	}


	
}
