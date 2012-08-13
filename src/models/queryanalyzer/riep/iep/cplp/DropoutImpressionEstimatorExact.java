package models.queryanalyzer.riep.iep.cplp;

import models.queryanalyzer.ds.AbstractQAInstance;
import models.queryanalyzer.ds.QAInstanceAll;
import models.queryanalyzer.ds.QAInstanceExact;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator;
import models.queryanalyzer.riep.iep.IEResult;
import models.queryanalyzer.riep.iep.AbstractImpressionEstimator.ObjectiveGoal;
import models.queryanalyzer.riep.iep.cp.ImpressionEstimatorExact;
import models.queryanalyzer.riep.iep.mip.EricImpressionEstimator;

import java.util.Arrays;



public class DropoutImpressionEstimatorExact extends AbstractDropoutImpressionEstimator {
	protected QAInstanceExact _instanceExact;
	
   public DropoutImpressionEstimatorExact(QAInstanceExact inst) {
	   super(inst);
	   _instanceExact = inst;
   }

   public String getName() { return "IELP_Exact"; }

   public AbstractQAInstance getInstanceExact() {return _instanceExact;}
   
   public IEResult search(int[] order) {
	  QAInstanceExact orderedInst = _instanceExact.reorder(order); 
	  int agentIndex = orderedInst.getAgentIndex();
	  
	  //System.out.println(Arrays.toString(_instanceExact.getAvgPos()));
	  //System.out.println(Arrays.toString(orderedInst.getAvgPos()));
	   
	  int advertisers = orderedInst.getNumAdvetisers();
	  int slots = orderedInst.getNumSlots();
	  
      double[] I_a = new double[advertisers];
      Arrays.fill(I_a, -1);
      
      I_a[agentIndex] = orderedInst.getImpressions();
      
      int[] minDropOut = new int[advertisers];
      int[] maxDropOut = new int[advertisers];
      
      //set default values
      for(int a=0; a < advertisers; a++){
    	  int ceilingSlot = ((int)Math.ceil(orderedInst.getAvgPos()[a]))-1;
    	  int floorSlot = ((int)Math.floor(orderedInst.getAvgPos()[a]))-1;
    	  if(ceilingSlot - floorSlot == 0 && (ceilingSlot == a || floorSlot == slots-1)){
    		  minDropOut[a] = ceilingSlot;
    	  } else {
    		  minDropOut[a] = 0; //0 becouse 0 is the top slot, slots are 0 indexed like agents?  
    	  }
    	  maxDropOut[a] = Math.min(Math.min(a,slots-1),floorSlot);
      }
      
      System.out.println(Arrays.toString(orderedInst.getAvgPos()));
      System.out.println(Arrays.toString(minDropOut));
      System.out.println(Arrays.toString(maxDropOut));
      
      
      AbstractImpressionEstimationLP IELP = new ImpressionEstimationLPExact(orderedInst.getImpressionsUB(), I_a, orderedInst.getAvgPos(), slots);
      if(orderedInst.hasImpressionPrior()){
    	  IELP.setPrior(orderedInst.getAgentImpressionDistributionMean(), orderedInst.getAgentImpressionDistributionStdev());
      }
      
      return search(order, IELP, minDropOut, maxDropOut, agentIndex);
   }

}
