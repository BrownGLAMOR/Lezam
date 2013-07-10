package models.queryanalyzer;

import ilog.cplex.IloCplex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import java.util.Set;
import fileutils.FileCopier;
import simulator.predictions.CompareSolverResults;
import simulator.predictions.CompareTimeResults;
import simulator.predictions.QAInstanceResults;
import simulator.predictions.ResultValues;


import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.ds.QAInstanceSampled;
import models.queryanalyzer.riep.ConstantImpressionAndRankEstimator;
import models.queryanalyzer.riep.ImpressionAndRankEstimator;
import models.queryanalyzer.riep.LDSImpressionAndRankEstimator;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorSample;
import models.queryanalyzer.riep.iep.cplp.DropoutImpressionEstimatorAll;
import models.queryanalyzer.riep.iep.cplp.DropoutImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.mip.EricImpressionEstimator;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorMaxTotalImpressions;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPExact;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPMinDiffTotalImpressions;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPPriors;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPSampled;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPWeightedMinDiffTotalImp;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMipTotalPriors;
import models.queryanalyzer.riep.iep.mip.WaterfallILP.Objective;
/*
 * QAAlgorithmEvaluator
 * 
 * This class runs a folder of games through user specified Query Analyzer algorithms 
 * and collects and/or sorts statistics on
 * the speed and accuracy of the impressions estimation results.
 * 
 * It can also compare two solvers by their resultant objective values. 
 * 
 * Betsy Hilliard betsy@cs.brown.edu
 */
public class QAAlgorithmEvaluator {

	private static final boolean SAMPLED_AVERAGE_POSITIONS = true; //Are we running the sampled or exact problem?
	private static final boolean ORDERING_KNOWN = false; //Are we running the problem given known rankings?

	//parameters used in the sampled problem
	private int numSamples = 10; 
	private int sampFrac = 100;
	private int fractionalBran = 0;


	//the folder of example game files to run
	File folder;
	File[] filesToTest;

	/*
	 * a map from a game log file to the results generated for that file.
	 * This allows us to compare results for a given file across solvers.
	 * 
	 * A QAInstanceResults stores the input classes as well as all results. 
	 * If results are sorted, the sorted lists are stored in QAInstanceResults as well
	 */
	HashMap<File, QAInstanceResults> results = new HashMap<File, QAInstanceResults>();
	QAEvaluationResultHandler resultHandler;

	//use one instance of a cplex solver
	IloCplex cplex;

	//constructor for the class
	//takes a path to the folder where the files to test are
	public QAAlgorithmEvaluator(String pathToFiles, double[] timeCutoffs){

		//determine if running on a file of games or a text file list of games
		// create an array of Files to test
		File files = new File(pathToFiles);
		if(files.isDirectory()){
			folder = new File(pathToFiles+"/files");
			filesToTest = folder.listFiles();
		}else{
			folder = new File(pathToFiles);
			folder = new File(folder.getParent());
			filesToTest = makeFolderFiles(files);
		}

		try {
			cplex = new IloCplex();
			new File(folder.getParent()+"/results").mkdir(); //makes sure a result file exists

		} catch (Exception e) {
			System.err.println("Error initializing Evaluator");
			e.printStackTrace();
		}
	}
	
	   public enum SolverType {
		      CP, IP, MIP, ERIC_MIP_SAMPLED, ERIC_MIP_MinSlotEric, ERIC_MIP_MinSlotCJC, ERIC_MIP_MinPriors, ERIC_MIP_MinTotal, ERIC_MIP_MaxTotal, LDSMIP, MULTI_MIP, Carleton_LP, CARLETON_SIMPLE_MIP_Exact, Carleton_LP_Exact, CARLETON_SIMPLE_MIP_Sampled,
		      CARLETON_SIMPLE_MIP_Exact_MinTotal, CARLETON_SIMPLE_MIP_Exact_MinDiffTotal, CARLETON_SIMPLE_MIP_Exact_WeightedMinDiffTotal, CARLETON_SIMPLE_MIP_Exact_PRIORS, CARLETON_SIMPLE_MIP_Exact_Total_Priors, 
		      CARLETON_SIMPLE_MIP_Sampled_WeightedMinDiffTotal
		   }

		   public enum GameSet {
		      finals2010, semifinals2010, test2010,semi2011server1,semi2011server2
		   }

		   public enum HistoricalPriorsType {
		      Naive, LastNonZero, SMA, EMA,
		   }


