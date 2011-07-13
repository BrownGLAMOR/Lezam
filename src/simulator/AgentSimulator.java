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
      finals2010, semifinals2010, test2010
   }

   public static ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) baseFile = "./game";
      if (GAMES_TO_TEST == GameSet.finals2010) {
    	  if (isMac()) {
    		  String homeDir = System.getProperty("user.home");
    		  if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-"; 
    		  if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
    	  }
    	  else baseFile = "/pro/aa/finals2010/game-tacaa1-";    
      }

      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = gameStart; i <= gameEnd; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public static PersistentHashMap setupSimulator(String filename) {
      return javasim.setupClojureSim(filename);
   }

   
   public static void simulateAgent(ArrayList<String> filenames, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh) {
	   MCKP.MultiDay multiDay = MCKP.MultiDay.HillClimbing;
	   int multiDayDiscretization = 50;
	   boolean PERFECT_SIM = true;
	   simulateAgent(filenames, agentNum, c1, c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh, multiDay, multiDayDiscretization, PERFECT_SIM);
   }
   
   public static void simulateAgent(ArrayList<String> filenames, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh, 
		   MCKP.MultiDay multiDay, int multiDayDiscretization, boolean PERFECT_SIM) {
      String[] agentsToReplace;
      if(agentNum == 0) {
         agentsToReplace = new String[] {"TacTex"};
      }
      else if(agentNum == 1) {
         agentsToReplace = new String[] {"Schlemazl"};
      }
      else if(agentNum == 2) {
         agentsToReplace = new String[] {"Mertacor"};
      }
      else if(agentNum == 3) {
         agentsToReplace = new String[] {"MetroClick"};
      }
      else if(agentNum == 4) {
         agentsToReplace = new String[] {"tau"};
      }
      else if(agentNum == 5) {
         agentsToReplace = new String[] {"Nanda_AA"};
      }
      else if(agentNum == 6) {
         agentsToReplace = new String[] {"crocodileagent"};
      }
      else {
         agentsToReplace = new String[] {"McCon"};
      }
//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor"};
//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor" , "MetroClick", "tau", "Nanda_AA", "crocodileagent", "McCon"};
      double totalProfitdiff = 0.0;
      for(String filename : filenames) {
         PersistentHashMap cljSim = setupSimulator(filename);
         for(String agentToReplace : agentsToReplace) {

            AbstractAgent agent;
            if(PERFECT_SIM) {
               agent = new MCKP(cljSim, agentToReplace,c1,c2,c3,multiDay, multiDayDiscretization);
            }
            else {
               agent = new MCKP(c1,c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh,multiDay,multiDayDiscretization);
//               agent = new EquatePPSSimple2010();
            }
            PersistentArrayMap results = javasim.simulateAgent(cljSim, agent, agentToReplace);
            PersistentArrayMap actual = javasim.getActualResults(cljSim);
//            System.out.println("Results of " + filename + " Sim: \n");
//            javasim.printResults(results);
//            System.out.println("Results of " + filename + " Actual: \n");
//            javasim.printResults(actual);
            ArrayList profDiff = javasim.compareResults(results,actual,agentToReplace);
            double[] profDiffArr = new double[profDiff.size()];
            for(int i = 0; i < profDiff.size(); i++) {
               profDiffArr[i] = (Double)profDiff.get(i);
            }
            System.out.println(agentToReplace + ", " + multiDay + ", " + profDiffArr[0] + ", " + profDiffArr[1] + ", " + (profDiffArr[1] - profDiffArr[0]));
            totalProfitdiff += (profDiffArr[1] - profDiffArr[0]);
         }
      }
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
   
   
   public static void main(String[] args) {
	   
      Random rand = new Random();
      double c1,c2,c3;
      c1 = .8 + rand.nextDouble() * .4;
      c2 = .8 + rand.nextDouble() * .4;
      c3 = .8 + rand.nextDouble() * .4;
      double simUB = 1.45;
      int gameNumStart = 15159;
      int gameNumEnd = 15159;
      int agentNum = 0;
      
      //For determining which of our agent configurations we should run.
      int ourAgentMethod = 1;
      int multiDayDiscretization = 10;
      
      //Perfect models?
      boolean PERFECT_SIM = false;
      
      if(args.length == 2) {
         gameNumStart = Integer.parseInt(args[0]);
         gameNumEnd = gameNumStart;
         agentNum = Integer.parseInt(args[1]);
      }
      if(args.length == 5) {
          gameNumStart = Integer.parseInt(args[0]);
          gameNumEnd = gameNumStart;
          agentNum = Integer.parseInt(args[1]);
          ourAgentMethod = Integer.parseInt(args[2]);
          multiDayDiscretization = Integer.parseInt(args[3]);
          PERFECT_SIM = Boolean.parseBoolean(args[4]);
       }

            
      MCKP.MultiDay multiDay = null;
      if (ourAgentMethod==0) multiDay = MCKP.MultiDay.OneDayHeuristic;
      else if (ourAgentMethod==1) multiDay = MCKP.MultiDay.HillClimbing;
      else if (ourAgentMethod==2) multiDay = MCKP.MultiDay.DP;
      else if (ourAgentMethod==3) multiDay = MCKP.MultiDay.DPHill;

      
      ArrayList<String> filenames = getGameStrings(GameSet.finals2010, gameNumStart, gameNumEnd);
      System.out.println("Running games " + gameNumStart + "-" + gameNumEnd + " for opponent " + agentNum + " with agent " + multiDay + ", discretization=" + multiDayDiscretization + ", perfect=" + PERFECT_SIM);
      long start = System.currentTimeMillis();
      simulateAgent(filenames,agentNum,c1,c2,c3,simUB,simUB,simUB,simUB,simUB, multiDay, multiDayDiscretization, PERFECT_SIM);
      long end = System.currentTimeMillis();
      System.out.println("Total seconds elapsed: " + (end-start)/1000.0 );
   }

}