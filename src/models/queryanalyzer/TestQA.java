package models.queryanalyzer;

import models.queryanalyzer.ds.QADataExactOnly;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorSample;
import models.queryanalyzer.riep.search.LDSearchIESmart;
import models.queryanalyzer.util.LoadData;

import java.util.Arrays;
import java.io.*;
public class TestQA {
	private BufferedReader reader=null;
	
	public TestQA(){
		reader=new BufferedReader(new InputStreamReader(System.in));
	}
	public void run(String[] args,boolean readfromCommandLine) throws IOException{
		String fileName="";
		// Read from the commandLine
		
		try{
		if(readfromCommandLine){
			assert(args.length > 0);
			fileName = args[0];
			System.out.println("Loading File: "+args[0]);
			System.out.println("");
		}
		// Input the filePath by user
		else{
			System.out.println("Please input file path:");
			fileName = reader.readLine();
			System.out.println("Loading File: "+fileName);
			System.out.println("");
			
		}
		}catch(IOException e){
			System.out.println(e);
		}
		
		QADataExactOnly data = LoadData.LoadIt(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		// Why are we hard code here?
		int advetiser = 3;
		QAInstanceExact instExact = data.buildInstances(advetiser);
		QAInstanceAll inst = instExact.makeQAInstanceAll();
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
		
		
		int[] avgPosOrder = QAInstanceAll.getAvgPosOrder(inst.getAvgPos());
		

		System.out.println("AvgPos order: "+Arrays.toString(avgPosOrder));
		
		System.out.println("passA");

		ImpressionEstimatorSample ie = new ImpressionEstimatorSample(inst);

		System.out.println("passB");

		
		LDSearchIESmart smartIESearcher = new LDSearchIESmart(10, ie);
		smartIESearcher.search(avgPosOrder, inst.getAvgPos());
		IEResult bestSol = smartIESearcher.getBestSolution();

		System.out.println("Smart Iterations: "+smartIESearcher.getIterations());
		System.out.println("Best solution: "+Arrays.toString(bestSol.getSol()));
		
		int[] trueImpressions = inst.getTrueImpressions(data);
		System.out.println("Ground Truth:  "+Arrays.toString(trueImpressions));
		
		
		System.out.println("our Order: "+Arrays.toString(bestSol.getOrder()));
		int[] bidOrder = instExact.getBidOrder(data);
		System.out.println("bid Order: "+Arrays.toString(bidOrder));
		
		System.out.println("Slot impressions: "+Arrays.toString(bestSol.getSlotImpressions()));
	}
	
	public static void main(String[] args) throws Exception {
		new TestQA().run(args,false);
	}
}
