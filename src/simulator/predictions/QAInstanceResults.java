package simulator.predictions;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Set;

import models.queryanalyzer.*;

import models.queryanalyzer.QAAlgorithmEvaluator.SolverType;
import models.queryanalyzer.ds.QADataAll;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.ds.QAInstanceSampled;
import models.queryanalyzer.util.LoadData;

public class QAInstanceResults {
	
	private QAInstanceAll inst;
	private QADataAll data;
	private QAInstanceExact instExact;
	private HashMap<SolverType, ResultValues> results = new HashMap<SolverType, ResultValues>();
	private PriorityQueue<ResultValues> sortedResults;
	private String filename;
	private QAInstanceSampled instSamp;
	
	public QAInstanceResults(String filename){
		this.filename = filename;
		data = LoadData.LoadIt(filename);
		if(data!=null){
			//System.out.println("OurAG NUM2: "+data.getOurAgentNum());
			inst = data.buildInstances(data.getOurAgentNum());
			//System.out.println("OurAG ID: "+data.getOurAgentId());
			//System.out.println("OurAG ID: "+data.getOtherAgentNum("Schlemazl"));
			if(inst!=null){
				instExact = data.buildExactInstance(data.getOurAgentNum());
				instSamp = data.buildSampledInstance(data.getOurAgentNum(), 8);
			}else{
				instExact = null;
				instSamp = null;
			}
		}else{
			inst = null;
			instExact = null;
			instSamp = null;
		}
		
		
	}
	
	public QAInstanceResults(String filename, String agent){
		this.filename = filename;
		
		data = LoadData.LoadIt(filename);
		if(data!=null){
			int newNum = data.getOtherAgentNum(agent);
			System.out.println("__________________________________________________________NEW NUM: "+newNum);
			inst = data.buildInstances(newNum);
			if(inst!=null){
				instExact = data.buildExactInstance(newNum);
				instSamp = data.buildSampledInstance(newNum, 8);
			}else{
				instExact = null;
				instSamp = null;
			}
		}else{
			inst = null;
			instExact = null;
			instSamp = null;
		}
		
		
		
	}
	
	
	public void setResult(SolverType solver, int[] predictedImpsPerAgent, 
			int[][] predictedWaterfall, int predictedTotImpr, double objective, double time ){
		
		ResultValues res = new ResultValues(solver, predictedImpsPerAgent, predictedWaterfall,
				predictedTotImpr,objective, time);
		
		getResults().put(solver, res);
		
		
	}
	
	public void makeSortedResults(String stat){
		ResultsComparator comp = new ResultsComparator(stat);
		sortedResults = new PriorityQueue<ResultValues>(3, comp);
		Set<SolverType> keys = results.keySet(); 
		   for(SolverType s: keys){
			   sortedResults.add(results.get(s));
		   }
	}
	
	public QAInstanceAll getQAInstAll(){
		return inst;
	}
	
	public QAInstanceExact getQAInstExact(){
		return instExact;
	}
	
	
	public QADataAll getQADataAll(){
		return data;
	}
	
	
	public double getTime(SolverType solver){
		return getResults().get(solver).time;
	}
	
	public String getFilename(){
		return filename;
	}
	
	public void setResults(HashMap<SolverType, ResultValues> results) {
		this.results = results;
	}

	public HashMap<SolverType, ResultValues> getResults() {
		return results;
	}

	

	public QAInstanceSampled getQAInstSampled() {
		// TODO Auto-generated method stub
		return this.instSamp;
	}

	public PriorityQueue<ResultValues> getSorted() {
		// TODO Auto-generated method stub
		return this.sortedResults;
	}

	public void setSorted(PriorityQueue<ResultValues> newSorted) {
		this.sortedResults = newSorted;
		
	}


//	public void setResult(SolverType solver, int[] predictedImpsPerAgent,
//			int[] reducedImps, double stat, double absError,
//			double secondsElapsed) {
//		// TODO Auto-generated method stub
//		
//	}
	

