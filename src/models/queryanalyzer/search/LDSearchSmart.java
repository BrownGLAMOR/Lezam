package models.queryanalyzer.search;

import models.queryanalyzer.iep.IEResult;

import java.util.*;

abstract class LDSearchSmart {
   private int _distFactor;
   private int _iterations;
   private PriorityQueue<LDSPerm> _LDSQueue;

   private int _slots;
   IEResult _best;

   public LDSearchSmart(int slots) {
      _LDSQueue = new PriorityQueue<LDSPerm>();
      _iterations = 0;
      _distFactor = 10000;
      _slots = slots;
   }

   public void search(int[] startPerm, double[] avgPos) {
      //System.out.println("Starting LDS Search. startPerm=" + Arrays.toString(startPerm) + ", avgPos=" + Arrays.toString(avgPos));
      _best = null;
      _iterations = 0;
      int Indexs = startPerm.length;
      _LDSQueue.clear();
      _LDSQueue.add(new LDSPerm(0, startPerm, new LinkedList<LDSSwap>()));

      Set<String> alreadySearched = new HashSet<String>();

      while (!_LDSQueue.isEmpty()) {

         LDSPerm perm = _LDSQueue.poll();

         if(!alreadySearched.contains(Arrays.toString(perm._perm))) {

            _iterations += 1;

            alreadySearched.add(Arrays.toString(perm._perm));

            if (evalPerm(perm._perm)) {
               return;
            }

            for (int i1 = 0; i1 < Indexs; i1++) {
               for (int i2 = i1 + 1; i2 < Indexs; i2++) {
                  LDSSwap nextSwap = new LDSSwap(i1, i2);
                  int[] nextPerm = new int[Indexs];
                  for (int j = 0; j < Indexs; j++) {
                     nextPerm[j] = perm._perm[j];
                  }

                  LinkedList<LDSSwap> nextSwapSet = new LinkedList<LDSSwap>();
                  nextSwapSet.addAll(perm._swapped);
                  nextSwapSet.add(nextSwap);

                  int temp = nextPerm[i1];
                  nextPerm[i1] = nextPerm[i2];
                  nextPerm[i2] = temp;

                  if (feasibleOrder(nextPerm, avgPos) &&
                          !alreadySearched.contains(Arrays.toString(nextPerm))) {

                     double[] avgPosClone = (double[]) avgPos.clone();
                     double val = 0.0;
                     for (LDSSwap swap : nextSwapSet) {
                        int idx1 = swap.getFirstIdx();
                        int idx2 = swap.getSecondIdx();
                        val += Math.abs(avgPosClone[idx1] - avgPosClone[idx2]);

                        double tmpAvg = avgPosClone[idx1];
                        avgPosClone[idx1] = avgPosClone[idx2];
                        avgPosClone[idx2] = tmpAvg;
                     }

                     int dsVal = (int) (100 * val);

                     _LDSQueue.add(new LDSPerm(dsVal + _distFactor, nextPerm, nextSwapSet));
                  }
               }
            }
         }
      }
   }

   public int getIterations() {
      return _iterations;
   }

   public IEResult getBestSolution() {
      //assert(_best != null) : "possibly called before search was run";
      return _best;
   }

   private boolean feasibleOrder(int[] order, double[] avgPos) {
      //assert(false);
      for (int i = 0; i < order.length; i++) {
         int startPos = Math.min(i + 1, _slots);
         if (startPos < avgPos[order[i]]) {
            return false;
         }
      }
      return true;
   }

   abstract protected boolean evalPerm(int[] perm);
}
