package models.queryanalyzer;

import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QADataExactOnly;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.ds.QAInstanceSampled;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.cplp.DropoutImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.mip.ImpressionEstimatorSimpleMIPExact;
import models.queryanalyzer.util.LoadData;

import java.util.Arrays;
import java.io.*;
public class TestIEPSampledNew {
	
	public TestIEPSampledNew(){}
	
	public void run(String[] args) throws IOException {
		String fileName="";
		
		if(args.length > 0){ // Read from the commandLine
			assert(args.length > 0);
			fileName = args[0];
			System.out.println("Loading File: "+args[0]);
			System.out.println("");
		} else { // Read from the console input
			BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please input file path:");
			fileName = reader.readLine();
			System.out.println("Loading File: "+fileName);
			System.out.println("");
		}
			// Load QA data for test
			QADataAll data = LoadData.LoadIt(fileName);
			
			System.out.println("All Data:");
			System.out.println(data);
			
			int advetiser = 2;
			int precision = 2;
			QAInstanceSampled inst = data.buildSampledInstance(advetiser, precision);
			
			System.out.println("Instance for "+advetiser+":");
			System.out.println(inst);
			
			int[] bidOrder = data.getBidOrder(inst.getAgentIds());
			System.out.println("Bid order: "+Arrays.toString(bidOrder));
			

			//AbstractImpressionEstimator IEP = new ImpressionEstimatorExact(inst);
			//AbstractImpressionEstimator IEP = new DropoutImpressionEstimatorExact(inst);
			AbstractImpressionEstimator IEP = new ImpressionEstimatorSimpleMIPSampled(inst);

			IEResult bestSol = IEP.search(bidOrder);

			System.out.println("Best solution: "+Arrays.toString(bestSol.getSol()));

			int[] trueImpressions = data.getTrueImpressions(inst.getAgentIds());
			System.out.println("Ground Truth:  "+Arrays.toString(trueImpressions));
			
			System.out.println("Slot impressions: "+Arrays.toString(bestSol.getSlotImpressions()));
		
			System.out.println(bestSol);
	}
	
	// Check if we can readin and output the file correctly
	void test() throws IOException{
		BufferedReader r;
		try{
			System.out.println("Input File ");
			String filename=new BufferedReader(new InputStreamReader(System.in)).readLine();
			System.out.println("Read From File "+filename);
			r= new BufferedReader(new FileReader(filename));
			String line="";
			line=r.readLine();
	
			while(line!=null){
				System.out.println(line);	
				line=r.readLine();
				
			}
		}catch(IOException e){
			System.out.println(e);
		}
		
		
	}
	public static void main(String[] args) throws Exception {
		//new TestIEP().test();
		new TestIEPSampledNew().run(args);
	}
}