	public int[] calcAgentImpsPerSlotMAE(SolverType solver){
		//get predicted from this
		int[][] trueImps = data.greedyAssign(inst.getAgentIds());
		int[][] predictedImps = results.get(solver).getPredictedWaterfall();
		//use data to get true
		int absDiff = 0;
		int count = 0;
		for(int a = 0; a<trueImps.length;a++){
			int sumRow = 0;
			for (int s =trueImps[a].length-1;s>=0;s--){
				//System.out.println("a "+a+" s "+s);
				sumRow+=trueImps[a][s];
				if(sumRow>0){
					//System.out.println("T " +trueImps[a][s]+" P:"+ predictedImps[a][s]);
					//System.out.println("AIPS Err true: "+trueImps[a][s]+" predicted: "+predictedImps[a][s]);
					absDiff+=Math.abs(trueImps[a][s]- predictedImps[a][s]);
					count+=1.0;
				}
			}
		}
		//System.out.println("absDiff AIPS: "+absDiff);
		int[] temp = {absDiff, count};
		return temp;
	}
	
	

	public double[] calcAgentImpsMAE(SolverType solver){
		int[] predicted = results.get(solver).getPredictedImpsPerAgent();
		int[] trueVals = data.getTrueImpressions(inst.getAgentIds());
		double absError = 0.0;
		boolean failed = false;
		for(int s = 0; s<predicted.length; s++){
			if(predicted[s]<=0){
				failed = true;
				System.out.println("FAILED!!!!");
			}
			//System.out.println("AI Err true: "+trueVals[s]+" predicted: "+predicted[s]);
			absError+=Math.abs(trueVals[s]-predicted[s]);
		}
		//System.out.println("absDiff AI: "+absError);
		double[] ans = {absError, predicted.length};
		return ans;
	}

	public double[] calcTotalImpsMAE(SolverType solver){
		int predicted = results.get(solver).getTotalImpressions();
		int trueVal = getMaxImps(inst.getNumSlots(), inst.getNumAdvetisers(), 
				data.getBidOrder(inst.getAgentIds()), data.getTrueImpressions(inst.getAgentIds()));
		double absError = 0.0;
		boolean failed = false;
		if(predicted<0){
			failed = true;
			System.out.println("FAILED!!!!");
		}
		//if(predicted>0){
		absError=Math.abs(trueVal-predicted);
//		}else{
//			if(trueVal==0){
//				absError =0;
//			}else{
//				absError = 1;
//			}
//		}
		//System.out.println("TIERROR: "+absError+" true: "+trueVal+" predicted: "+predicted);
		double[] ans = {absError, 1};
		return ans;
	
	}
	public ResultValues getResult(SolverType solver) { 
		return results.get(solver);
	}

	public int getMaxImps(int slots, int agents, int[] order, int[] impressions) {
	      int[][] impressionsBySlot = new int[agents][slots];

	      int[] slotStart = new int[slots];
	      int a;

	      for (int i = 0; i < agents; ++i) {
	         a = order[i];
	         //System.out.println(a);
	         int remainingImp = impressions[a];
	         //System.out.println("remaining impressions "+ impressions[a]);
	         for (int s = Math.min(i + 1, slots) - 1; s >= 0; --s) {
	            if (s == 0) {
	               impressionsBySlot[a][0] = remainingImp;
	               slotStart[0] += remainingImp;
	            } else {

	               int r = slotStart[s - 1] - slotStart[s];
	               //System.out.println("agent " +a + " r = "+(slotStart[s-1] - slotStart[s]));
	               assert (r >= 0);
	               if (r < remainingImp) {
	                  remainingImp -= r;
	                  impressionsBySlot[a][s] = r;
	                  slotStart[s] += r;
	               } else {
	                  impressionsBySlot[a][s] = remainingImp;
	                  slotStart[s] += remainingImp;
	                  break;
	               }
	            }
	         }
	      }

	      int totImps = 0;
	      for(int i = 0; i < impressionsBySlot.length; i++) {
	         totImps += impressionsBySlot[i][0];
	      }

	      return totImps;
	   }


	public boolean containsAgent(String agent) {
		return data.isAgentIn(agent);
	}

//	public double getStat(SolverType solver) {
//		// TODO Auto-generated method stub
//		return 0;
//	}

	

}
