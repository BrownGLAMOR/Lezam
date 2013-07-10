package models.queryanalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import simulator.predictions.QAInstanceResults;
import models.queryanalyzer.QAAlgorithmEvaluator.SolverType;

public class QAEvaluationResultHandler {
	
	//initialize file writers to store experimental results
	private FileWriter AIPSwriter;
	private FileWriter AIwriter;
	private FileWriter TIwriter;
	private FileWriter Timewriter;
	private FileWriter behaviorWriter;
	
	private String resultLocation;
	
	
	public QAEvaluationResultHandler(String resultLocation, double[] timeCutoffs){
		try {

			this.resultLocation = resultLocation+"/results";
			System.out.println(resultLocation);
			AIPSwriter = new FileWriter(new File(resultLocation+"/results/aips.csv"));
			AIwriter= new FileWriter(new File(resultLocation+"/results/ai.csv"));
			TIwriter= new FileWriter(new File(resultLocation+"/results/ti.csv"));
			Timewriter= new FileWriter(new File(resultLocation+"/results/time.csv"));
			behaviorWriter= new FileWriter(new File(resultLocation+"/results/behavior.csv"));
			AIPSwriter.write("Algorithm,Metric");
			AIwriter.write("Algorithm,Metric");
			TIwriter.write("Algorithm,Metric");
			
			for(int tc = 0;tc<timeCutoffs.length;tc++){
				AIPSwriter.write(","+timeCutoffs[tc]);
				AIwriter.write(","+timeCutoffs[tc]);
				TIwriter.write(","+timeCutoffs[tc]);
			}
			AIPSwriter.write("\n");
			AIwriter.write("\n");
			TIwriter.write("\n");
			
		
		} catch (IOException e) {
			System.err.println("Error initializing Result Files");
			e.printStackTrace();
		}
		
		
		
	}
	
	public double[] compareObjectiveResults(SolverType solver1, SolverType solver2, double delta, HashMap<File, QAInstanceResults> results){

		
		
		
		double diff = 0.0;
		int countDiff = 0;
		int countZero = 0;
		double[] diffResults = {diff, countDiff};
		try {
			FileWriter writer = new FileWriter(new File(resultLocation+"/furtherThanDelta.txt"));
			FileWriter zeroWriter = new FileWriter(new File(resultLocation+"/FailedFiles.txt"));
			//System.out.println(resultLocation+"/furtherThanDelta.txt");
		Set<File> keys = results.keySet(); 
		for(File f: keys){
			if(results.get(f).getResults().get(solver1)!=null&& results.get(f).getResults().get(solver2)!= null){
				double diffVal = Math.abs(results.get(f).getResults().get(solver1).getObjective()-
						results.get(f).getResults().get(solver2).getObjective());
				if( results.get(f).getResults().get(solver1).getObjective()==0.0 ||
						results.get(f).getResults().get(solver2).getObjective()==0.0){
					countZero+=1;
					zeroWriter.write(f.getAbsolutePath()+"\n");
				}
				//System.out.println("File: "+f+" "+(results.get(f).getResults().get(solver1).getObjective()+" "+results.get(f).getResults().get(solver2).getObjective()));

				diff = diff + diffVal;
				if(diffVal>delta){
					countDiff++;
					writer.write(f.getAbsolutePath()+"\n");
				}
			}
		}
		diffResults[0] = diff;
		diffResults[1] = countDiff;
		
		writer.flush();
		writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Count Zeros: "+countZero);
		return diffResults;
	}
	
	public void startNewLines(SolverType solver){
		
		try {
			AIPSwriter.write(solver.toString()+",aips");
			AIwriter.write(solver.toString()+",ai");
			TIwriter.write(solver.toString()+",ti");
			Timewriter.write(solver.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void startBehaviorLine(SolverType solver, String agentName){
		try {
			behaviorWriter.write(solver.toString()+",");
			behaviorWriter.write(agentName);

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
	public void writeAIPSResults(String toWrite){
		try {
			AIPSwriter.write(toWrite);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAIResults(String toWrite){
		try {
			AIwriter.write(toWrite);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTIResults(String toWrite){
		try {
			TIwriter.write(toWrite);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTimeResults(String toWrite){
		try {
			Timewriter.write(toWrite);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void flushAndClose() {
		try {
			AIPSwriter.flush();
			AIwriter.flush();
			TIwriter.flush();
			Timewriter.flush();
			AIPSwriter.close();
			AIwriter.close();
			TIwriter.close();
			Timewriter.close();
		} catch (IOException e) {
			System.err.println("Error writing results.");
			e.printStackTrace();
		}
		
	}

	public void writeBehaviorResults(String behaviorResults) {
		try {
			behaviorWriter.write(behaviorResults);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void flushAndCloseBehavior() {
		
		try {
			behaviorWriter.flush();
			behaviorWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
