package models.queryanalyzer;

import models.queryanalyzer.ds.QAData;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimatorExact;
import models.queryanalyzer.util.LoadData;

import java.util.Arrays;

public class TestIEP {
	
	public void run(String[] args) {
		assert(args.length > 0);
		String fileName = args[0];
		System.out.println("Loading File: "+args[0]);
		System.out.println("");
		QAData data = LoadData.LoadIt(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		int advetiser = 0;
		QAInstance inst = data.buildInstances(advetiser);
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
		
		int[] bidOrder = inst.getBidOrder(data);
		System.out.println("Bid order: "+Arrays.toString(bidOrder));
		
		ImpressionEstimatorExact IEP = new ImpressionEstimatorExact(inst);
		IEResult bestSol = IEP.search(bidOrder);
		System.out.println("Best solution: "+Arrays.toString(bestSol.getSol()));
		
		int[] trueImpressions = inst.getTrueImpressions(data);
		System.out.println("Ground Truth:  "+Arrays.toString(trueImpressions));
		
		System.out.println("Slot impressions: "+Arrays.toString(bestSol.getSlotImpressions()));
	}
	
	public static void main(String[] args) throws Exception {
		new TestIEP().run(args);
	}
}
