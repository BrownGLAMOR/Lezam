package agents.modelbased;

import java.io.*;
import java.util.*;

import simulator.AgentSimulator;


public class AgentComparator {

	//Arguments and necessary variables	
	
	public static double profit;
	private static int lowSlot = 10;
	private static int highSlot = 1;
	
	public static void main(String[] args) {
		//Set up output files
		String filename = System.getProperty("user.dir")+System.getProperty("file.separator")+Long.toString(System.currentTimeMillis())+"extendedComparator.csv";
		StringWriter stBuff = new StringWriter();
		BufferedWriter buffWrit = null;
		
		double profitTotal1 = 0;
		double profitTotal2 = 0;
		double profitTotal3 = 0;
		long timeTotal1 = 0;
		long timeTotal2 = 0;
		long timeTotal3 = 0;
		
		try{
			buffWrit = new BufferedWriter(new FileWriter(filename, true));
			//whatever header you want
			stBuff.append("GameSet"+",");
			stBuff.append("Game"+",");
			stBuff.append("Agent"+",");
			stBuff.append("Total time"+",");
			stBuff.append("Total profit"+"\n");
			buffWrit.write(stBuff.toString());	
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		//Establish game parameters
		double c1 = 1;
		double c2 = 0;
		double c3 = 0;
		double simUB = 1.45;
		
		boolean PERFECT_SIM = true;
		int multiDayDiscretization = 10;
		int HCDiscretization = 10;
		
		//MCKP.MultiDay agent1 = MCKP.MultiDay.MultiDayOptimizer;
		MCKP.MultiDay agent1 = MCKP.MultiDay.HillClimbing;
		MCKP.MultiDay agent2 = MCKP.MultiDay.DP;
		MCKP.MultiDay agent3 = MCKP.MultiDay.MDPMCKP;
		MCKP.MultiDay agent4 = MCKP.MultiDay.PMCKP;
		
		long timeStart;
		long timeEnd;
		
		ArrayList<String> filenames;
		//Set up simulator
		//May be necessary to do this more than once, or iterate through the set of game sets
		//May even have to do this per game set
		
		//finals2010 set
			//loop through 1297-1340 and 15127-15170
		//The filenames for the first set here aren't set up properly in AgentSimulator.
//		for (int i = 1297; i<=1320; i++){
//			stBuff = new StringWriter();
//			filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.finals2010, i, i);
//
//			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.finals2010, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent1, multiDayDiscretization, PERFECT_SIM);
//			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("finals2010"+",");
//				stBuff.append(i+",");
//				stBuff.append("MultiDayOptimizer"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();
//			}
//			profitTotalMDO += profit;
//			timeTotalMDO += (timeEnd-timeStart)/1000;
//			
//
//			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.finals2010, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent2, multiDayDiscretization, PERFECT_SIM);
//			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("finals2010"+",");
//				stBuff.append(i+",");
//				stBuff.append("HillClimbing"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();
//			}
//			
//			profitTotalHC += profit;
//			timeTotalHC += (timeEnd-timeStart)/1000;
//			
//			if (buffWrit != null){
//				try {
//					buffWrit.write(stBuff.toString());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		
//		for (int i = 15145; i<=15160; i++){
//			stBuff = new StringWriter();
//			filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.finals2010, i, i);
//
//			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.finals2010, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent1, multiDayDiscretization, PERFECT_SIM);
//			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("finals2010"+",");
//				stBuff.append(i+",");
//				stBuff.append("MultiDayOptimizer"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();
//			}
//			
//			profitTotalMDO += profit;
//			timeTotalMDO += (timeEnd-timeStart)/1000;
//
//
//			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.finals2010, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent2, HCDiscretization, PERFECT_SIM);
//			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("finals2010"+",");
//				stBuff.append(i+",");
//				stBuff.append("HillClimbing"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();			
//			}
//			
//			profitTotalHC += profit;
//			timeTotalHC += (timeEnd-timeStart)/1000;
//			
//			if (buffWrit != null){
//				try {
//					buffWrit.write(stBuff.toString());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
		
//		
		//semi2011server1 set
			//loop through 1410-1445 (1418-1433) (1418-1427)
		
		//int[] daysToLook = {1,2,5,10};
		int[] daysToLook = {5};
		int[] timeConstraint = {2000000};
		for(int d = 0; d<daysToLook.length; d++){
			
		for (int i = 1418; i<=1419; i++){
			stBuff = new StringWriter();
			filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.semi2011server1, i, i);
			
			timeStart = System.currentTimeMillis();
			AgentSimulator.simulateAgent(daysToLook[d], timeConstraint[0], lowSlot, highSlot, filenames, 
					AgentSimulator.GameSet.semi2011server1, 1, c1, c2, c3, simUB, 
					simUB, simUB, simUB, simUB, agent1, multiDayDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
			try{
				stBuff.append("semi2011server1"+",");
				stBuff.append(i+",");
				stBuff.append(agent1.toString()+",");
				stBuff.append((timeEnd-timeStart)/1000+",");
				stBuff.append(profit+"\n");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			profitTotal1 += profit;
			timeTotal1 += (timeEnd-timeStart)/1000;
			
			
			timeStart = System.currentTimeMillis();
			AgentSimulator.simulateAgent(daysToLook[d],timeConstraint[0],lowSlot, highSlot, filenames, 
					AgentSimulator.GameSet.semi2011server1, 1, c1, c2, c3, simUB, 
					simUB, simUB, simUB, simUB, agent2, multiDayDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
			try{
				stBuff.append("semi2011server1"+",");
				stBuff.append(i+",");
				stBuff.append(agent2.toString()+",");
				stBuff.append((timeEnd-timeStart)/1000+",");
				stBuff.append(profit+"\n");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			profitTotal2 += profit;
			timeTotal2 += (timeEnd-timeStart)/1000;
			
			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(daysToLook[d],timeConstraint[0], lowSlot, highSlot, filenames, 
//					AgentSimulator.GameSet.semi2011server1, 1, c1, c2, c3, simUB, 
//					simUB, simUB, simUB, simUB, agent3, HCDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
			try{
				stBuff.append("semi2011server1"+",");
				stBuff.append(i+",");
				stBuff.append(agent3.toString()+",");
				stBuff.append((timeEnd-timeStart)/1000+",");
				stBuff.append(profit+"\n");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			profitTotal3 += profit;
			timeTotal3 += (timeEnd-timeStart)/1000;
			
			if (buffWrit != null){
				try {
					
					buffWrit.write(stBuff.toString());
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		//semi2011server2 set
		//Start ordinarily at 621-640 (621-625)
	for (int i = 621; i<=620; i++){
			stBuff = new StringWriter();
	
			filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.semi2011server2, i, i);
			
			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(58, lowSlot, highSlot, filenames, 
//					AgentSimulator.GameSet.semi2011server2, 1, c1, c2, c3, simUB, 
//					simUB, simUB, simUB, simUB, agent1, multiDayDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("semi2011server2"+",");
//				stBuff.append(i+",");
//				stBuff.append("HC"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();
//			}
			
			profitTotal1 += profit;
			timeTotal1 += (timeEnd-timeStart)/1000;
			
			timeStart = System.currentTimeMillis();
			AgentSimulator.simulateAgent(lowSlot, highSlot, filenames, 
					AgentSimulator.GameSet.semi2011server2, 1, c1, c2, c3, simUB, 
					simUB, simUB, simUB, simUB, agent2, HCDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
			try{
				stBuff.append("semi2011server2"+",");
				stBuff.append(i+",");
				stBuff.append("DP"+",");
				stBuff.append((timeEnd-timeStart)/1000+",");
				stBuff.append(profit+"\n");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			profitTotal2 += profit;
			timeTotal2 += (timeEnd-timeStart)/1000;
			
			if (buffWrit != null){
				try {
					buffWrit.write(stBuff.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
//			stBuff = new StringWriter();
//			
//			filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.semi2011server2, i, i);			
//			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.semi2011server2, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent1, multiDayDiscretization, PERFECT_SIM);
//			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("semi2011server2"+",");
//				stBuff.append(i+",");
//				stBuff.append("MultiDayOptimizer"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();
//			}
//			
//			profitTotalMDO += profit;
//			timeTotalMDO += (timeEnd-timeStart)/1000;
//			
//			timeStart = System.currentTimeMillis();
//			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.semi2011server2, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent2, HCDiscretization, PERFECT_SIM);
//			timeEnd = System.currentTimeMillis();
//			try{
//				stBuff.append("semi2011server2"+",");
//				stBuff.append(i+",");
//				stBuff.append("HillClimbing"+",");
//				stBuff.append((timeEnd-timeStart)/1000+",");
//				stBuff.append(profit+"\n");
//			} catch (Exception e){
//				e.printStackTrace();
//			}
//			
//			profitTotalHC += profit;
//			timeTotalHC += (timeEnd-timeStart)/1000;
//			
//			if (buffWrit != null){
//				try {
//					buffWrit.write(stBuff.toString());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		
		//Cycle through games
			//In theory, same game plus same algorithm equals same results
			//So run a game with algorithm A, then with algorithm B, recording the profit and time taken
			//Tally these up (also marking the blow-by-blow results down) and continue
			//Info collected: Game set, game, agent, time taken for the entire game, average time spent per day, profit, etc
			//Write as you go
		
		//Compare the two, make sure all the results are in the file, and finish
			//Aggregate totals for time, aggregate totals for profit
			//Average difference in time, average difference in profit
		}
		try{
			stBuff = new StringWriter();
			stBuff.append("\n");
			stBuff.append(agent1.toString()+" Profits"+",");
			stBuff.append(agent2.toString()+" Profits"+",");
			stBuff.append(agent3.toString()+" Profits"+",");
			stBuff.append(agent1.toString()+" Time"+",");
			stBuff.append(agent2.toString()+" Time"+",");
			stBuff.append(agent3.toString()+" Time"+",");
			stBuff.append("\n");
			stBuff.append(profitTotal1+","+profitTotal2+","+profitTotal3+","+timeTotal1+","+timeTotal2+","+timeTotal3+","+"\n");
			//stBuff.append(profitTotalHC+","+timeTotalHC+","+"\n");
			buffWrit.write(stBuff.toString());
			buffWrit.flush();
			buffWrit.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	
	}

}
