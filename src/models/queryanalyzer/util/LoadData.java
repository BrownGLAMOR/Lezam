package models.queryanalyzer.util;

import models.queryanalyzer.ds.AdvertiserInfo;
import models.queryanalyzer.ds.AdvertiserInfoExactOnly;
import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QADataExactOnly;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;


public class LoadData {
	static public QADataAll LoadIt(String fileName){
		String instanceString = fileToString(fileName);
		
		//System.out.println(instanceString);
		
		StringTokenizer tokens = new StringTokenizer(instanceString);
		
		int agents = Integer.parseInt(tokens.nextToken());
		int slots = Integer.parseInt(tokens.nextToken());
		int ourAgentNum= Integer.parseInt(tokens.nextToken());
		AdvertiserInfo[] agentInfo = new AdvertiserInfo[agents];
		
		for(int a = 0; a < agents; a++){
			int id = Integer.parseInt(tokens.nextToken());
			String agent= tokens.nextToken();
			double avgPos = Double.parseDouble(tokens.nextToken());
			int impressions = Integer.parseInt(tokens.nextToken());
			double bid = Double.parseDouble(tokens.nextToken());
			double budget = Double.parseDouble(tokens.nextToken());
			double sampledAveragePositions = Double.parseDouble(tokens.nextToken());
			double impsDistMean = Double.parseDouble(tokens.nextToken());
			double impsDistStdev = Double.parseDouble(tokens.nextToken());

			agentInfo[a] = new AdvertiserInfo(id, agent ,avgPos, impressions, bid, budget,sampledAveragePositions,impsDistMean,impsDistStdev);
		}

		return new QADataAll(agents, slots, ourAgentNum, agentInfo);
	}
	
	static public QADataExactOnly LoadItExactOnly(String fileName){
		String instanceString = fileToString(fileName);
		
		//System.out.println(instanceString);
		
		StringTokenizer tokens = new StringTokenizer(instanceString);
		
		int agents = Integer.parseInt(tokens.nextToken());
		int slots = Integer.parseInt(tokens.nextToken());
		AdvertiserInfoExactOnly[] agentInfo = new AdvertiserInfoExactOnly[agents];
		
		for(int a = 0; a < agents; a++){
			int id = Integer.parseInt(tokens.nextToken());
			double avgPos = Double.parseDouble(tokens.nextToken());
			int impressions = Integer.parseInt(tokens.nextToken());
			double bid = Double.parseDouble(tokens.nextToken());
			double budget = Double.parseDouble(tokens.nextToken());

			agentInfo[a] = new AdvertiserInfoExactOnly(id, avgPos, impressions, bid, budget);
		}

		
		return new QADataExactOnly(agents, slots, agentInfo);
	}
	
	private static String fileToString(String fileLoc) {
		StringBuffer fileData = new StringBuffer();

		try {
			FileReader fstream = new FileReader(fileLoc);
			BufferedReader in = new BufferedReader(fstream);
			while(in.ready()){
				fileData.append(in.readLine()).append("\n");
			}
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return fileData.toString();
	}
}
