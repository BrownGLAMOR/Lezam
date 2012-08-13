package models.queryanalyzer;

import models.queryanalyzer.ds.QADataExactOnly;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.riep.search.LDSearchOrder;
import models.queryanalyzer.riep.search.LDSearchOrderSmart;
import models.queryanalyzer.util.LoadData;

import java.util.Arrays;

public class TestLDSearch {
	
	public void run(String[] args) {
		assert(args.length > 0);
		String fileName = args[0];
		System.out.println("Loading File: "+args[0]);
		System.out.println("");
		QADataExactOnly data = LoadData.LoadItExactOnly(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		int advetiser = 3;
		
		QAInstanceExact instExact = data.buildInstances(advetiser);
		QAInstanceAll inst = instExact.makeQAInstanceAll();
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
		
		int[] avgPosOrder = QAInstanceAll.getAvgPosOrder(inst.getAvgPos());
		System.out.println("AvgPos order: "+Arrays.toString(avgPosOrder));
		
		int[] carletonOrder = QAInstanceAll.getCarletonOrder(inst.getAvgPos(),inst.getNumSlots());
		System.out.println("Carleton order: "+Arrays.toString(carletonOrder));
		
		int[] bidOrder = data.getBidOrder(instExact.getAgentIds());
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
