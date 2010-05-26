package models.queryanalyzer;

import models.queryanalyzer.iep.IEResult;

import java.util.Arrays;

import models.queryanalyzer.search.LDSearchIESmart;
import models.queryanalyzer.util.LoadData;

import models.queryanalyzer.ds.QAData;
import models.queryanalyzer.ds.QAInstance;

public class TestQA {
	
	public void run(String[] args) {
		assert(args.length > 0);
		String fileName = args[0];
		System.out.println("Loading File: "+args[0]);
		System.out.println("");
		QAData data = LoadData.LoadIt(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		int advetiser = 4;
		QAInstance inst = data.buildInstances(advetiser);
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
		
		int[] avgPosOrder = inst.getAvgPosOrder(data);
		System.out.println("AvgPos order: "+Arrays.toString(avgPosOrder));
		
		LDSearchIESmart smartIESearcher = new LDSearchIESmart(10, inst);
		smartIESearcher.search(avgPosOrder, inst.getAvgPos());
		IEResult bestSol = smartIESearcher.getBestSolution();
		
		System.out.println("Smart Iterations: "+smartIESearcher.getIterations());
		System.out.println("Best solution: "+Arrays.toString(bestSol.getSol()));
		
		int[] trueImpressions = inst.getTrueImpressions(data);
		System.out.println("Ground Truth:  "+Arrays.toString(trueImpressions));
		
		
		System.out.println("our Order: "+Arrays.toString(bestSol.getOrder()));
		int[] bidOrder = inst.getBidOrder(data);
		System.out.println("bid Order: "+Arrays.toString(bidOrder));
	}
	
	public static void main(String[] args) throws Exception {
		new TestQA().run(args);
	}
}
