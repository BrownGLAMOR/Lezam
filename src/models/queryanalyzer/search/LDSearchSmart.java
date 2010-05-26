package models.queryanalyzer.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;

abstract class LDSearchSmart {
	private int _distFactor;
	private int _iterations;
	private PriorityQueue<LDSPerm> _LDSQueue;
	
	public LDSearchSmart(){
		_LDSQueue = new PriorityQueue<LDSPerm>();
		_iterations = 0;
		_distFactor = 10000;
	}
	
	public void search(int[] startPerm, double[] avgPos){
		_iterations = 0;
		int Indexs = startPerm.length;
		_LDSQueue.clear();
		_LDSQueue.add(new LDSPerm(0, startPerm, new HashSet<LDSSwap>()));

		while(!_LDSQueue.isEmpty()){
			_iterations += 1;
			LDSPerm perm = _LDSQueue.poll();
			int dsVal = perm.getVal();
			
			if(evalPerm(perm._perm)){
				return;
			}

			for(int i1=0; i1 < Indexs; i1++){
				for(int i2=i1+1; i2 < Indexs; i2++){
					LDSSwap nextSwap = new LDSSwap(i1,i2);
					if(!perm._swapped.contains(nextSwap)){
						int[] nextPerm = new int[Indexs]; 
						for(int j=0; j < Indexs; j++){
							nextPerm[j] = perm._perm[j];
						}
						
						HashSet<LDSSwap> nextSwapSet = new HashSet<LDSSwap>();
						nextSwapSet.addAll(perm._swapped);
						nextSwapSet.add(new LDSSwap(i1,i2));
						
						int temp = nextPerm[i1];
						nextPerm[i1] = nextPerm[i2];
						nextPerm[i2] = temp;
						
						int delta = (int)(100*Math.abs(avgPos[nextPerm[i1]] - avgPos[nextPerm[i2]])); //needs a little more work, becouse avp array changes with swaps
						//System.out.println("Pushing: "+Arrays.toString(nextPerm)+" "+nextSwapSet);
						_LDSQueue.add(new LDSPerm(dsVal+_distFactor+delta, nextPerm, nextSwapSet));
					}
				}
			}
		}
	}
	
	public int getIterations(){return _iterations;}
	
	abstract protected boolean evalPerm(int[] perm);
}
