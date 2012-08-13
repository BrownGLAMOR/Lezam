package models.queryanalyzer.ds;


public abstract class AbstractQAData {
	
	public abstract int[] getBidOrder(int[] AgentIds);
	public abstract int[] getTrueImpressions(int[] AgentIds);


   protected static void sortListsDecending(int[] ids, double[] vals) {
      assert (ids.length == vals.length);
      int length = ids.length;

      for (int i = 0; i < length; i++) {
         for (int j = i + 1; j < length; j++) {
            if (vals[i] < vals[j]) {
               double tempVal = vals[i];
               int tempId = ids[i];

               vals[i] = vals[j];
               ids[i] = ids[j];

               vals[j] = tempVal;
               ids[j] = tempId;
            }
         }
      }
   }
   
   
}
