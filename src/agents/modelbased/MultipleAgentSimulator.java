

package agents.modelbased;

import java.io.*;
import java.util.*;

import agents.modelbased.MCKP.MultiDay;

import simulator.AgentSimulator;
import simulator.AgentSimulator.GameSet;


public class MultipleAgentSimulator {

	//Arguments and necessary variables	

	public static double profit;


	public static void main(String[] args) {
		//Set up output files
		//TODO: Read parameters in
		//String name = args[0];
		int numAgents=1;
		String name = "test1_9_25_12_";
		Properties prop = new Properties();
		List<HashMap<String, String>> parameters = new ArrayList<HashMap<String,String>>();
		MultiDay[] agentTypes = new MultiDay[numAgents];;
		try {
			prop.load(new FileInputStream(System.getProperty("user.dir")+"/src/agents/modelbased/test_new.properties"));
			numAgents = Integer.parseInt(prop.getProperty("numAgents"));
			agentTypes = new MultiDay[numAgents];
			for (int p = 0; p<numAgents;p++){
				String aName = "Agent"+p;
				String params = (String) prop.get(aName);
				System.out.println(params);
				parameters.add(p, setAgentParams(params));
				agentTypes[p]= MultiDay.valueOf(parameters.get(p).get("MultiDay"));
			}


		} catch (IOException ex) {
			ex.printStackTrace();
		}



		int gamesToRun = Integer.parseInt(prop.getProperty("gamesToRun"));
		System.out.println("GamesTo Run "+gamesToRun);

		String games = prop.getProperty("gameFolderNames");
		String[] gamesSplit = games.split(",");
		ArrayList<String> gameFolders = new ArrayList<String>();
		for (int g = 0; g<gamesSplit.length; g++){
			gameFolders.add(gamesSplit[g]);
		}

		int[] agentProfits = new int[numAgents];
		Arrays.fill(agentProfits, 0);
		int[] agentTimes = new int[numAgents];
		Arrays.fill(agentTimes, 0);

		String filename = System.getProperty("user.dir")+System.getProperty("file.separator")+"MultAgentResults"+System.getProperty("file.separator")+name+"_MultiAgentSim.csv";
		StringWriter stBuff = new StringWriter();
		BufferedWriter buffWrit = null;


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
		//MCKP.MultiDay agent2 = MCKP.MultiDay.HillClimbing;

		long timeStart;
		long timeEnd;

		ArrayList<String> filenames;
		int gamesRun = 0;
		while (gamesRun<gamesToRun){
			if(gameFolders.contains("finals2010")){
				//for (int i = 1297; i<=1320; i++){
				//for (int i = 15145; i<=15160; i++){
				for (int i = 15145; i<=15160; i++){
					for(int j = 0; j<numAgents; j++){
						if(gamesRun<gamesToRun){
							stBuff = new StringWriter();
							filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.finals2010, i, i);

							timeStart = System.currentTimeMillis();
							AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.finals2010, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, MultiDay.valueOf(parameters.get(j).get("MultiDay")), multiDayDiscretization, PERFECT_SIM, parameters.get(j));
							timeEnd = System.currentTimeMillis();
							try{
								stBuff.append("finals2010"+",");
								stBuff.append(i+",");
								stBuff.append( parameters.get(j).get("MultiDay")+"_"+j+",");
								stBuff.append((timeEnd-timeStart)/1000+",");
								stBuff.append(profit+"\n");
							} catch (Exception e){
								e.printStackTrace();
							}
							//System.out.println("Profit:"+profit);
							agentProfits[j] += profit;
							agentTimes[j] += (timeEnd-timeStart)/1000;
							gamesRun++;

							if (buffWrit != null){
								try {
									buffWrit.write(stBuff.toString());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}

				}
			}

			if(gameFolders.contains("semi2011server1")){
				//for (int i = 1414; i<=1445; i++){
				for (int i = 1414; i<=1429; i++){
					for(int j = 0; j<numAgents; j++){
						if(gamesRun<gamesToRun){
							stBuff = new StringWriter();
							filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.semi2011server1, i, i);

							timeStart = System.currentTimeMillis();
							AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.semi2011server1, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, MultiDay.valueOf(parameters.get(j).get("MultiDay")), multiDayDiscretization, PERFECT_SIM, parameters.get(j));
							timeEnd = System.currentTimeMillis();
							try{
								stBuff.append("semi2011server1"+",");
								stBuff.append(i+",");
								stBuff.append( parameters.get(j).get("MultiDay")+"_"+j+",");
								stBuff.append((timeEnd-timeStart)/1000+",");
								stBuff.append(profit+"\n");
							} catch (Exception e){
								e.printStackTrace();
							}
							//System.out.println("Profit:"+profit);
							agentProfits[j] += profit;
							agentTimes[j] += (timeEnd-timeStart)/1000;
							gamesRun++;

							if (buffWrit != null){
								try {
									buffWrit.write(stBuff.toString());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}

			if(gameFolders.contains("semi2011server2")){
				//for (int i = 621; i<=640; i++){
				for (int i = 621; i<=636; i++){	
					for(int j = 0; j<numAgents; j++){
						if(gamesRun<gamesToRun){
							stBuff = new StringWriter();
							filenames = AgentSimulator.getGameStrings(AgentSimulator.GameSet.semi2011server2, i, i);

							timeStart = System.currentTimeMillis();
							AgentSimulator.simulateAgent(filenames, AgentSimulator.GameSet.semi2011server2, 1, c1, c2, c3, simUB, simUB, simUB, simUB, simUB, MultiDay.valueOf(parameters.get(j).get("MultiDay")), multiDayDiscretization, PERFECT_SIM, parameters.get(j));
							timeEnd = System.currentTimeMillis();
							try{
								stBuff.append("semi2011server2"+",");
								stBuff.append(i+",");
								stBuff.append( parameters.get(j).get("MultiDay")+"_"+j+",");
								stBuff.append((timeEnd-timeStart)/1000+",");
								stBuff.append(profit+"\n");
							} catch (Exception e){
								e.printStackTrace();
							}
							//System.out.println("Profit:"+profit);
							agentProfits[j] += profit;
							agentTimes[j] += (timeEnd-timeStart)/1000;
							gamesRun++;

							if (buffWrit != null){
								try {
									buffWrit.write(stBuff.toString());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}


			}
			//Set up simulator
			//May be necessary to do this more than once, or iterate through the set of game sets
			//May even have to do this per game set

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
				for(int ag=0; ag<numAgents; ag++){
					stBuff.append(agentTypes[ag].toString()+"_"+ag+",");
				}
				stBuff.append("\n");
				for(int ag=0; ag<numAgents; ag++){
					stBuff.append(agentProfits[ag]+",");
				}
				stBuff.append("\n");
				for(int ag=0; ag<numAgents; ag++){
					stBuff.append(agentTimes[ag]+",");
				}
				stBuff.append("\n");

				buffWrit.write(stBuff.toString());
				buffWrit.flush();
				buffWrit.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		
	}
	

	private static HashMap<String, String> setAgentParams(String input) {
		HashMap<String, String> params = new HashMap<String, String>();
		String[] vals = input.split(",");
		if(vals[0].compareToIgnoreCase("MultiDayOptimizer")==0){
			params.put("MultiDay", vals[0]);
			params.put("numToTake", vals[1]);
			params.put("reCalc", vals[2]);
			params.put("reCalcWithExtra", vals[3]);
			params.put("changeWandV", vals[4]);
			params.put("adjustBudget", vals[5]);
			params.put("goOver", vals[6]);
			params.put("accountForProbing", vals[7]);


		}
		if(vals[0].compareToIgnoreCase("HillClimbing")==0){
			params.put("MultiDay", vals[0]);
			params.put("accountForProbing", vals[1]);
			params.put("changeWandV", vals[2]);
			params.put("adjustBudget", vals[3]);
			params.put("goOver", vals[4]);

		}
		return params;
	}

}
