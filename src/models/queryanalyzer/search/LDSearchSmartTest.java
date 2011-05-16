package models.queryanalyzer.search;

import java.util.Arrays;

public class LDSearchSmartTest extends LDSearchSmart {

   private final static int NUM_SLOTS = 5;
   private int[] _correctOrder;
   private int _iters;
   private String orderStr;

	public LDSearchSmartTest(int[] correctOrder){
		super(NUM_SLOTS);
      _correctOrder = correctOrder.clone();
      _iters = 0;
      orderStr = Arrays.toString(_correctOrder);
	}
	
	@Override
	protected boolean evalPerm(int[] perm) {
		if(Arrays.toString(perm).equals(orderStr)) {
         System.out.println(_iters);
			return true;
      }
		else {
         _iters++;
			return false;
      }
	}

}
