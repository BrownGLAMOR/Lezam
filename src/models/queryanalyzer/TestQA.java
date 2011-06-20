package models.queryanalyzer;

import models.queryanalyzer.ds.QAData;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.search.LDSearchIESmart;
import models.queryanalyzer.util.LoadData;

import java.util.Arrays;

public class TestQA {
	
	public void run(String[] args) {
		assert(args.length > 0);
		String fileName = args[0];
		System.out.println("Loading File: "+args[0]);
		System.out.println("");
		QAData data = LoadData.LoadIt(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		int advetiser = 3;
		QAInstance inst = data.buildInstances(advetiser);
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
		
		int[] avgPosOrder = inst.getAvgPosOrder();
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
		
		System.out.println("Slot impressions: "+Arrays.toString(bestSol.getSlotImpressions()));
	}
	
	public static void main(String[] args) throws Exception {
		new TestQA().run(args);
	}
}
