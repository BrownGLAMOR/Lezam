package models.queryanalyzer.ds;

public class AdvertiserInfo {
	public int id;
	public double avgPos;
	public int impressions;
	public double bid;
	public double budget;
	
	public AdvertiserInfo(int id, double avgPos, int impressions, double bid, double budget){
		this.id = id;
		this.avgPos = avgPos;
		this.impressions = impressions;
		this.bid = bid;
		this.budget = budget;
	}
	
	public String toString() {
		return id + " " + avgPos + " " + impressions + " " + bid + " " + budget;
	}
}
