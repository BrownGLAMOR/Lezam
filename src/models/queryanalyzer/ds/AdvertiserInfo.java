package models.queryanalyzer.ds;

public class AdvertiserInfo {
	public int id;
	public String agentName;
	public double avgPos;
	public int impressions;
	public double bid;
	public double budget;
	public double sampledAveragePositions; 
	public double impsDistMean; 
	public double impsDistStdev; 

	public AdvertiserInfo(int id,String agentName, double avgPos, int impressions, double bid, double budget, 
	double sampledAveragePositions, double impsDistMean, double impsDistStdev)
	{
		this.id = id;
		this.agentName=agentName;
		this.avgPos = avgPos;
		this.impressions = impressions;
		this.bid = bid;
		this.budget = budget;
		this.sampledAveragePositions=sampledAveragePositions;
		this.impsDistMean=impsDistMean;
		this.impsDistStdev=impsDistStdev;
	}
	
	public String toString() {
		return id+" "+agentName+ " " + avgPos + " " + impressions + " " + bid + " " + budget+" "+sampledAveragePositions
		+" "+impsDistMean+" "+impsDistStdev;
	}
}
