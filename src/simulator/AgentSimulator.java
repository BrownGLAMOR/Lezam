package simulator;

import agents.AbstractAgent;
import agents.modelbased.AgentComparator;
import agents.modelbased.FastMDPMCKP;
import agents.modelbased.FastPMCKP;
import agents.modelbased.MultipleAgentSimulator;
import agents.modelbased.DP;
import agents.modelbased.DPHill;
import agents.modelbased.JordanHillClimbing;
import agents.modelbased.MCKP;
import agents.modelbased.MultiDayOptimizer;
import agents.modelbased.OneDayHeuristic;
import agents.modelbased.MCKP.MultiDay;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import tacaa.javasim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class AgentSimulator {

	public enum GameSet {
		finals2010, semifinals2010, test2010, semi2011server1, semi2011server2, qual2012
	}


	public static String timeFile = null;
	public static String gameString = null;





	public static ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {

		
		
		String baseDir = "/pro/aa/";
		if (isMac()) {
			String homeDir = System.getProperty("user.home");
			if (homeDir.equals("/Users/sodomka")) baseDir = homeDir + "/Desktop/";
			if (homeDir.equals("/Users/jordanberg"))baseDir = homeDir + "/Desktop/";
		}

		
		
		String baseFile = null;
		switch (GAMES_TO_TEST) {
		case finals2010: 			baseFile = "finals2010/game-tacaa1-"; 		break;
		case semi2011server1: 		baseFile = "tacaa2011/semi/server1/game"; 	break;
		case semi2011server2:		baseFile = "tacaa2011/semi/server2/game";		break;
		case qual2012:				baseFile = "tacaa2012/qual/game"; 			break;
		}

				

		ArrayList<String> filenames = new ArrayList<String>();
		for (int i = gameStart; i <= gameEnd; i++) {
			filenames.add(baseDir + baseFile + i + ".slg");
		}
		return filenames;
	}

	public static PersistentHashMap setupSimulator(String filename) {
		return javasim.setupClojureSim(filename);
	}

	public static void simulateAgent(int lowS, int highS, ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh) {
		MCKP.MultiDay multiDay = MCKP.MultiDay.MultiDayOptimizer;
		int multiDayDiscretization = 50;
		boolean PERFECT_SIM = true;
		simulateAgent(lowS, highS, filenames, gameSet, agentNum, c1, c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh, multiDay, multiDayDiscretization, PERFECT_SIM);
	}

	
	public static void simulateAgent(int lowS, int highS, ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh,
			MCKP.MultiDay multiDay, int multiDayDiscretization, boolean PERFECT_SIM) {
		String[] agentsToReplace;
		if(agentNum == 0) {
			agentsToReplace = new String[] {"TacTex"};
		}
		else if(agentNum == 1) {
			agentsToReplace = new String[] {"Schlemazl"};
		}
		else {
			agentsToReplace = new String[] {"Schlemazl", "TacTex"};
		}
		//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor"};
		//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor" , "MetroClick", "tau", "Nanda_AA", "crocodileagent", "McCon"};

		
		
		double totalProfitdiff = 0.0;
		double totalProfit = 0.0;
		for(String filename : filenames) {
			long gameStart = System.currentTimeMillis();
			PersistentHashMap cljSim = setupSimulator(filename);
			for(String agentToReplace : agentsToReplace) {

				if(gameSet.equals(GameSet.semi2011server2) && agentToReplace.equals("TacTex")) {
					gameString = filename.substring(filename.length()-7);
					if(gameString.equals("621.slg") ||
							gameString.equals("622.slg") ||
							gameString.equals("623.slg") ||
							gameString.equals("624.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("TacTex")) {
					gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1426.slg") ||
							gameString.equals("1427.slg") ||
							gameString.equals("1428.slg") ||
							gameString.equals("1429.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("Schlemazl")) {
					gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1434.slg") ||
							gameString.equals("1435.slg") ||
							gameString.equals("1436.slg") ||
							gameString.equals("1437.slg")) {
						continue;
					}
				}
				//for testing purposes
				timeFile = Long.toString(System.currentTimeMillis());
				Boolean fileDir = (new File(System.getProperty("user.dir")+System.getProperty("file.separator")+"Details").mkdirs());
				if(!fileDir){
					System.out.println("Error: Directory not created");
				}
				AbstractAgent agent;
				if(PERFECT_SIM) {
					agent = makeAgent(cljSim, agentToReplace,c1,c2,c3,multiDay, multiDayDiscretization, filename);
				}
				else {
					agent = makeAgent(lowS, highS, c1,c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh,multiDay,multiDayDiscretization);
					//               agent = new EquatePPSSimple2010();
				}
				
				PersistentArrayMap results = javasim.simulateAgent(cljSim, agent, agentToReplace);
				//tester.closeFile();
				PersistentArrayMap actual = javasim.getActualResults(cljSim);
				            System.out.println("Results of " + filename + " Sim: \n"); 
				            javasim.printResults(results);
				            System.out.println("Results of " + filename + " Actual: \n");
				            javasim.printResults(actual);
				ArrayList profDiff = javasim.compareResults(results,actual,agentToReplace);
				double[] profDiffArr = new double[profDiff.size()];
				for(int i = 0; i < profDiff.size(); i++) {
					profDiffArr[i] = (Double)profDiff.get(i);
				}
				//            System.out.println(agentToReplace + ", " + multiDay + ", " + profDiffArr[0] + ", " + profDiffArr[1] + ", " + (profDiffArr[1] - profDiffArr[0]));
				totalProfitdiff += (profDiffArr[1] - profDiffArr[0]);
				totalProfit+=profDiffArr[1];
				AgentComparator.profit = profDiffArr[1];
				System.out.println("Profit: "+AgentComparator.profit+" difArr: "+profDiffArr[1]);
				System.out.println("TARGET ARRAY" +Arrays.toString((int[])agent.targetArray));
				
			}
			
			long gameEnd = System.currentTimeMillis();
			System.out.println("Game Time: "+(gameEnd-gameStart)/1000.000);
		}
		//FIXME: put in file so can compare
		System.out.println("Profit: "+totalProfit+"Profit Diff"+ totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3 + ", " + budgetL + ", " + budgetM + ", " + budgetH + ", " + bidMultLow + ", " + bidMultHigh);
		BufferedWriter bwriter = null;
				try {
					bwriter= new BufferedWriter(new FileWriter(new File("/home/betsy/git/Lezam-1/bidding/Profit_"+lowS+"_"+highS+".txt")));
					bwriter.write("Profit: "+totalProfit+"Profit Diff"+ totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3 + ", " + budgetL + ", " + budgetM + ", " + budgetH + ", " + bidMultLow + ", " + bidMultHigh);
					bwriter.flush();
					bwriter.close();
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	
	public static void simulateAgent(int daysToLook, long timeConstraint, int lowS, int highS, ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh,
			MCKP.MultiDay multiDay, int multiDayDiscretization, boolean PERFECT_SIM) {
		String[] agentsToReplace;
		
		if(agentNum == 0) {
			agentsToReplace = new String[] {"TacTex"};
		}
		else if(agentNum == 1) {
			agentsToReplace = new String[] {"Schlemazl"};
		}
		else {
			agentsToReplace = new String[] {"Schlemazl", "TacTex"};
		}
		//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor"};
		//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor" , "MetroClick", "tau", "Nanda_AA", "crocodileagent", "McCon"};

		
		
		double totalProfitdiff = 0.0;
		double totalProfit = 0.0;
		for(String filename : filenames) {
			long gameStart = System.currentTimeMillis();
			PersistentHashMap cljSim = setupSimulator(filename);
			for(String agentToReplace : agentsToReplace) {

				if(gameSet.equals(GameSet.semi2011server2) && agentToReplace.equals("TacTex")) {
					gameString = filename.substring(filename.length()-7);
					if(gameString.equals("621.slg") ||
							gameString.equals("622.slg") ||
							gameString.equals("623.slg") ||
							gameString.equals("624.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("TacTex")) {
					gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1426.slg") ||
							gameString.equals("1427.slg") ||
							gameString.equals("1428.slg") ||
							gameString.equals("1429.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("Schlemazl")) {
					gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1434.slg") ||
							gameString.equals("1435.slg") ||
							gameString.equals("1436.slg") ||
							gameString.equals("1437.slg")) {
						continue;
					}
				}
				//for testing purposes
				timeFile = Long.toString(System.currentTimeMillis());
				Boolean fileDir = (new File(System.getProperty("user.dir")+System.getProperty("file.separator")+"Details").mkdirs());
				if(!fileDir){
					System.out.println("Error: Directory not created");
				}
				AbstractAgent agent;
				if(PERFECT_SIM) {
					debug("here");
					agent = makeAgent(daysToLook, timeConstraint,cljSim, agentToReplace,c1,c2,c3,multiDay, multiDayDiscretization, filename);
				}
				else {
					
					agent = makeAgent(daysToLook, lowS, highS, c1,c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh,multiDay,multiDayDiscretization);
					//               agent = new EquatePPSSimple2010();
				}
				if(agent==null){
					debug("agentNull");
				}
				PersistentArrayMap results = javasim.simulateAgent(cljSim, agent, agentToReplace);
				//tester.closeFile();
				PersistentArrayMap actual = javasim.getActualResults(cljSim);
				            System.out.println("Results of " + filename + " Sim: \n"); 
				            javasim.printResults(results);
				            System.out.println("Results of " + filename + " Actual: \n");
				            javasim.printResults(actual);
				ArrayList profDiff = javasim.compareResults(results,actual,agentToReplace);
				double[] profDiffArr = new double[profDiff.size()];
				for(int i = 0; i < profDiff.size(); i++) {
					profDiffArr[i] = (Double)profDiff.get(i);
				}
				//            System.out.println(agentToReplace + ", " + multiDay + ", " + profDiffArr[0] + ", " + profDiffArr[1] + ", " + (profDiffArr[1] - profDiffArr[0]));
				totalProfitdiff += (profDiffArr[1] - profDiffArr[0]);
				totalProfit+=profDiffArr[1];
				AgentComparator.profit = profDiffArr[1];
				System.out.println("Profit: "+AgentComparator.profit+" difArr: "+profDiffArr[1]);
				System.out.println("TARGET ARRAY" +Arrays.toString((int[])agent.targetArray));
				
			}
			
			long gameEnd = System.currentTimeMillis();
			System.out.println("Game Time: "+(gameEnd-gameStart)/1000.000);
		}
		//FIXME: put in file so can compare
		System.out.println("Profit: "+totalProfit+"Profit Diff"+ totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3 + ", " + budgetL + ", " + budgetM + ", " + budgetH + ", " + bidMultLow + ", " + bidMultHigh);
		BufferedWriter bwriter = null;
				try {
					bwriter= new BufferedWriter(new FileWriter(new File("/home/betsy/git/Lezam-1/bidding/Profit_"+lowS+"_"+highS+".txt")));
					bwriter.write("Profit: "+totalProfit+"Profit Diff"+ totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3 + ", " + budgetL + ", " + budgetM + ", " + budgetH + ", " + bidMultLow + ", " + bidMultHigh);
					bwriter.flush();
					bwriter.close();
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	
	
	
	private static AbstractAgent makeAgent(int daysToLook, int lowSlot, int highSlot, double c1, double c2, double c3,
			double budgetL, double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		
		if(multiDay.equals(MultiDay.HillClimbing)){
			System.out.println("creating J");
			return new JordanHillClimbing(daysToLook, c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.MultiDayOptimizer)){
			return new MultiDayOptimizer(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DP)){
			return new DP(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DPHill)){
			return new DPHill(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.OneDayHeuristic)){
			return new OneDayHeuristic(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.PMCKP)){
			return new FastPMCKP(null, null);
		}else if(multiDay.equals(MultiDay.MDPMCKP)){
			return new FastMDPMCKP(2, 200000, null, null);
		}else{
			return null;
		}
	}

	private static AbstractAgent makeAgent(int daysToLook, long timeConstraint, PersistentHashMap cljSim,
			String agentToReplace, double c1, double c2, double c3,
			MultiDay multiDay, int multiDayDiscretization, String filename) {
		if(multiDay.equals(MultiDay.HillClimbing)){
			System.out.println("creating J 1");
			System.out.println("DTL HERE IS: "+daysToLook);
			return new JordanHillClimbing(daysToLook, timeConstraint, cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization, filename);
		}else if(multiDay.equals(MultiDay.MultiDayOptimizer)){
			return new MultiDayOptimizer(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization, filename);
		}else if(multiDay.equals(MultiDay.DP)){
			return new DP(daysToLook, cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DPHill)){
			return new DPHill(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.OneDayHeuristic)){
			return new OneDayHeuristic(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.PMCKP)){
			return new FastPMCKP(cljSim, agentToReplace);
		}else if(multiDay.equals(MultiDay.MDPMCKP)){
			return new FastMDPMCKP(daysToLook, timeConstraint, cljSim, agentToReplace);
		}else{
			return null;
		}
	}

	
	private static AbstractAgent makeAgent(int lowSlot, int highSlot, double c1, double c2, double c3,
			double budgetL, double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization) {
		
		if(multiDay.equals(MultiDay.HillClimbing)){
			return new JordanHillClimbing(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.MultiDayOptimizer)){
			return new MultiDayOptimizer(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DP)){
			return new DP(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DPHill)){
			return new DPHill(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.OneDayHeuristic)){
			return new OneDayHeuristic(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.PMCKP)){
			return new FastPMCKP(null, null);
		}else if(multiDay.equals(MultiDay.MDPMCKP)){
			return new FastMDPMCKP(2, 200000, null, null);
			
		}else{
			return null;
		}
	}

	private static AbstractAgent makeAgent(PersistentHashMap cljSim,
			String agentToReplace, double c1, double c2, double c3,
			MultiDay multiDay, int multiDayDiscretization, String filename) {
		if(multiDay.equals(MultiDay.HillClimbing)){
			return new JordanHillClimbing(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization, filename);
		}else if(multiDay.equals(MultiDay.MultiDayOptimizer)){
			return new MultiDayOptimizer(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization, filename);
		}else if(multiDay.equals(MultiDay.DP)){
			return new DP(58, cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DPHill)){
			return new DPHill(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.OneDayHeuristic)){
			return new OneDayHeuristic(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.PMCKP)){
			return new FastPMCKP(cljSim, agentToReplace);
		}else if(multiDay.equals(MultiDay.MDPMCKP)){
			return new FastMDPMCKP(2, 200000, cljSim, agentToReplace);
		}else{
			return null;
		}
	}
	
	private static AbstractAgent makeAgent(int lowSlot, int highSlot, double c1, double c2, double c3,
			double budgetL, double budgetM, double budgetH, double bidMultLow,
			double bidMultHigh, MultiDay multiDay, int multiDayDiscretization, HashMap<String,String> params) {
		
		if(multiDay.equals(MultiDay.HillClimbing)){
			return new JordanHillClimbing(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization, 
					Integer.parseInt(params.get("accountForProbing")), 
					params.get("changeWandV").compareToIgnoreCase("true")==0, 
					params.get("adjustBudget").compareToIgnoreCase("true")==0,
					params.get("goOver").compareToIgnoreCase("true")==0);
			//accountForProbing, boolean changeWandV, boolean adjustBudget
		}else if(multiDay.equals(MultiDay.MultiDayOptimizer)){
			return new MultiDayOptimizer(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization,
					Integer.parseInt(params.get("numTake")), 
					params.get("reCalc").compareToIgnoreCase("true")==0, 
					params.get("reCalcWithExtra").compareToIgnoreCase("true")==0,
					params.get("changeWandV").compareToIgnoreCase("true")==0,
					params.get("adjustBudget").compareToIgnoreCase("true")==0, 
					params.get("goOver").compareToIgnoreCase("true")==0, 
					Integer.parseInt(params.get("accountForProbing")));
		}else if(multiDay.equals(MultiDay.DP)){
			return new DP(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DPHill)){
			return new DPHill(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.OneDayHeuristic)){
			return new OneDayHeuristic(c1, c2, c3,
					budgetL, budgetM,  budgetH,  bidMultLow,
					bidMultHigh, multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.PMCKP)){
			return new FastPMCKP(null, null);
		}else if(multiDay.equals(MultiDay.MDPMCKP)){
			return new FastMDPMCKP(2, 200000, null, null);
		}else{
			return null;
		}
	}

	private static AbstractAgent makeAgent(PersistentHashMap cljSim,
			String agentToReplace, double c1, double c2, double c3,
			MultiDay multiDay, int multiDayDiscretization, HashMap<String,String> params) {
		if(multiDay.equals(MultiDay.HillClimbing)){
			return new JordanHillClimbing(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization, 
					Integer.parseInt(params.get("accountForProbing")), 
					params.get("changeWandV").compareToIgnoreCase("true")==0, 
					params.get("adjustBudget").compareToIgnoreCase("true")==0, 
					params.get("goOver").compareToIgnoreCase("true")==0);
		}else if(multiDay.equals(MultiDay.MultiDayOptimizer)){
			return new MultiDayOptimizer(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization,
					Integer.parseInt(params.get("numToTake")), 
					params.get("reCalc").compareToIgnoreCase("true")==0, 
					params.get("reCalcWithExtra").compareToIgnoreCase("true")==0,
					params.get("changeWandV").compareToIgnoreCase("true")==0,
					params.get("adjustBudget").compareToIgnoreCase("true")==0,
					params.get("goOver").compareToIgnoreCase("true")==0);
		}else if(multiDay.equals(MultiDay.DP)){
			return new DP(58, cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.DPHill)){
			return new DPHill(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else if(multiDay.equals(MultiDay.OneDayHeuristic)){
			return new OneDayHeuristic(cljSim,
					agentToReplace, c1, c2, c3,
					multiDay, multiDayDiscretization);
		}else{
			return null;
		}
	}
	
	
	private static double randDouble(Random R, double a, double b) {
		double rand = R.nextDouble();
		return rand * (b - a) + a;
	}


	public static boolean isMac(){
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf( "mac" ) >= 0); //Mac
	}


	public static void main(String[] args) throws IOException {

//		for (int highS = 1;highS<=5;highS++){
//			for(int lowS = 5;lowS>=highS;lowS--){
		//----------------------------------------------------------
		//  Agent configuration parameters
		//----------------------------------------------------------
		double c1 = 1;
		double c2 = 0;
		double c3 = 0;
		double simUB = 1.45;
		
		

		//For determining which of our agent configurations we should run.
		int ourAgentMethod = 4;
		int multiDayDiscretization = 10;
		//----------------------------------------------------------
		//  Simulator configuration parameters
		//----------------------------------------------------------
		GameSet gameSet = GameSet.semi2011server1; //GameSet.qual2012; //GameSet.semi2011server1; //GameSet.semi2011server2;
		int gameNumStart = 1419; //944; //1414; //621;
		int gameNumEnd = 1425; //944; //1414; //621;
		int agentNum = 1;


		//Perfect models?
		boolean PERFECT_SIM = false;
		
		boolean TEST_QUEUES = true;





		//----------------------------------------------------------
		//  Handling command-line arguments
		//----------------------------------------------------------
		if(args.length == 5) {
			gameNumStart = Integer.parseInt(args[0]);
			gameNumEnd = gameNumStart;
			agentNum = Integer.parseInt(args[1]);
			ourAgentMethod = Integer.parseInt(args[2]);
			multiDayDiscretization = Integer.parseInt(args[3]);
			PERFECT_SIM = Boolean.parseBoolean(args[4]);
		}
		if(args.length == 4) {
			gameNumStart = Integer.parseInt(args[0]);
			gameNumEnd = gameNumStart;
			c1 = Double.parseDouble(args[1]);
			c2 = Double.parseDouble(args[2]);
			c3 = Double.parseDouble(args[3]);
		}





		MCKP.MultiDay multiDay = null;
		if (ourAgentMethod==0) multiDay = MCKP.MultiDay.OneDayHeuristic;
		else if (ourAgentMethod==1) multiDay = MCKP.MultiDay.HillClimbing;
		else if (ourAgentMethod==2) multiDay = MCKP.MultiDay.DP;
		else if (ourAgentMethod==3) multiDay = MCKP.MultiDay.DPHill;
		else if (ourAgentMethod==4) multiDay = MCKP.MultiDay.MultiDayOptimizer;

		
		
		int highS = 5;
		int lowS = 5; //low >=high
		
		
		//----------------------------------------------------------
		//  Setup and run simulator
		//----------------------------------------------------------
		ArrayList<String> filenames = getGameStrings(gameSet, gameNumStart, gameNumEnd);
		System.out.println("Running games " + gameNumStart + "-" + gameNumEnd + " for opponent " + agentNum + " with agent " + multiDay + ", discretization=" + multiDayDiscretization + ", perfect=" + PERFECT_SIM);
		long start = System.currentTimeMillis();
		simulateAgent(lowS, highS, filenames,gameSet,agentNum,c1,c2,c3,simUB,simUB,simUB,simUB,simUB, multiDay, multiDayDiscretization, PERFECT_SIM);
		long end = System.currentTimeMillis();
		System.out.println("Total seconds elapsed: " + (end-start)/1000.0 );
	//}
		//}
}

	public static void simulateAgent(ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh,
			MCKP.MultiDay multiDay, int multiDayDiscretization, boolean PERFECT_SIM, HashMap<String, String> parameters) {
		
		String[] agentsToReplace;
		if(agentNum == 0) {
			agentsToReplace = new String[] {"TacTex"};
		}
		else if(agentNum == 1) {
			agentsToReplace = new String[] {"Schlemazl"};
		}
		else {
			agentsToReplace = new String[] {"Schlemazl", "TacTex"};
		}
		//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor"};
		//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor" , "MetroClick", "tau", "Nanda_AA", "crocodileagent", "McCon"};

		
		
		double totalProfitdiff = 0.0;
		for(String filename : filenames) {
			long gameStart = System.currentTimeMillis();
			PersistentHashMap cljSim = setupSimulator(filename);
			for(String agentToReplace : agentsToReplace) {

				if(gameSet.equals(GameSet.semi2011server2) && agentToReplace.equals("TacTex")) {
					gameString = filename.substring(filename.length()-7);
					if(gameString.equals("621.slg") ||
							gameString.equals("622.slg") ||
							gameString.equals("623.slg") ||
							gameString.equals("624.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("TacTex")) {
					gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1426.slg") ||
							gameString.equals("1427.slg") ||
							gameString.equals("1428.slg") ||
							gameString.equals("1429.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("Schlemazl")) {
					gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1434.slg") ||
							gameString.equals("1435.slg") ||
							gameString.equals("1436.slg") ||
							gameString.equals("1437.slg")) {
						continue;
					}
				}
				//for testing purposes
				timeFile = Long.toString(System.currentTimeMillis());
				Boolean fileDir = (new File(System.getProperty("user.dir")+System.getProperty("file.separator")+AgentSimulator.timeFile)).mkdirs();
				if(!fileDir){
					System.out.println("Error: Directory not created");
				}
				AbstractAgent agent;
				if(PERFECT_SIM) {
					agent = makeAgent(cljSim, agentToReplace,c1,c2,c3,multiDay, multiDayDiscretization, parameters);
				}
				else {
					agent = makeAgent(-1,-1, c1,c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh,multiDay,multiDayDiscretization, parameters);
					//               agent = new EquatePPSSimple2010();
				}
				
				PersistentArrayMap results = javasim.simulateAgent(cljSim, agent, agentToReplace);
				//tester.closeFile();
				PersistentArrayMap actual = javasim.getActualResults(cljSim);
				            System.out.println("Results of " + filename + " Sim: \n"); 
				            javasim.printResults(results);
				            System.out.println("Results of " + filename + " Actual: \n");
				            javasim.printResults(actual);
				ArrayList profDiff = javasim.compareResults(results,actual,agentToReplace);
				double[] profDiffArr = new double[profDiff.size()];
				for(int i = 0; i < profDiff.size(); i++) {
					profDiffArr[i] = (Double)profDiff.get(i);
				}
				//            System.out.println(agentToReplace + ", " + multiDay + ", " + profDiffArr[0] + ", " + profDiffArr[1] + ", " + (profDiffArr[1] - profDiffArr[0]));
				totalProfitdiff += (profDiffArr[1] - profDiffArr[0]);
				MultipleAgentSimulator.profit = profDiffArr[1];
				System.out.println("Profit: "+MultipleAgentSimulator.profit+" difArr: "+profDiffArr[1]);
				System.out.println("TARGET ARRAY" +Arrays.toString((int[])agent.targetArray));
				
				
			}
			
			long gameEnd = System.currentTimeMillis();
			System.out.println("Game Time: "+(gameEnd-gameStart)/1000.000);
		}
		//FIXME: put in file so can compare
		System.out.println(totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3 + ", " + budgetL + ", " + budgetM + ", " + budgetH + ", " + bidMultLow + ", " + bidMultHigh);
		
	}

	private static void debug(String output){
		System.out.println(output);
	}
	
	
}