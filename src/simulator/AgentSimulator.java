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
      finals2010, semifinals2010, test2010, semi2011server1, semi2011server2
   }

   public static ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) {
         baseFile = "./game";
      }
      else if (GAMES_TO_TEST == GameSet.finals2010) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2010/game-tacaa1-";
         }
         else baseFile = "/pro/aa/finals2010/game-tacaa1-";
      }
      else if (GAMES_TO_TEST == GameSet.semi2011server1) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2011/semi/server1/game";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2011/semi/server1/game";
         }
         else baseFile = "/pro/aa/tacaa2011/semi/server1/game";
      }
      else if (GAMES_TO_TEST == GameSet.semi2011server2) {
         if (isMac()) {
            String homeDir = System.getProperty("user.home");
            if (homeDir.equals("/Users/sodomka")) baseFile = homeDir + "/Desktop/tacaa2011/semi/server2/game";
            if (homeDir.equals("/Users/jordanberg"))baseFile = homeDir + "/Desktop/tacaa2011/semi/server2/game";
         }
         else baseFile = "/pro/aa/tacaa2011/semi/server2/game";
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

   public static void simulateAgent(ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh) {
      MCKP.MultiDay multiDay = MCKP.MultiDay.HillClimbing;
      int multiDayDiscretization = 50;
      boolean PERFECT_SIM = true;
      simulateAgent(filenames, gameSet, agentNum, c1, c2,c3,budgetL,budgetM,budgetH,bidMultLow,bidMultHigh, multiDay, multiDayDiscretization, PERFECT_SIM);
   }

   public static void simulateAgent(ArrayList<String> filenames, GameSet gameSet, int agentNum, double c1, double c2, double c3, double budgetL, double budgetM, double budgetH, double bidMultLow, double bidMultHigh,
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
      for(String filename : filenames) {
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
//            System.out.println(agentToReplace + ", " + multiDay + ", " + profDiffArr[0] + ", " + profDiffArr[1] + ", " + (profDiffArr[1] - profDiffArr[0]));
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
      c1 = 1;
      c2 = 0;
      c3 = 0;
      double simUB = 1.45;
      GameSet gameSet = GameSet.semi2011server1;
      int gameNumStart = 1414;
      int gameNumEnd = 1414;

//      GameSet gameSet = GameSet.semi2011server2;
//      int gameNumStart = 621;
//      int gameNumEnd = 621;

      int agentNum = 2;

      //For determining which of our agent configurations we should run.
      int ourAgentMethod = 1;
      int multiDayDiscretization = 10;

      //Perfect models?
      boolean PERFECT_SIM = false;

//      if(args.length == 2) {
//         gameNumStart = Integer.parseInt(args[0]);
//         gameNumEnd = gameNumStart;
//         agentNum = Integer.parseInt(args[1]);
//      }
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

      ArrayList<String> filenames = getGameStrings(gameSet, gameNumStart, gameNumEnd);
//      System.out.println("Running games " + gameNumStart + "-" + gameNumEnd + " for opponent " + agentNum + " with agent " + multiDay + ", discretization=" + multiDayDiscretization + ", perfect=" + PERFECT_SIM);
      long start = System.currentTimeMillis();
      simulateAgent(filenames,gameSet,agentNum,c1,c2,c3,simUB,simUB,simUB,simUB,simUB, multiDay, multiDayDiscretization, PERFECT_SIM);
      long end = System.currentTimeMillis();
//      System.out.println("Total seconds elapsed: " + (end-start)/1000.0 );
   }

}