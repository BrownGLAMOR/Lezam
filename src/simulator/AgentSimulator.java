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
      if (GAMES_TO_TEST == GameSet.finals2010) baseFile = "/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-";  //"/pro/aa/finals2010/game-tacaa1-";    //"/Users/sodomka/Desktop/tacaa2010/game-tacaa1-";

      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = gameStart; i <= gameEnd; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public static PersistentHashMap setupSimulator(String filename) {
      return javasim.setupClojureSim(filename);
   }

   public static void simulateAgent(ArrayList<String> filenames, double c1, double c2, double c3) {
      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor"};
//      String[] agentsToReplace = new String[] {"TacTex", "Schlemazl", "Mertacor" , "MetroClick", "tau", "Nanda_AA", "crocodileagent", "McCon"};
      double totalProfitdiff = 0.0;
      for(String filename : filenames) {
         PersistentHashMap cljSim = setupSimulator(filename);
         for(String agentToReplace : agentsToReplace) {

            boolean PERFECT_SIM = false;
            AbstractAgent agent;
            if(PERFECT_SIM) {
               agent = new MCKP(cljSim, agentToReplace,c1,c2,c3);
            }
            else {
               agent = new MCKP(c1,c2,c3);
            }
            PersistentArrayMap results = javasim.simulateAgent(cljSim, agent, agentToReplace);
            PersistentArrayMap actual = javasim.getActualResults(cljSim);
//            System.out.println("Results of " + filename + " Sim: \n");
//            javasim.printResults(results);
//            System.out.println("Results of " + filename + " Actual: \n");
//            javasim.printResults(actual);
            double profDiff = javasim.compareResults(results,actual,agentToReplace);
            System.out.println(agentToReplace + " Profit Diff " + profDiff);
            totalProfitdiff += profDiff;
         }
      }
      System.out.println(totalProfitdiff + ", " + c1 + ", " + c2 + ", " + c3);
   }

   private static double randDouble(Random R, double a, double b) {
      double rand = R.nextDouble();
      return rand * (b - a) + a;
   }

   public static void main(String[] args) {
      Random rand = new Random();
      double c1,c2,c3;
//      c1 = randDouble(rand,.01,.11);
//      c2 = randDouble(rand,.11,.23);
//      c3 = randDouble(rand,.23,.36);
      c1 = .03;
      c2 = .13;
      c3 = .25;
      ArrayList<String> filenames = getGameStrings(GameSet.finals2010, 15127, 15130);
      simulateAgent(filenames,c1,c2,c3);
   }

}
