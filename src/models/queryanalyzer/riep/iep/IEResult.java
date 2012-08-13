package models.queryanalyzer.riep.iep;

import models.queryanalyzer.AbstractQueryAnalyzer;

import java.util.Arrays;

public class IEResult {
   private String[] _agentNames; //name of each agent in the result.
   private int[] _order;
   private int[] _sol; //numImps each agent saw
   private int[] _slotImpr; //numImps in each slot
   private int[][] _waterfall;
   private double _obj;
   
   public IEResult(double obj, int[] sol, int[] order, int[] slotImpr, String[] agentNames) {
	 _obj = obj;
      _sol = sol;
      _order = order;
      _slotImpr = slotImpr;
      _waterfall = AbstractQueryAnalyzer.greedyAssign(slotImpr.length,sol.length,order,sol);
      _agentNames = agentNames;
   }

   public IEResult(double obj, int[] sol, int[] order, int[] slotImpr, int[][] waterfall, String[] agentNames) {
      _obj = obj;
      _sol = sol;
      _order = order;
      _slotImpr = slotImpr;
      _waterfall = waterfall;
      _agentNames = agentNames;
   }

   public double getObj() {return _obj;}

   public int[] getSol() {return _sol;}

   public int[] getOrder() {return _order;}
   
   public void setOrder(int[] order) {_order = order;}

   public int[] getSlotImpressions() {return _slotImpr;}

   public int[][] getWaterfall() {return _waterfall;}
   
   public String[] getAgentNames() {return _agentNames;}
      
   public String toString() {
      StringBuffer sb = new StringBuffer();
     // sb.append("Result:\n\tobj=" + _obj + "\n\tsol=" + Arrays.toString(_sol) + "\n\torder=" + Arrays.toString(_order) + "\n\tslotImpr=" + Arrays.toString(_slotImpr));
      sb.append("Result:\n\tobj=" + _obj + "\n\torder=" + Arrays.toString(_order) + "\n\tslotImpr=" + Arrays.toString(_slotImpr) + "\n\tsol=" + Arrays.toString(_sol) + "\n\tnames=" + Arrays.toString(_agentNames)+"\n");
      //sb.append("Result:\tobj=" + _obj + "\tsol=" + Arrays.toString(_sol));
      sb.append("\twaterfall:\n");
      for(int i=0; i < _waterfall.length; i++){
    	  sb.append("\t  R"+i+": "+Arrays.toString(_waterfall[i])+"\n");
      }
      return sb.toString();

   }
}
