package simulator.predictions;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import simulator.predictions.ImpressionEstimatorTest.SolverType;

import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
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
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPExact;
/*
 * QAAlgorithmTester
 * 
 * This class runs a folder of games through user specified Query Analyzer algorithms and collects and sorts statistics on
 * the speed and accuracy of the impressions estimation results.
 * 
 * Betsy Hilliard betsy@cs.brown.edu
 */
public class QAAlgorithmTester {
	
	private static final boolean SAMPLED_AVERAGE_POSITIONS = false;
	private static final boolean ORDERING_KNOWN = true;
	private static final double IP_TIMEOUT_IN_SECONDS = 0;
	private static final boolean CHEATING = false;
	private static final boolean REMOVE_SAMPLE_NANS = false;
	
	private int numSamples = 10; 
	private int sampFrac = 100;
	private int fractionalBran = 0;
	
	File[] filesToTest;
	HashMap<File, QAInstanceResults> results = new HashMap<File, QAInstanceResults>();
	
	//constructor for the class
	//takes a path to the folder where the files to test are
	public QAAlgorithmTester(String pathToFile){
		 
		  File folder = new File(pathToFile);
		  filesToTest = folder.listFiles();
		  //System.out.println(filesToTest.length);
		  
		  
	}
	
	//runs the specified solvers on the files
	public void runTests(SolverType[] solvers){

		for(int s=0; s<solvers.length; s++){
			for(int f = 0; f<filesToTest.length; f++){
				if(filesToTest[f].getAbsolutePath().split("/")[8].compareToIgnoreCase(".svn")!=0){
					if(s==0){
						QAInstanceResults res = new QAInstanceResults(filesToTest[f].getAbsolutePath());
						//System.out.println(res+" "+filesToTest[f]);
						results.put(filesToTest[f], res);
					}
					runInstance(results.get(filesToTest[f]), solvers[s]);
				}else{
					System.out.println("SKIPPING SVN__________________________________________________________________");
				}
			}

		}
	}

