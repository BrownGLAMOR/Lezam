package models.queryanalyzer.ds;

public class QAData {
	int _agents;
	int _slots;
	AdvertiserInfo[] _agentInfo;
	
	public QAData(int agents, int slots, AdvertiserInfo[] agentInfo){
		assert(agents == agentInfo.length);
		_agents = agents;
		_slots = slots;
		_agentInfo = agentInfo;
	}
	
	/**
	 * 
	 * @param advIndex the value of adv can be a bit missleading.  This is the index of the i-th advetiser after non-participants are dropped
	 * @return
	 */
	public QAInstance buildInstances(int advIndex){
		assert(advIndex < _agents);
		int usedAgents = 0;
		for(int i=0; i < _agents; i++){
			if(_agentInfo[i].avgPos >= 0){
				usedAgents++;
			}
		}
		
		AdvertiserInfo[] usedAgentInfo = new AdvertiserInfo[usedAgents];
		int index = 0;
		for(int i=0; i < _agents; i++){
			if(_agentInfo[i].avgPos >= 0){
				usedAgentInfo[index] = _agentInfo[i];
				index++;
			}
		}
		
		double[] avgPos = new double[usedAgents];
		int[] agentIds = new int[usedAgents];
		int impressionsUB = 0;
		
		for(int i=0; i < usedAgents; i++){
			avgPos[i] = usedAgentInfo[i].avgPos;
			agentIds[i] = usedAgentInfo[i].id;
			impressionsUB += usedAgentInfo[i].impressions;
		}
		
		return new QAInstance(_slots, usedAgents, avgPos, agentIds, advIndex, usedAgentInfo[advIndex].impressions, impressionsUB);
	}
	
	public String toString() {
		String temp = "";
		temp += "Slots: "+_slots+"\n";
		temp += "Agents: "+_agents+"\n";
		for(int i=0; i < _agentInfo.length; i++){
			temp += _agentInfo[i].toString()+"\n";
		}
		return temp;
	}
}
