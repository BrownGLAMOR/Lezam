package models.queryanalyzer;

import models.queryanalyzer.ds.QADataExactOnly;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.util.LoadData;

public class TestFileLoading {
	
	public void run(String[] args) {
		assert(args.length > 0);
		System.out.println("Loading File: "+args[0]);
		System.out.println("");
		
		String fileName = args[0];
		QADataExactOnly data = LoadData.LoadIt(fileName);
		
		System.out.println("All Data:");
		System.out.println(data);
		
		int advetiser = 3;
		QAInstanceExact inst = data.buildInstances(advetiser);
		
		System.out.println("Instance for "+advetiser+":");
		System.out.println(inst);
	}
	
	public static void main(String[] args) throws Exception {
		new TestFileLoading().run(args);
	}
}