	private void runInstance(QAInstanceResults qaInstanceResults, SolverType solver) {
		ImpressionAndRankEstimator fullModel = null;
        AbstractImpressionEstimator model = null;
		QAInstanceAll inst = qaInstanceResults.getQAInstAll();
		QAInstanceExact instExact = qaInstanceResults.getQAInstExact();
		
		for(int j=0; j<inst.getAvgPos().length; j++){
			System.out.println("Avg Pos: "+inst.getAvgPos()[j]);
			System.out.println("SampAvg Pos: "+inst.getSampledAvgPos()[j]);
		}
		
		
       
		QADataAll temp = qaInstanceResults.getQADataAll();
		
		//System.out.println(temp);
		// Need to determine how many agents actually participated?
		
        //double[] reducedBids = temp.getBids();
        //TODO: fix this...
        int[] agentIds = inst.getAgentIds();
        
        int[] reducedImps = qaInstanceResults.getQADataAll().getTrueImpressions(agentIds);
       
        
		int[] trueOrdering = temp.getBidOrder(agentIds);
		for (int i = 0; i < trueOrdering.length; i++) {
          System.out.println("Ord: "+trueOrdering[i]);
		}
//       tru
		//int[] ordering;
//		
//        if (ORDERING_KNOWN) {
//           ordering = trueOrdering.clone();
//        } else {
//           ordering = new int[reducedBids.length];
//           for (int i = 0; i < ordering.length; i++) {
//              ordering[i] = i;
//           }
//        }
//		
		
		
		double start = System.currentTimeMillis(); //time the prediction time on this instance

        //---------------- SOLVE INSTANCE ----------------------
        if (solver == SolverType.CP) {
           if(SAMPLED_AVERAGE_POSITIONS) {
              if (ORDERING_KNOWN) {
                 model = new ImpressionEstimatorSample(inst, sampFrac, fractionalBran, numSamples);
                 fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
              } else {
                 model = new ImpressionEstimatorSample(inst, sampFrac, fractionalBran, numSamples);
                 fullModel = new LDSImpressionAndRankEstimator(model);
              }
           }
           else {
              if (ORDERING_KNOWN) {
            	  System.out.println("__________________________________DOING RIGHT THING____________________________");
                 model = new ImpressionEstimatorExact(inst, sampFrac, fractionalBran);
                 fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
              } else {
                 model = new ImpressionEstimatorExact(inst, sampFrac, fractionalBran);
                 fullModel = new LDSImpressionAndRankEstimator(model);
              }
           }
           
        }
        if (solver == SolverType.MIP) {
           boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
           model = new EricImpressionEstimator(inst, useRankingConstraints, true, false, IP_TIMEOUT_IN_SECONDS);
           fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
        }
        if (solver == SolverType.MULTI_MIP) {
           boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
           model = new EricImpressionEstimator(inst, useRankingConstraints, true, true, IP_TIMEOUT_IN_SECONDS);
           fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
        }
        if (solver == SolverType.MIP_LP) {
           boolean useRankingConstraints = !ORDERING_KNOWN; //Only use ranking constraints if you don't know the ordering
           model = new EricImpressionEstimator(inst, useRankingConstraints, false, false, IP_TIMEOUT_IN_SECONDS);
           fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
        }

        if (solver == SolverType.LDSMIP) {
           boolean useRankingConstraints = false;
           model = new EricImpressionEstimator(inst, useRankingConstraints, true, false, IP_TIMEOUT_IN_SECONDS);
           if (ORDERING_KNOWN) {
              fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
           } else {
              fullModel = new LDSImpressionAndRankEstimator(model);
           }
        }
        if (solver == SolverType.Carleton_LP) {
       	 if (ORDERING_KNOWN) {
                model = new DropoutImpressionEstimatorAll(inst, false, false, false, IP_TIMEOUT_IN_SECONDS);
                fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
       	 } else {
                model = new DropoutImpressionEstimatorAll(inst, false, false, false, IP_TIMEOUT_IN_SECONDS);
                fullModel = new LDSImpressionAndRankEstimator(model);
       	 }
        }
        
        if (solver == SolverType.Carleton_LP_Exact) {
       	 if (ORDERING_KNOWN) {
                model = new DropoutImpressionEstimatorExact(instExact);
                fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
       	 } else {
                model = new DropoutImpressionEstimatorExact(instExact);
                fullModel = new LDSImpressionAndRankEstimator(model);
       	 }
        }
        
        if (solver == SolverType.SIMPLE_MIP_Exact) {
       	 if (ORDERING_KNOWN) {
                model = new ImpressionEstimatorSimpleMIPExact(instExact);
                fullModel = new ConstantImpressionAndRankEstimator(model, trueOrdering);
       	 } else {
                model = new ImpressionEstimatorSimpleMIPExact(instExact);
                fullModel = new LDSImpressionAndRankEstimator(model);
       	 }
        }

        //---------------- SOLVE FOR RESULT -----------------------
        IEResult result = fullModel.getBestSolution();
        //Get predictions (also provide dummy values for failure)
        int[] predictedImpsPerAgent;
        int[] predictedOrdering;
        int predictedTotImpr = 0;

        if (result != null) {
           predictedImpsPerAgent = result.getSol();
           predictedOrdering = result.getOrder();
           predictedTotImpr = result.getSlotImpressions()[0];
           //int[] predictedRelativeOrdering = getRelativeOrdering(predictedOrdering);
        } else {
          
           predictedImpsPerAgent = new int[reducedImps.length];
           Arrays.fill(predictedImpsPerAgent, -1);

           predictedOrdering = new int[reducedImps.length];
           Arrays.fill(predictedOrdering, -1);

           predictedTotImpr = -1;

           //nullIEResult++;
        }
        
        double stop = System.currentTimeMillis();
        double secondsElapsed = (stop - start) / 1000.0;
        //System.out.println("SECONDS: "+secondsElapsed);
        double stat = 0.0;
        double absError = 0.0;
        boolean failed = false;
        for(int s = 0; s<predictedImpsPerAgent.length; s++){
        	if(predictedImpsPerAgent[s]<=0){
        		failed = true;
        	}
        	stat+=(double)Math.abs(reducedImps[s]-predictedImpsPerAgent[s])/reducedImps[s];
        	
        	System.out.println("Real: "+reducedImps[s]+" Pred: "+predictedImpsPerAgent[s]+" stat: "+stat);
        	absError+=Math.abs(reducedImps[s]-predictedImpsPerAgent[s]);
        }
        if(failed){
        	stat = Double.MAX_VALUE;
        }else{
        	stat = stat/predictedImpsPerAgent.length;
        }
        
        qaInstanceResults.setResult(solver, predictedImpsPerAgent, reducedImps, stat, absError, secondsElapsed);
        //totalAlgRunTime += secondsElapsed;

        //System.out.println("predicted: " + Arrays.toString(predictedImpsPerAgent));
        //System.out.println("actual: " + Arrays.toString(reducedImps));
		
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
	   
	   
	   public void getResults(SolverType[] solvers, boolean compareSolver, boolean time){
		   if(compareSolver){
			   QAInstanceResults[][] solverresults = new QAInstanceResults[solvers.length][];
			   QAInstanceResults[][] solverresults2 = new QAInstanceResults[solvers.length][];
			   for(int s = 0; s<solvers.length; s++){
				   solverresults[s] = sortResults(solvers[s], "stat");
				   //System.out.println(solverresults[s][2].getStat(solvers[s]));
				   //System.out.println(solverresults[s][1].getGuessedImps(solvers[s])[2]);
				   printResults(solvers[s], solverresults[s]);
				   solverresults2[s] = sortResults(solvers[s], "absError");
				   printResults(solvers[s], solverresults[s]);
			   }
		   }
		   if(time){
			   QAInstanceResults[][] timeresults = new QAInstanceResults[solvers.length][];
			   for(int s = 0; s<solvers.length; s++){
				   timeresults[s] = sortResults(solvers[s], "time");
				   //printResults(solvers[s], timeresults[s]);
			   }
			   
		   }
	   }
//	   private void printResults(SolverType[] solvers) {
//			Set<File> keys = results.keySet();
//			for(int s = 0; s<solvers.length; s++){
//				System.out.println("Solver: "+solvers[s]);
//				
//				for(File f : keys){
//					int size = results.get(f).getFilename().split("/").length;
//					System.out.println("File: "+results.get(f).getFilename().split("/")[size ]+" Stat:"+results.get(f).getStat(solvers[s])+" Time:"+results.get(f).getTime(solvers[s]));
//				}
//			}
//	   }
	   
	   private void printResults(SolverType solver, QAInstanceResults[] res) {

		   System.out.println("Solver: "+solver);
		   for(int r = 0; r<res.length; r++){
			   int size = res[r].getFilename().split("/").length;
			   if(res[r].getStat(solver)==Double.MAX_VALUE){
				   System.out.println("File: "+res[r].getFilename().split("/")[size-1]+" Stat: FAILED AbsError:"+res[r].getAbsError(solver)+" Time:"+res[r].getTime(solver));

			   }else{
				   System.out.println("File: "+res[r].getFilename().split("/")[size-1]+" Stat:"+res[r].getStat(solver)+" AbsError:"+res[r].getAbsError(solver)+" Time:"+res[r].getTime(solver));
			   }
		   }
	
	   }
	   
	   public static void main(String args[]){
		   QAAlgorithmTester tester = new QAAlgorithmTester("/gpfs/main/home/betsy/TACAA2/aa-2012/goodData");
		   SolverType[] solvers = {SolverType.CP};//, SolverType.SIMPLE_MIP_Exact};
		   tester.runTests(solvers);
		   tester.getResults(solvers, true, true);
		  
		   
	   }

	
	
}
