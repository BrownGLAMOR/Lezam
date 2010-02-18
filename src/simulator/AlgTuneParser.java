package simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

import agents.AbstractAgent;
import agents.rulebased.AdjustPM;
import agents.rulebased.AdjustPPS;
import agents.rulebased.AdjustPR;
import agents.rulebased.AdjustROI;
import agents.rulebased.EquatePM;
import agents.rulebased.EquatePPS;
import agents.rulebased.EquatePR;
import agents.rulebased.EquateROI;

public class AlgTuneParser {

	public static void main(String[] args) throws IOException, ParseException {
		runAllParams(args);
	}

	private static void runAllParams(String[] args) throws IOException, ParseException {
		int numPastGenerations = 10;
		ArrayList<String> paramStrings = new ArrayList<String>();
		String firstFilename = null;
		for(String filename : args) {
			if(filename.equals(args[0])) {
				firstFilename = filename;
			}
			ArrayList<String> params = generatePotentialParamsList(filename);
			if(params.size() < numPastGenerations) {
				if(params.size() != 0) {
					for(String paramString : params) {
						paramStrings.add(paramString);
					}
				}
			}
			else {
				for(int i = 0; i < numPastGenerations; i++) {
					paramStrings.add(params.get(params.size()-1-i));
				}
			}
		}
		if(paramStrings.size() > 0) {
			AbstractAgent agent = null;
			if("adjustPM".equals(firstFilename.substring(0,8))) {
				agent = new AdjustPM(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("adjustPR".equals(firstFilename.substring(0,8))) {
				agent = new AdjustPR(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("adjustPP".equals(firstFilename.substring(0,8))) {
				agent = new AdjustPPS(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("adjustRO".equals(firstFilename.substring(0,8))) {
				agent = new AdjustROI(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("equatePM".equals(firstFilename.substring(0,8))) {
				agent = new EquatePM(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("equatePR".equals(firstFilename.substring(0,8))) {
				agent = new EquatePR(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("equatePP".equals(firstFilename.substring(0,8))) {
				agent = new EquatePPS(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			else if("equateRO".equals(firstFilename.substring(0,8))) {
				agent = new EquateROI(numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations, numPastGenerations);
			}
			System.out.println("Running the following parameters for " + agent + ":");
			for(String str : paramStrings) {
				System.out.println(str);
			}

			Collections.shuffle(paramStrings);
			
			HashMap<String,Double> noDupesParams = new HashMap<String, Double>();
			for(String params : paramStrings) {
				noDupesParams.put(params, 0.0);
			}
			BasicSimulator sim = new BasicSimulator();
			HashMap<String,Double> paramVals = new HashMap<String, Double>();
			for(String params : noDupesParams.keySet()) {
				Class<? extends AbstractAgent> c = agent.getClass();
				Constructor[] constr = c.getConstructors();
				
				ArrayList<Double> paramsArrList = new ArrayList<Double>();
				StringTokenizer st = new StringTokenizer(params," ");
				while(st.hasMoreTokens()) {
					paramsArrList.add(Double.parseDouble(st.nextToken()));
				}
				Object[] agentArgs = new Object[paramsArrList.size()];
				for(int i = 0; i < agentArgs.length; i++) {
					agentArgs[i] = paramsArrList.get(i);
				}

				AbstractAgent agentCopy = null;

				try {
					agentCopy = (AbstractAgent)(constr[0].newInstance(agentArgs));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				
				double val = sim.runSimulations("/Users/jordanberg/Desktop/finalsgames/server1/game",1425,1465,0,0, agentCopy);
				paramVals.put(params,val);
				paramVals = sortHashMap(paramVals);
				System.out.println("Best Params:");
				for(String paramStr : paramVals.keySet()) {
					System.out.println(paramVals.get(paramStr) + ": " + paramStr);
				}
				
				
				/*
				 * Print out vals of all solutions in order of value
				 */
				
				
				
				
				/*
				 * ENSURE GARBAGE COLLECTOR IS RUN BETWEEN ITERATIONS
				 */
				System.gc(); sim.emptyFunction(); sim.emptyFunction(); sim.emptyFunction(); sim.emptyFunction();
			}
		}
		else {
			System.out.println("Try parsing some files that actually have solutions!");
		}
	}

	private static ArrayList<String> generatePotentialParamsList(String filename) throws IOException {
		System.out.println(filename);
		ArrayList<String> paramStrings = new ArrayList<String>();
		BufferedReader input =  new BufferedReader(new FileReader(filename));

		/*
		 * Skip the file header
		 */
		for(int i = 0; i < 18; i++) {
			input.readLine();
		}

		String line;
		while ((line = input.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line,"]");
			if(!st.hasMoreElements()) {
				continue;
			}
			st.nextToken();
			if(!st.hasMoreElements()) {
				continue;
			}
			st.nextToken();
			if(!st.hasMoreElements()) {
				continue;
			}
			String token = st.nextToken();
			if(token.length() >= 11) {
				if(" Generation".equals(token.substring(0, 11))) {
					line = input.readLine();
					line = input.readLine();
					line = input.readLine(); // skip three lines
					if(line != null) {
						StringTokenizer st2 = new StringTokenizer(line,"]");
						st2.nextToken();
						st2.nextToken();
						String token2 = st2.nextToken().substring(3);
						paramStrings.add(token2);
						input.readLine(); //skip next line
					}
				}
			}
		}
		return paramStrings;
	}
	
	private static HashMap<String, Double> sortHashMap(HashMap<String, Double> input){
	    Map<String, Double> tempMap = new HashMap<String, Double>();
	    for (String wsState : input.keySet()){
	        tempMap.put(wsState,input.get(wsState));
	    }

	    List<String> mapKeys = new ArrayList<String>(tempMap.keySet());
	    List<Double> mapValues = new ArrayList<Double>(tempMap.values());
	    HashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>();
	    TreeSet<Double> sortedSet = new TreeSet<Double>(mapValues);
	    Object[] sortedArray = sortedSet.toArray();
	    int size = sortedArray.length;
	    for (int i=size-1; i >= 0; i--){
	        sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), 
	                      (Double)sortedArray[i]);
	    }
	    return sortedMap;
	}

}
