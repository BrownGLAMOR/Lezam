package simulator;

import agents.AbstractAgent;
import agents.modelbased.MCKP;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import tacaa.core;

import java.util.ArrayList;

public class AgentSimulator {

   public enum GameSet {
      finals2010, semifinals2010, test2010
   }

   public static ArrayList<String> getGameStrings(GameSet GAMES_TO_TEST, int gameStart, int gameEnd) {
      String baseFile = null;
      if (GAMES_TO_TEST == GameSet.test2010) baseFile = "./game";
      if (GAMES_TO_TEST == GameSet.finals2010) baseFile = "/Users/jordanberg/Desktop/tacaa2010/game-tacaa1-";  //"/pro/aa/finals2010/game-tacaa1-";   //"/Users/sodomka/Desktop/tacaa2010/game-tacaa1-";

      ArrayList<String> filenames = new ArrayList<String>();
      for (int i = gameStart; i <= gameEnd; i++) {
         filenames.add(baseFile + i + ".slg");
      }
      return filenames;
   }

   public static PersistentHashMap setupSimulator(String filename) {
      return core.setupClojureSim(filename);
   }

   public static void simulateAgent(ArrayList<String> filenames) {
      String[] agentsToReplace = new String[] {"tau", "TacTex", "MetroClick", "Schlemazl", "Nanda_AA", "Mertacor" };
      for(String agentToReplace : agentsToReplace) {
         for(String filename : filenames) {
            PersistentHashMap cljSim = setupSimulator(filename);

            boolean PERFECT_SIM = true;
            AbstractAgent agent;
            if(PERFECT_SIM) {
               agent = new MCKP(cljSim, agentToReplace);
            }
            else {
               agent = new MCKP();
            }
            PersistentArrayMap results = core.simulateAgent(cljSim, agent, agentToReplace);
            PersistentArrayMap actual = core.getActualResults(cljSim);
//            System.out.println("Results of " + filename + " Sim: \n");
//            core.printResults(results);
//            System.out.println("Results of " + filename + " Actual: \n");
//            core.printResults(actual);
            core.compareResults(results,actual,agentToReplace);
         }
      }
   }

   public static void main(String[] args) {
      ArrayList<String> filenames = getGameStrings(GameSet.finals2010, 15128, 15128);
      simulateAgent(filenames);
   }

}
