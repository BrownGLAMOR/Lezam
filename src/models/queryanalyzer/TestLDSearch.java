package models.queryanalyzer;

import java.util.Arrays;

import models.queryanalyzer.search.LDSearchOrder;
import models.queryanalyzer.search.LDSearchOrderSmart;
import models.queryanalyzer.util.LoadData;

import models.queryanalyzer.ds.QAData;
import models.queryanalyzer.ds.QAInstance;

public class TestLDSearch {
	
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
		
		int[] carletonOrder = inst.getCarletonOrder();
		System.out.println("Carleton order: "+Arrays.toString(carletonOrder));
		
		int[] bidOrder = inst.getBidOrder(data);
		System.out.println("Bid order: "+Arrays.toString(bidOrder));
		
		LDSearchOrder searcher = new LDSearchOrder(bidOrder);
		searcher.search(carletonOrder);
		
		System.out.println("Iterations: "+searcher.getIterations());
		
		LDSearchOrderSmart smartSearcher = new LDSearchOrderSmart(bidOrder, inst.getNumSlots());
		smartSearcher.search(carletonOrder, inst.getAvgPos());
		
		System.out.println("Smart Iterations: "+smartSearcher.getIterations());
	}
	
	public static void main(String[] args) throws Exception {
		new TestLDSearch().run(args);
	}
}
