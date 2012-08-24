package simulator;

import agents.AbstractAgent;
import agents.modelbased.MCKP;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import tacaa.javasim;

import java.util.ArrayList;
import java.util.Random;

public class AgentSimulator {

	public enum GameSet {
		finals2010, semifinals2010, test2010, semi2011server1, semi2011server2, qual2012
	}




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

	public static void simulateAgent(ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh) throws IOException {
		MCKP.MultiDay multiDay = MCKP.MultiDay.HillClimbing;
		int multiDayDiscretization = 50;
		boolean PERFECT_SIM = true;
		boolean TEST_QUEUES = false;
		simulateAgent(filenames, gameSet, agentNum, c1, c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh, multiDay, multiDayDiscretization, PERFECT_SIM, TEST_QUEUES);
	}

	public static void simulateAgent(ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh,
			MCKP.MultiDay multiDay, int multiDayDiscretization, boolean PERFECT_SIM, boolean TEST_QUEUES) throws IOException {

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
					String gameString = filename.substring(filename.length()-7);
					if(gameString.equals("621.slg") ||
							gameString.equals("622.slg") ||
							gameString.equals("623.slg") ||
							gameString.equals("624.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("TacTex")) {
					String gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1426.slg") ||
							gameString.equals("1427.slg") ||
							gameString.equals("1428.slg") ||
							gameString.equals("1429.slg")) {
						continue;
					}
				}
				else if(gameSet.equals(GameSet.semi2011server1) && agentToReplace.equals("Schlemazl")) {
					String gameString = filename.substring(filename.length()-8);
					if(gameString.equals("1434.slg") ||
							gameString.equals("1435.slg") ||
							gameString.equals("1436.slg") ||
							gameString.equals("1437.slg")) {
						continue;
					}
				}

				AbstractAgent agent;
				TestAgent tester = new TestAgent(filename);
				if(PERFECT_SIM) {
					agent = new MCKP(cljSim, agentToReplace,c1,c2,c3,multiDay, multiDayDiscretization);
				}
				else {
					if(TEST_QUEUES){
						
					
						System.out.println(tester+"________________________________________");
						agent = new MCKP(c1,c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh,multiDay,multiDayDiscretization, tester);
						
					}else{
						agent = new MCKP(c1,c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh,multiDay,multiDayDiscretization);
					//               agent = new EquatePPSSimple2010();
					}
				}
				PersistentArrayMap results = javasim.simulateAgent(cljSim, agent, agentToReplace);
				tester.closeFile();
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
				
			}
			
			long gameEnd = System.currentTimeMillis();
			System.out.println("Game Time: "+(gameEnd-gameStart)/1000.000);
		}
		//FIXME: put in file so can compare
		System.out.println(totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3 + ", " + budgetL + ", " + budgetM + ", " + budgetH + ", " + bidMultLow + ", " + bidMultHigh);
		
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


		//----------------------------------------------------------
		//  Agent configuration parameters
		//----------------------------------------------------------
		double c1 = 1;
		double c2 = 0;
		double c3 = 0;
		double simUB = 1.45;
		
		

		//For determining which of our agent configurations we should run.
		int ourAgentMethod = 1;
		int multiDayDiscretization = 10;

		
		//----------------------------------------------------------
		//  Simulator configuration parameters
		//----------------------------------------------------------
		GameSet gameSet = GameSet.semi2011server1; //GameSet.qual2012; //GameSet.semi2011server1; //GameSet.semi2011server2;
		int gameNumStart = 1417; //944; //1414; //621;
		int gameNumEnd = 1417; //944; //1414; //621;
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

		
		

		
		
		//----------------------------------------------------------
		//  Setup and run simulator
		//----------------------------------------------------------
		ArrayList<String> filenames = getGameStrings(gameSet, gameNumStart, gameNumEnd);
		System.out.println("Running games " + gameNumStart + "-" + gameNumEnd + " for opponent " + agentNum + " with agent " + multiDay + ", discretization=" + multiDayDiscretization + ", perfect=" + PERFECT_SIM);
		long start = System.currentTimeMillis();
		simulateAgent(filenames,gameSet,agentNum,c1,c2,c3,simUB,simUB,simUB,simUB,simUB, multiDay, multiDayDiscretization, PERFECT_SIM, TEST_QUEUES);
		long end = System.currentTimeMillis();
		System.out.println("Total seconds elapsed: " + (end-start)/1000.0 );
	}

	
	
}