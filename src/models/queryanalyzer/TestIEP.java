package models.queryanalyzer;

import models.queryanalyzer.ds.QAData;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.iep.ImpressionEstimatorExact;
import models.queryanalyzer.util.LoadData;

import java.util.Arrays;
import java.io.*;
public class TestIEP {
	private BufferedReader reader=null;
	
	public TestIEP(){
		reader=new BufferedReader(new InputStreamReader(System.in));
		
	}
	
	
	public void run(String[] args, boolean readfromCommandLine) throws IOException {
		
		
	try{
		String fileName="";
		// Read from the commandLine
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
			// Load QA data for test
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
			
		}catch(IOException e){
			System.out.println(e);
		}
	}
	
	// Check if we can readin and output the file correctly
	void test() throws IOException{
		BufferedReader r;
		try{
			System.out.println("Input File ");
			String filename=reader.readLine();
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
		boolean readfromCommandLine=false;
		//new TestIEP().test();
		new TestIEP().run(args,readfromCommandLine);
	}
}
