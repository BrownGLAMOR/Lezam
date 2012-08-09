package models.queryanalyzer.riep.search;

import java.util.Arrays;
import java.util.List;

public class LDSPerm implements Comparable<LDSPerm> {
   int _value;
   int[] _perm;
   List<LDSSwap> _swapped;

   public LDSPerm(int value, int[] perm, List<LDSSwap> swapped) {
      _value = value;
      _perm = perm;
      _swapped = swapped;
   }

   public int compareTo(LDSPerm o) {
      if (o._value < _value) {
         return 1;
      }
      if (o._value > _value) {
         return -1;
      }
      return 0;
   }

   
   public int getVal() {
      return _value;
   }
   
   public String toString() {
	   return "(value=" + _value + ", perm=" + Arrays.toString(_perm) + ", swapped=" + _swapped.toString() + ")";
   }
}