	/*
	 * Takes a path to a text file and reads specific file names
	 * This can be used to run specific instances (such as a test set or
	 * set of hard/failure cases)
	 * 
	 * files is a path to a text file
	 * 
	 * returns an Array of files that initializes the filesToTest variable
	 */
	private File[] makeFolderFiles(File files) {
		File[] fileArray = null;

		//construct a buffered reader, count number of files and create File array
		try {
			BufferedReader reader = new BufferedReader(new FileReader(files));
			int count=0;
			while(reader.readLine()!=null){ count+=1;}
			reader.close();
			//System.out.println("Count: "+count);
			fileArray = new File[count];
			count = 0;

			//reread the file
			reader = new BufferedReader(new FileReader(files));
			String fileName = reader.readLine();
			while(fileName!=null){
				//System.out.println("Count: "+count);
				fileArray[count] = new File(fileName);
				count+=1;
				fileName = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileArray;
	}


	/*
	 * runEvaluations runs the specified set of files through a set of solvers
	 * restricts the time allowed for a solver to run (timeCuttoffs)
	 */
	public void runEvaluations(SolverType[] solvers, boolean rie,
			double[] timeCutoffs){
		resultHandler = new QAEvaluationResultHandler(folder.getParent(), timeCutoffs);

		//initialize strings to hold results (Can I do this more cleanly?)
		String AIPSResults ="";
		String AIResults ="";
		String TIResults ="";
		String TimeResults ="";

		//for each solver, for each time cutoff, attempt to solve all files. collect statistics.
		for(int s=0; s<solvers.length; s++){
			System.out.println("SOLVER: "+solvers[s]);

			AIPSResults +=solvers[s].toString()+",aips";
			AIResults+=solvers[s].toString()+",ai";
			TIResults+=solvers[s].toString()+",ti";
			TimeResults +=solvers[s].toString();

			//for each time cutoff, run all files
			for(int t=0;t<timeCutoffs.length;t++){
				System.out.println("TIMEOUT IN: "+timeCutoffs[t]);

				//create a SolverStats to maintain all stats (Can I store this in "results" somehow?)
				SolverStats stats = new SolverStats();

				//it would be good to be able to set a range of files to run. Maybe not worth the time?
				//for(int f = 1; f<2000; f++){
				 for(int f = 0; f<filesToTest.length; f++){
					System.out.println("File: "+filesToTest[f].getName());

					//get just the file name
					String[] sfileName = filesToTest[f].getAbsolutePath().split("/");
					String last = sfileName[sfileName.length-1];

					//if legit file and not a directory or other junk, run the file
					if(last.compareToIgnoreCase(".svn")!=0 && !last.contains(".csv") && 
							!filesToTest[f].isDirectory()){

						//if new solver, create a new results object, this makes sure we store all files' results 
						// in the same location so they can compared easily
						if(s==0){
							System.out.println("New solver: "+filesToTest[f].getAbsolutePath());
							QAInstanceResults res = new QAInstanceResults(filesToTest[f].getAbsolutePath());
							results.put(filesToTest[f], res);
						}

						System.out.println(results.get(filesToTest[f])+" "+filesToTest[f]);
						//if an instance could be created, solve it and collect statistics.
						if(results.get(filesToTest[f]).getQAInstAll()!=null){

							//solves instance f with solver s, within time t
							ResultValues rieResVals =runInstance(solvers[s], timeCutoffs[t], results.get(filesToTest[f]), rie);
							//System.out.println("SOLVER:" + solvers[s] + ", RESULT: " + rieResVals);

							//if solved, collect stats
							if(rieResVals.getPredictedWaterfall() !=null){
								//System.out.println("RES: "+rieResVals+" results: "+results.get(filesToTest[f]));
								int[][] waterfallP = rieResVals.getPredictedWaterfall();
								int[][] waterfallT = results.get(filesToTest[f]).getQADataAll().greedyAssign(results.get(filesToTest[f]).getQAInstAll().getAgentIds());
								System.out.println("Predicted");
								visualizeWaterfall(waterfallP);
								System.out.println("True");
								visualizeWaterfall(waterfallT);
								stats.updateMAES(results.get(filesToTest[f]).calcAgentImpsPerSlotMAE(solvers[s]),
										results.get(filesToTest[f]).calcAgentImpsMAE(solvers[s]), 
										results.get(filesToTest[f]).calcTotalImpsMAE(solvers[s]),
										results.get(filesToTest[f]).getTime(solvers[s]));
								//if(results.get(filesToTest[f]).calcAgentImpsPerSlotMAE(solvers[s])[0]>0){
								double val = results.get(filesToTest[f]).calcAgentImpsPerSlotMAE(solvers[s])[0];
//									System.out.println("HHHHHHHHHHHHHHHHHHHHHHEREHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH______________________HH: "+
//											val);
//									if(val>0){
//										break;
//									}
									
								//}
							}else{
								System.out.println("_____________________________FAILED TO SOLVE________________________________");
							}
						}else{
							System.out.println("___________________________SKIPPING FILE__________________________________");
						}
					}else{
						//System.out.println("SKIPPING NON-AUCTION FILE");
					}
				}
				//collect all 3 stats and times here
				//System.out.println("SOLVER: "+solvers[s]);
				stats.calcStats();
				
				AIPSResults = AIPSResults+","+stats.getAIPSMAE();
				AIResults= AIResults+","+stats.getAIMAE();
				TIResults=TIResults+","+stats.getTIMAE();
				TimeResults = TimeResults+","+stats.getTime();

			}	//end time loop

			AIPSResults = AIPSResults+"\n";
			AIResults= AIResults+"\n";
			TIResults=TIResults+"\n";
			TimeResults = TimeResults+"\n";
		}
		//instruct handler to print results, print all at once for now (if string too long, print earlier...)
		resultHandler.writeAIPSResults(AIPSResults);
		resultHandler.writeAIResults(AIResults);
		resultHandler.writeTIResults(TIResults);
		resultHandler.writeTimeResults(TimeResults);
		resultHandler.flushAndClose();

		//TODO: This should be in stats and Result Handler
		//resultHandler.compareObjectiveResults(solvers[0], solvers[1], 1, results);
	}

	/*
	 * runEvaluations runs the specified set of files through a set of solvers
	 * acting as each of a list of agents.
	 * Collects data about the behavior of agents
	 */
	public void calcBehaviorStats(SolverType[] solvers, String[] agentNames,
			double timeCutoff ){
		resultHandler = new QAEvaluationResultHandler(folder.getParent(), null);
		String behaviorResults = "";

		//for each solver, for each agent, run all instances and collect performance and
		// behavior stats
		for(int s = 0; s<solvers.length;s++){
			for(int a=0; a<agentNames.length; a++){
				//for(int t=0;t<timeCutoffs.length;t++){
				SolverBehaviorStats stats = new SolverBehaviorStats();

				boolean output = false;
				for(int f = 0; f<filesToTest.length; f++){
					//for(int f = 0; f<1000; f++){
					String[] sfileName = filesToTest[f].getAbsolutePath().split("/");
					String last = sfileName[sfileName.length-1];
					//if legit file,
					if(last.compareToIgnoreCase(".svn")!=0 && !last.contains(".csv") && 
							!filesToTest[f].isDirectory()){

						//System.out.println("File: "+filesToTest[f].getAbsolutePath());
						//create a way to store all results for a given file (across agents)
						QAInstanceResults res = new QAInstanceResults(filesToTest[f].getAbsolutePath(), agentNames[a]);
						if(res.getQAInstAll()!=null){
							//System.out.println(res+" "+filesToTest[f]);
							results.put(filesToTest[f], res);

							//run instance
							ResultValues rieResVals =runInstance(solvers[s], timeCutoff, results.get(filesToTest[f]), true);
							if(rieResVals.getPredictedWaterfall()!=null){
								//update stats
								stats.updateMAES(results.get(filesToTest[f]).calcAgentImpsPerSlotMAE(solvers[s]),
										results.get(filesToTest[f]).calcAgentImpsMAE(solvers[s]),
										results.get(filesToTest[f]).calcTotalImpsMAE(solvers[s]));

								//update behavior stats
								if(results.get(filesToTest[f]).getQADataAll().isAgentIn(agentNames[a])){
									stats.incrementN();
								}
								if(filesToTest[f].getName().contains("Querytype13")||filesToTest[f].getName().contains("Querytype13")){
									stats.incrementNumF2();
								}
								if(results.get(filesToTest[f]).getQADataAll().isIntegerPosition(agentNames[a])){
									stats.incrementNumIntPosition();
								}
								if(results.get(filesToTest[f]).getQADataAll().isInTopSlot(agentNames[a])){
									stats.incrementNumTopSlot();
								}
								if(results.get(filesToTest[f]).getQADataAll().isProbing(agentNames[a])){
									stats.incrementNumProbing();
								}
								if(results.get(filesToTest[f]).getQADataAll().numLongTerm(agentNames[a])){
									stats.incrementNumLongTerm();
								}
								stats.incrementpercentSeen(results.get(filesToTest[f]).getQADataAll().getPercentImpressionsSeen(agentNames[a]));
								output = true;
							}

						}else{
							output = false;
							System.out.println("_________________________SKIPPING REVISIONS________________________");
						}
					}else{
						//System.out.println("SKIPPING NON-AUCTION FILE");
					}
				}

				if(output){
					//output files for all 3 stats for ie and rie here
					//System.out.println("SOLVER: "+agentNames[a]);
					//waterfall
					//to file 
					stats.calcStats();

					//add to string to be printed to file
					behaviorResults=behaviorResults+","+stats.getAIPSMAE()+","+stats.getAIMAE()+","+stats.getTIMAE()+","+
					stats.getN()+","+stats.getNumF2()/stats.getN()+","+stats.getNumIntPosition()/stats.getN()+","+
					stats.getNumTopSlot()/stats.getN()+","+stats.getPercentSeen()/stats.getN()+","+stats.getNumProbing()/stats.getN()
					+","+stats.getNumLongTerm()/stats.getN(); 
				}

				//end time loop
				//}	

				behaviorResults+="\n";

				//print results to file and close output file
				resultHandler.writeBehaviorResults(behaviorResults);
				resultHandler.flushAndCloseBehavior();
			}
		}
	}

	/*
	 * Runs a particular solver on an auction instance
	 */
	private ResultValues runInstance(SolverType solver, double timeCut, 
			QAInstanceResults qaInstanceResults, boolean rankKnown) {
		//ERIC: NOTE: rankKnown is never used; just ORDERING_KNOWN.

		//we generate an instance from the log data
		QAInstanceAll inst = qaInstanceResults.getQAInstAll();
		QAInstanceSampled instSamp = qaInstanceResults.getQAInstSampled();
		QAInstanceExact instExact = qaInstanceResults.getQAInstExact();

		ImpressionAndRankEstimator fullModel = null;
		AbstractImpressionEstimator model = null;

		QADataAll temp = qaInstanceResults.getQADataAll();

		//get agent ids, use to get reducedImps and true ordering
		int[] agentIds = inst.getAgentIds();
		int[] trueAgentImps = qaInstanceResults.getQADataAll().getTrueImpressions(agentIds);
		//int[][] trueWaterfall = qaInstanceResults.getQADataAll().getTrueImpressions(agentIds);

		int[] trueOrdering = temp.getBidOrder(agentIds);

		double start = System.currentTimeMillis(); //time the prediction time on this instance

		//---------------- SOLVE AN INSTANCE ----------------------

		//sampled problem
		if(SAMPLED_AVERAGE_POSITIONS) {
			if (solver == SolverType.CP) {
				model = new ImpressionEstimatorSample(inst, sampFrac, fractionalBran, numSamples, timeCut);
			}
			if (solver == SolverType.ERIC_MIP_MinSlotEric) {
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, true, Objective.DEPENDS_ON_CIRCUMSTANCES);
			}
			
			if (solver == SolverType.CARLETON_SIMPLE_MIP_Sampled) {
				model = new ImpressionEstimatorSimpleMIPSampled(instSamp, timeCut,cplex);
			}


			//NOT Sampled Problem
		}else{ 
			if (solver == SolverType.CP){
				model = new ImpressionEstimatorExact(inst, sampFrac, fractionalBran, timeCut);
			}

			//should be called MIP_IP
			if (solver == SolverType.IP) {
				inst.setAvgPos(instExact.getAvgPos());

				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
				model = new EricImpressionEstimator(inst, useRankingConstraints, true, false, timeCut,cplex, false, Objective.DEPENDS_ON_CIRCUMSTANCES);
			}
			//Eric and Betsy aren't sure what this is...you might need to choose among multiple solutions????
			//IGNORE FOR NOW!!!
			if (solver == SolverType.MULTI_MIP) {

				if (ORDERING_KNOWN) {
					inst.setAvgPos(instExact.getAvgPos());
					double[] samp = inst.getSampledAvgPos();
					Arrays.fill(samp, -1);
					inst.setSampledAvgPos(samp);
				}
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, true, timeCut,cplex, false, Objective.DEPENDS_ON_CIRCUMSTANCES);

			}
			//should be called MIP
			if (solver == SolverType.MIP) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, false, Objective.DEPENDS_ON_CIRCUMSTANCES);
			}
			if (solver == SolverType.ERIC_MIP_MinSlotEric) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, false, Objective.MINIMIZE_SLOT_DIFF);
			}
			if (solver == SolverType.ERIC_MIP_MinSlotCJC) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, false, Objective.MINIMIZE_SLOT_DIFF_CJC);
			}
			if (solver == SolverType.ERIC_MIP_MinPriors) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, false, Objective.MINIMIZE_IMPRESSION_PRIOR_ERROR);
			}
			if (solver == SolverType.ERIC_MIP_MinTotal) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, false, Objective.MINIMIZE_TOTAL_IMPRESSIONS);
			}
			if (solver == SolverType.ERIC_MIP_MaxTotal) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new EricImpressionEstimator(inst, !ORDERING_KNOWN, false, false, timeCut,cplex, false, Objective.MAXIMIZE_TOTAL_IMPRESSIONS);
			}
			

			if (solver == SolverType.LDSMIP)model = new EricImpressionEstimator(inst, false, false, false, timeCut,cplex, false, Objective.DEPENDS_ON_CIRCUMSTANCES);

			if (solver == SolverType.Carleton_LP) model = new DropoutImpressionEstimatorAll(inst, false, false, false, timeCut);	

			if (solver == SolverType.Carleton_LP_Exact) model = new DropoutImpressionEstimatorExact(instExact);	

			if (solver == SolverType.CARLETON_SIMPLE_MIP_Exact) model = new ImpressionEstimatorSimpleMIPExact(instExact, timeCut,cplex);

			if (solver == SolverType.CARLETON_SIMPLE_MIP_Exact_MinTotal) model = new ImpressionEstimatorMaxTotalImpressions(instExact, timeCut,cplex);	

			if (solver == SolverType.CARLETON_SIMPLE_MIP_Exact_MinDiffTotal) model = new ImpressionEstimatorSimpleMIPMinDiffTotalImpressions(instExact, timeCut,cplex);

			if (solver == SolverType.CARLETON_SIMPLE_MIP_Exact_WeightedMinDiffTotal) model = new ImpressionEstimatorSimpleMIPWeightedMinDiffTotalImp(instExact, timeCut,cplex);	

			if (solver == SolverType.CARLETON_SIMPLE_MIP_Sampled) model = new ImpressionEstimatorSimpleMIPSampled(instSamp, timeCut,cplex);

			if (solver == SolverType.CARLETON_SIMPLE_MIP_Exact_PRIORS) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new ImpressionEstimatorSimpleMIPPriors(inst, timeCut,cplex);
			}
			if (solver == SolverType.CARLETON_SIMPLE_MIP_Exact_Total_Priors) {
				inst.setAvgPos(instExact.getAvgPos());
				double[] samp = inst.getSampledAvgPos();
				Arrays.fill(samp, -1);
				inst.setSampledAvgPos(samp);
				model = new ImpressionEstimatorSimpleMipTotalPriors(inst, timeCut,cplex);
			}	
		}

		//set full model based on known or unknown bid order.
		if (ORDERING_KNOWN) {
			fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
		} else {
			fullModel = new LDSImpressionAndRankEstimator(model);
		}

		//---------------- SOLVE FOR RESULT -----------------------

		IEResult result = fullModel.getBestSolution();

		//Get predictions (also provide dummy values for failure)
		int[] predictedImpsPerAgent;
		int[] predictedOrdering;
		int[][] predictedWaterfall;
		int predictedTotImpr = 0;
		double objective = 0;

		//if solve successful, set result values
		if (result != null) {
			predictedImpsPerAgent = result.getSol();
			predictedWaterfall = result.getWaterfall();
			predictedOrdering = result.getOrder();
			predictedTotImpr = result.getSlotImpressions()[0];
			objective = result.getObj();
			if(Double.isInfinite(objective)){
				System.out.println("Failed to Solve");
				objective = 0.0;
			}
			//if solve wasn't successful, set default values for error calculation
		} else {
			System.out.println("_______________________________NULL RESULT!!!!!!!!!_____________________________________");
			predictedImpsPerAgent = new int[trueAgentImps.length];
			Arrays.fill(predictedImpsPerAgent, 0);

			predictedOrdering = new int[trueAgentImps.length];
			Arrays.fill(predictedOrdering, 0);

			predictedTotImpr = 0;

			predictedWaterfall = new int[agentIds.length][instExact.getNumSlots()];
			for(int w = 0;w<agentIds.length;w++){
				int[] zeros = new int[instExact.getNumSlots()];
				Arrays.fill(zeros, 0);
				predictedWaterfall[w]= zeros;
			}
			//nullIEResult++;
		}

		double stop = System.currentTimeMillis();
		double secondsElapsed = (stop - start) / 1000.0;
		//System.out.println("SECONDS: "+secondsElapsed);

		//System.out.println("obj: "+objective);

		//store and then return result values
		qaInstanceResults.setResult(solver, predictedImpsPerAgent, predictedWaterfall, predictedTotImpr, objective,secondsElapsed);
		return qaInstanceResults.getResult(solver);
		//totalAlgRunTime += secondsElapsed;
		//System.out.println("predicted: " + Arrays.toString(predictedImpsPerAgent));
		//System.out.println("actual: " + Arrays.toString(reducedImps));
	}


	public QAInstanceResults[] sortResults(SolverType solver, String compareVal){
		QAInstanceResults[] resultList = new QAInstanceResults[results.size()];
		Comparator<QAInstanceResults> comparer;
		if(compareVal.compareToIgnoreCase("stat")==0){
			comparer = new CompareSolverResults(solver, compareVal);

		}else if(compareVal.compareToIgnoreCase("time")==0){
			comparer = new CompareTimeResults(solver);

		}else{
			comparer = new CompareSolverResults(solver, compareVal);
		}
		Set<File> keys = results.keySet(); 
		int i =0;
		for(File f: keys){
			resultList[i] = results.get(f);
			i+=1;
		}
		Arrays.sort(resultList, comparer);

		return resultList;

	}

	/*
	 * Compare two solvers based on a given delta
	 * TODO: This should be printed to a file
	 */
	private void compareObjectivesResults(SolverType solver1,
			SolverType solver2, double delta) {
		//ans = [difference, number different by more than delta]
		double[] diffResults = resultHandler.compareObjectiveResults(solver1, solver2, delta, results);
		System.out.println("Total objective difference: "+diffResults[0]+". Number instances different by more than delta: "+diffResults[1]);
	}


	/*
	 * Sorts array of files to different folders by type of instance
	 */
	private void sortToTypes(String pathToResults){

		FileCopier copier = new FileCopier();
		new File(pathToResults).mkdir();
		for(int f =0;f<filesToTest.length;f++){
			//for(int f = 140; f<145; f++){
			if(results.get(filesToTest[f])!=null){
				if(results.get(filesToTest[f]).getQADataAll()!=null){
					
					//note: if more types wanted, make this more modular? With boolean choices?
					if(results.get(filesToTest[f]).getQADataAll().isProbing()){
						new File(pathToResults+"/probing").mkdir();
						copier.copyFile(filesToTest[f].getAbsolutePath(), pathToResults+"/probing/"+filesToTest[f].getName());
					}
					if(results.get(filesToTest[f]).getQADataAll().hasLongTermPlayer()){
						new File(pathToResults+"/longTerm").mkdir();
						copier.copyFile(filesToTest[f].getAbsolutePath(), pathToResults+"/longTerm/"+filesToTest[f].getName());
					}
				}
			}
		}
	}

	/*
	 * Provides an elementary visualization of waterfalls
	 * Note: Eric's code may have a better version of this that is more visually pleasing
	 */
	private void visualizeWaterfall(int[][] waterfallOrig){

		//copy so can set values to 0 as they are used
		int[][] waterfall = new int[waterfallOrig.length][];
		for(int j=0;j<waterfallOrig.length;j++){
			waterfall[j]=Arrays.copyOf(waterfallOrig[j], waterfallOrig[j].length);
		}

		int[][] vizValues = new int[waterfallOrig[0].length][waterfallOrig.length];
		int[][] vizAgents = new int[waterfallOrig[0].length][waterfallOrig.length];

		for(int i = 0;i< waterfallOrig[0].length; i++){
			int[] temp1 = new int[waterfallOrig.length];
			Arrays.fill(temp1, 0);
			int[] temp2 = new int[waterfallOrig.length];
			Arrays.fill(temp2, 0);
			vizValues[i] = temp1;
			vizAgents[i] = temp2;
		}
		int j = 0;

		//loop through non-zeros portions of the waterfall
		while(isNonZero(waterfall)&&j<waterfall.length){
			for (int a = 0; a<waterfallOrig.length;a++){
				int i = waterfallOrig[0].length -1;
				//loop backwards through 2d array
				while (waterfall[a][i]==0){
					if(i>0){
						i = i-1;
					}else{
						break;
					}
				}
				//System.out.println("a "+a+" slot "+i+" period "+j+" wf:"+ waterfall[a][i]);
				if(waterfall[a][i]>0){
					vizValues[i][j] = waterfall[a][i];
					vizAgents[i][j] = a;
					waterfall[a][i]=0;
				}
			}
			j=j+1;
		}
		//print final values for visualization
		for (int l = 0; l<vizValues.length;l++){
			for (int k = 0; k<vizValues[0].length;k++){
				if(vizValues[l][k]!=0){
					System.out.printf("|A: %1d N: % 4d|", vizAgents[l][k],vizValues[l][k]);
				}
			}
			System.out.println();
		}
	}


	//checks if a waterfall is non-zero in some entries.
	private boolean isNonZero(int[][] waterfall) {
		boolean nz = true;
		for (int i = 0; i<waterfall.length;i++){
			for (int j = 0; j<waterfall[0].length;j++){
				if(waterfall[i][j]!=0){
					nz = true;
				}
			}
		}
		return nz;
	}


	//The following commented out code consists of old, depreciated methods
	/*	private void printResults(SolverType solver, QAInstanceResults[] res) {

		System.out.println("Solver: "+solver);
		for(int r = 0; r<res.length; r++){
			int size = res[r].getFilename().split("/").length;
			//if(res[r].getStat(solver)==Double.MAX_VALUE){
			//System.out.println("File: "+res[r].getFilename().split("/")[size-1]+" Stat: FAILED AbsError:"+res[r].getAbsError(solver)+" Time:"+res[r].getTime(solver));

			//}else{
			//System.out.println("File: "+res[r].getFilename().split("/")[size-1]+" Stat:"+res[r].getStat(solver)+" AbsError:"+res[r].getAbsError(solver)+" Time:"+res[r].getTime(solver));
			//}
		}
	}

	private void printSolverSorted(PriorityQueue<ResultValues> sorted, File file, String value) {
		System.out.print("File: "+file.getName());
		ResultsComparator comp = new ResultsComparator(value);
		PriorityQueue<ResultValues> newSorted = new PriorityQueue<ResultValues>(1,comp);
		while(sorted.peek()!=null){
			ResultValues toP = sorted.poll();
			newSorted.add(toP);
			if(value.compareTo("stat")==0){
				//System.out.print(" Solver: "+toP.getSolverType()+": "+toP.getStat());
			}else if(value.compareTo("time")==0){
				//System.out.print(" Solver: "+toP.getSolverType()+": "+toP.getTime());
			}else if(value.compareTo("absError")==0){
				//System.out.print(" Solver: "+toP.getSolverType()+": "+toP.getAbsError());
			}

		}
		results.get(file).setSorted(newSorted);
		System.out.println();

	}

	private void printSolverSortedToFile(PriorityQueue<ResultValues> sorted, File file, String value, FileWriter writer) {
		try{

			ResultsComparator comp = new ResultsComparator(value);
			PriorityQueue<ResultValues> newSorted = new PriorityQueue<ResultValues>(1,comp);
			while(sorted.peek()!=null){
				ResultValues toP = sorted.poll();
				newSorted.add(toP);
				if(value.compareTo("stat")==0){
					//writer.append(file.getName()+','+toP.getSolverType()+','+toP.getStat());
				}else if(value.compareTo("time")==0){
					writer.append(file.getName()+','+toP.getSolverType()+','+toP.getTime());
				}else if(value.compareTo("absError")==0){
					//writer.append(file.getName()+','+toP.getSolverType()+','+toP.getAbsError());
				}
				if(sorted.peek()!=null){
					writer.append(',');
				}

			}
			results.get(file).setSorted(newSorted);
			writer.append('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getResults(SolverType[] solvers, boolean compareSolver, boolean time, boolean sortedSolvers){
		if(compareSolver){
			QAInstanceResults[][] solverresults = new QAInstanceResults[solvers.length][];
			QAInstanceResults[][] solverresults2 = new QAInstanceResults[solvers.length][];
			for(int s = 0; s<solvers.length; s++){ 
				System.out.println("Sorted by Absolute Prediction Error");
				solverresults[s] = sortResults(solvers[s], "stat");
				printResults(solvers[s], solverresults[s]);
				solverresults2[s] = sortResults(solvers[s], "absError");
				System.out.println("Sorted by Absolute Error");
				printResults(solvers[s], solverresults2[s]);
			}
		}
		if(time){
			QAInstanceResults[][] timeresults = new QAInstanceResults[solvers.length][];
			for(int s = 0; s<solvers.length; s++){
				System.out.println("Sorted by Time");
				timeresults[s] = sortResults(solvers[s], "time");
				printResults(solvers[s], timeresults[s]);
			}

		}
		if(sortedSolvers){

			try{	
				File output;
				System.out.println("To Compare Solvers");
				Set<File> files = results.keySet();  
				System.out.println("Sorted by Absolute Prediction Error");
				//sortToTypes();
				for(File f: files){

					results.get(f).makeSortedResults("stat");
					printSolverSorted(results.get(f).getSorted(), f,"stat");
					output= new File(folder.getAbsolutePath()+"/sorted_stat_results.csv");
					FileWriter writer;
					writer = new FileWriter(output);
					printSolverSortedToFile(results.get(f).getSorted(), f,"stat",writer);

				}
				sortToFiles("stat");

				System.out.println("Sorted by Absolute Error");
				for(File f: files){

					results.get(f).makeSortedResults("absEror");
					printSolverSorted(results.get(f).getSorted(), f,"absError");
					output = new File(folder.getAbsolutePath()+"/sorted_absError_results.csv");
					FileWriter writer;
					writer = new FileWriter(output);
					printSolverSortedToFile(results.get(f).getSorted(), f,"absError",writer);
				}
				sortToFiles("absError");
				System.out.println("Sorted by Time");
				for(File f: files){

					results.get(f).makeSortedResults("time");
					printSolverSorted(results.get(f).getSorted(), f,"time");
					output = new File(folder.getAbsolutePath()+"/sorted_time_results.csv");
					FileWriter writer;
					writer = new FileWriter(output);
					printSolverSortedToFile(results.get(f).getSorted(), f,"time",writer);
				}
				sortToFiles("time");


			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private void sortToFiles(String value) {
		FileCopier copier = new FileCopier();
		new File(folder.getAbsolutePath()+"/"+value).mkdir();
		ResultsComparator comp = new ResultsComparator(value);

		Set<File> files = results.keySet();
		for(File f : files){
			PriorityQueue<ResultValues> sorted = results.get(f).getSorted();
			PriorityQueue<ResultValues> newSorted = new PriorityQueue<ResultValues>(1,comp);
			if(sorted.peek()!=null){
				ResultValues toS = sorted.poll();
				newSorted.add(toS);
				String solver = toS.getSolverType().toString();
				new File(folder.getAbsolutePath()+"/"+value+"/"+solver.toString()).mkdir();
				copier.copyFile(f.getAbsolutePath(), f.getParent()+"/"+value+"/"+solver+"/"+f.getName());
				if(results.get(f).getQADataAll().isProbing()){
					new File(folder.getAbsolutePath()+"/"+value+"/"+solver.toString()+"/probing").mkdir();
					copier.copyFile(f.getAbsolutePath(), f.getParent()+"/"+value+"/"+solver+"/probing/"+f.getName());
				}
				if(results.get(f).getQADataAll().hasLongTermPlayer()){
					new File(folder.getAbsolutePath()+"/"+value+"/"+solver.toString()+"/longTerm").mkdir();
					copier.copyFile(f.getAbsolutePath(), f.getParent()+"/"+value+"/"+solver+"/longTerm/"+f.getName());
				}


			}
			results.get(f).setSorted(newSorted);
		}
	}
		private void printResults(SolverType[] solvers) {
				Set<File> keys = results.keySet();
				for(int s = 0; s<solvers.length; s++){
					System.out.println("Solver: "+solvers[s]);

					for(File f : keys){
						int size = results.get(f).getFilename().split("/").length;
						System.out.println("File: "+results.get(f).getFilename().split("/")[size ]+" Stat:"+results.get(f).getStat(solvers[s])+" Time:"+results.get(f).getTime(solvers[s]));
					}
				}
		   } 

			public void generateGraphData(SolverType[] solvers, String folderPath, boolean rankKnown, double[] timeCutoffs){
				ORDERING_KNOWN = rankKnown;
				for(int t =0;t<timeCutoffs.length;t++){
					folder = new File(folderPath);
					filesToTest = folder.listFiles();
					runTests(solvers);
					getResults(solvers, true,true, true, true);

				}

			}
	//Get the indices of the vals, starting with the highest val and decreasing.

	public static int[] getIndicesForDescendingOrder(double[] valsUnsorted) {
		double[] vals = valsUnsorted.clone(); //these values will be modified
		int length = vals.length;

		int[] ids = new int[length];
		for (int i = 0; i < length; i++) {
			ids[i] = i;
		}

		for (int i = 0; i < length; i++) {
			for (int j = i + 1; j < length; j++) {
				if (vals[i] < vals[j]) {
					double tempVal = vals[i];
					int tempId = ids[i];

					vals[i] = vals[j];
					ids[i] = ids[j];

					vals[j] = tempVal;
					ids[j] = tempId;
				}
			}
		}
		return ids;
	}
	 */
 
	public static void main(String args[]){
		int totalTime = 0;
		//for (int i =1; i<60; i++){
		//SET SOLVERS
		//OPTIONS: SolverType.SIMPLE_MIP_Exact_WeightedMinDiffTotal, SolverType.SIMPLE_MIP_Exact_MinDiffTotal, 
		//          , SolverType.SIMPLE_MIP_Exact_PRIORS, 
		//           SolverType.SIMPLE_MIP_Sampled, SolverType.CP, SolverType.IP, SolverType.MIP, SolverType.MIP_MinTotal,
		
//		SolverType.IP,
//				SolverType.MIP_MinSlotEric, SolverType.MIP_MinSlotCJC, 
//				SolverType.MIP_MinPriors, 
//				SolverType.MIP_MinTotal, SolverType.MIP_MaxTotal,
//
//		SolverType[] solvers = {SolverType.ERIC_MIP_MinSlotEric, SolverType.ERIC_MIP_MinSlotCJC, 
//				SolverType.ERIC_MIP_MinPriors, 
//				SolverType.ERIC_MIP_MinTotal, SolverType.ERIC_MIP_MaxTotal,
//				SolverType.CARLETON_SIMPLE_MIP_Exact_MinTotal, SolverType.CARLETON_SIMPLE_MIP_Exact_MinDiffTotal, 
//				SolverType.CARLETON_SIMPLE_MIP_Exact_WeightedMinDiffTotal, SolverType.CARLETON_SIMPLE_MIP_Exact_PRIORS, SolverType.CARLETON_SIMPLE_MIP_Exact_Total_Priors};
//		SolverType[] solvers = {SolverType.MIP_MinTotal,
//				SolverType.SIMPLE_MIP_Exact_MinTotal};
		
		SolverType[] solvers = { SolverType.ERIC_MIP_MinSlotEric, SolverType.CARLETON_SIMPLE_MIP_Sampled, SolverType.CP};
		//SolverType[] solvers = {SolverType.CP};
		
		
		//SET TIME CUTOFFS (seconds)
		//double[] timeCutoffs = {.001, .01, .05, .1, .2, .3, .4, 1, 20};
		//double[] timeCutoffs = {.01, .05, .2, .3, 5}; //ERIC: changed this
		double[] timeCutoffs = {.1};
		//SET AGENT NAMES to look at behavior.
		//the following are all known options
		//TODO: fix bug where solver picks new agent to use
		String[] agentNames = {"Schlemazl", "TacTex", "hermes", "tau", "EDAAgent", "CrocodileAgent", "AA-HEU", "Mertacor", "PoleCAT"};

		//CREATE EVALUATOR
		// Folder Options: ./GameLogFiles_server1, ./GameLogFiles_server2, ./Testing_logFiles, ./Interesting, ./GameLogFiles_server2/testingFileIn.txt
		// can also pass a text file location to read specific file names from
		long start = System.currentTimeMillis();
		QAAlgorithmEvaluator tester = new QAAlgorithmEvaluator("./GameLogFiles_server2", timeCutoffs);

		//RUN EVALUATIONS
		tester.runEvaluations(solvers, false, timeCutoffs);
		totalTime +=(System.currentTimeMillis()-start);
		//}
		long start2 = System.currentTimeMillis();
//		QAAlgorithmEvaluator tester2 = new QAAlgorithmEvaluator("./GameLogFiles_server2", timeCutoffs);
//.
//		//RUN EVALUATIONS
//		tester2.runEvaluations(solvers, false, timeCutoffs);
	
		System.out.println("Total 1: "+totalTime+" Total2: "+(System.currentTimeMillis()-start2));
		//double delta = 20.0;
		//tester.compareObjectivesResults(solvers[0], solvers[1], delta);
//		delta = 50.0;
//		tester.compareObjectivesResults(solvers[0], solvers[1], delta);
//		delta = 100.0;
//		tester.compareObjectivesResults(solvers[0], solvers[1], delta);
		//tester.calcBehaviorStats(solvers, agentNames, 0);

		//tester.sortToTypes("./sorted_files");

		//SORT FILES (This may be depreciated.)
		//tester.sortToTypes("./GameLogFiles_server1");

	}
}