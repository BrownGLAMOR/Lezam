package agents.modelbased;

import java.io.*;
import java.util.*;

import simulator.AgentSimulator;


public class AgentComparator {

	//Arguments and necessary variables	
	
	public static double profit;
	
	public static void main(String[] args) {
		//Set up output files
		String filename = System.getProperty("user.dir")+System.getProperty("file.separator")+Long.toString(System.currentTimeMillis())+"extendedComparator.csv";
		StringWriter stBuff = new StringWriter();
		BufferedWriter buffWrit = null;
		
		double profitTotalMDO = 0;
		double profitTotalHC = 0;
		long timeTotalMDO = 0;
		long timeTotalHC = 0;
		
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
		
		MCKP.MultiDay agent1 = MCKP.MultiDay.MultiDayOptimizer;
		MCKP.MultiDay agent2 = MCKP.MultiDay.HillClimbing;
		
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
			//loop through 1410-1445
		for (int i = 1418; i<=1418; i++){
			stBuff = new StringWriter();
			filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.semi2011server1, i, i);
			
			timeStart = System.currentTimeMillis();
			AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.semi2011server1, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent1, multiDayDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
			try{
				stBuff.append("semi2011server1"+",");
				stBuff.append(i+",");
				stBuff.append("MultiDayOptimizer"+",");
				stBuff.append((timeEnd-timeStart)/1000+",");
				stBuff.append(profit+"\n");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			profitTotalMDO += profit;
			timeTotalMDO += (timeEnd-timeStart)/1000;
			
			timeStart = System.currentTimeMillis();
			//AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.semi2011server1, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, agent2, HCDiscretization, PERFECT_SIM);
			timeEnd = System.currentTimeMillis();
			try{
				stBuff.append("semi2011server1"+",");
				stBuff.append(i+",");
				stBuff.append("HillClimbing"+",");
				stBuff.append((timeEnd-timeStart)/1000+",");
				stBuff.append(profit+"\n");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			profitTotalHC += profit;
			timeTotalHC += (timeEnd-timeStart)/1000;
			
			if (buffWrit != null){
				try {
					buffWrit.write(stBuff.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		//semi2011server2 set
		//Start ordinarily at 621
//		for (int i = 621; i<=640; i++){
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
		
		//Cycle through games
			//In theory, same game plus same algorithm equals same results
			//So run a game with algorithm A, then with algorithm B, recording the profit and time taken
			//Tally these up (also marking the blow-by-blow results down) and continue
			//Info collected: Game set, game, agent, time taken for the entire game, average time spent per day, profit, etc
			//Write as you go
		
		//Compare the two, make sure all the results are in the file, and finish
			//Aggregate totals for time, aggregate totals for profit
			//Average difference in time, average difference in profit
		
		try{
			stBuff = new StringWriter();
			stBuff.append("\n");
			stBuff.append("MDO Profits"+",");
			stBuff.append("HC Profits"+",");
			stBuff.append("MDO Time"+",");
			stBuff.append("HC Time"+",");
			stBuff.append("\n");
			stBuff.append(profitTotalMDO+","+profitTotalHC+","+timeTotalMDO+","+timeTotalHC+","+"\n");
			buffWrit.write(stBuff.toString());
			buffWrit.flush();
			buffWrit.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
