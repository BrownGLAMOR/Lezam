package models.queryanalyzer;

import models.queryanalyzer.ds.QAData;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.util.LoadData;

public class TestFileLoading {
	
	public void run(String[] args) {
		assert(args.length > 0);
		System.out.println("Loading File: "+args[0]);
		System.out.println("");
		
		String fileName = args[0];
		QAData data = LoadData.LoadIt(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		int advetiser = 3;
		QAInstance inst = data.buildInstances(advetiser);
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
	}
	
	public static void main(String[] args) throws Exception {
		new TestFileLoading().run(args);
	}
}
