package models.queryanalyzer.greg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;



public class Algo {
	
	public static final int NUM_SLICES = 10;
	private double EPSILON = .02;
	public static final int DECIMAL_PLACES = 3;
	ArrayList<int []> _feasibleOrders;
	public boolean _verbose;
	HashMap<Integer,/*ArrayList<fract>*/char[]> _goodPoints;
	int _roundVal;
	boolean _findFirst;
	boolean _onePerUnique;
	double _timeout;
	
	
	public Algo(){
		_timeout = -1;
		_verbose = false;
		_feasibleOrders = new ArrayList<int []>();
		_findFirst = false;
		_onePerUnique = false;
		_roundVal  = 1;
		
		
		
		for(int i = 0; i < DECIMAL_PLACES; ++i){
			_roundVal *= 10;
		}


		_goodPoints = new HashMap<Integer,/*ArrayList<fract>*/ char[]>();
		//ArrayList<fract> vals1 = new ArrayList<fract>();
		char[] vals1 = new char[NUM_SLICES+1];
		
		for(char i = 0; i <10; ++i){
			//fract newNum = new fract();
			//newNum.numerator = i+1;
			//newNum.denominator = i+1;
			//vals1.add(newNum);
			vals1[i+1] = (char) (i+1);
		}
		_goodPoints.put((int)(0.000*_roundVal), vals1);
				
		//ArrayList<fract> vals2 = new ArrayList<fract>();
		char[] vals2 = new char[NUM_SLICES+1];
		for (char i = 0; i < 5; ++i){
			//fract newNum = new fract();
			//newNum.numerator = i+1;
			//newNum.denominator = (i+1)*2;
			vals2[(i+1)*2] = (char) (i+1);
			//vals2.add(newNum);
		}

		_goodPoints.put((int)(0.500*_roundVal),vals2);

		//ArrayList<fract> vals3 = new ArrayList<fract>();
		char[] vals3 = new char[NUM_SLICES+1];
		for(char i = 0; i < 3; ++i){
			//fract newNum = new fract();
			//newNum.numerator = i+1;
			//newNum.denominator = (i+1)*3;
			//vals3.add(newNum);
			vals3[(i+1)*3] = (char) (i+1);
		}
		_goodPoints.put((int)(.333*_roundVal),vals3);

		//ArrayList<fract> vals3p2 = new ArrayList<fract>();
		char[] vals3p2 = new char[NUM_SLICES+1];
		for(char i = 0; i < 3; ++i){
			//fract newNum = new fract();
			//newNum.numerator = (i+1)*2;
			//newNum.denominator = (i+1)*3;
			//vals3p2.add(newNum);
			vals3p2[(i+1)*3] = (char) ((i+1)*2);
		}
		//System.out.println((int)(.666*_roundVal));
		_goodPoints.put((int)(.666*_roundVal),vals3p2);

		//ArrayList<fract> vals4 = new ArrayList<fract>();
		char[] vals4 = new char[NUM_SLICES+1];
		for(char i = 0; i < 2; ++i){
			//fract newNum = new fract();
			//newNum.numerator = i+1;
			//newNum.denominator = (i+1)*4;
			//vals4.add(newNum);
			vals4[(i+1)*4] = (char) (i+1);
		}

		_goodPoints.put((int)(.250*_roundVal),vals4);

		//ArrayList<fract> vals4pt2 = new ArrayList<fract>();
		char[] vals4pt2 = new char[NUM_SLICES+1];
		for(char i = 0; i < 2; ++i){
			//fract newNum = new fract();
			//newNum.numerator = (i+1)*3;
			//newNum.denominator = (i+1)*4;
			//vals4pt2.add(newNum);
			vals4pt2[(i+1)*4] = (char) ((i+1)*3);
		}

		_goodPoints.put((int)(.750*_roundVal),vals4pt2);

		for(char i = 0; i < 4; ++i){
			//ArrayList<fract> vals5 = new ArrayList<fract>();
			char[] vals5 = new char[NUM_SLICES+1];
			for(char j = 0; j < 2; ++j){
				//fract newNum = new fract();
				//newNum.numerator = (j+1)*(i+1);
				//newNum.denominator = (j+1)*5;
				//vals5.add(newNum);
				vals5[(j+1)*5] = (char) ((j+1)*(i+1));
			}
			//System.out.println((int)(i*.200*_roundVal));
			_goodPoints.put((int)((i+1)*.200*_roundVal),vals5);
		}

		//ArrayList<fract> vals6 = new ArrayList<fract>();
		char[] vals6 = new char[NUM_SLICES+1];
		
		//fract newNum = new fract();
		//newNum.numerator = 1;
		//newNum.denominator = 6;
		//vals6.add(newNum);
		vals6[6] = 1;

		_goodPoints.put((int)(.166*_roundVal),vals6);

		//ArrayList<fract> vals6pt2 = new ArrayList<fract>();
		char[] vals6pt2 = new char[NUM_SLICES+1];
		
		//newNum = new fract();
		//newNum.numerator = 5;
		//newNum.denominator = 6;
		//vals6pt2.add(newNum);
		vals6pt2[6] = 5;
		
		_goodPoints.put((int)(.833*_roundVal),vals6pt2);




		//ArrayList<fract> vals7 = new ArrayList<fract>();
		char[] vals7 = new char[NUM_SLICES +1];
		
		//newNum = new fract();
		//newNum.numerator = 1;
		//newNum.denominator = 7;
		//vals7.add(newNum);
		vals7[7]=1;
				
		_goodPoints.put((int)(0.142*_roundVal),vals7);

		//ArrayList<fract> vals7pt2 = new ArrayList<fract>();
		char[] vals7pt2 = new char[NUM_SLICES +1];
		
		//newNum = new fract();
		//newNum.numerator = 2;
		//newNum.denominator = 7;
		//vals7pt2.add(newNum);
		vals7pt2[7] = 2;

		_goodPoints.put((int)(.285*_roundVal),vals7pt2);

		//ArrayList<fract> vals7pt3 = new ArrayList<fract>();
		char[] vals7pt3 = new char[NUM_SLICES +1];
		
		//newNum = new fract();
		//newNum.numerator = 3;
		//newNum.denominator = 7;
		//vals7pt3.add(newNum);
		vals7pt3[7] = 3;
		
		_goodPoints.put((int)(0.428*_roundVal),vals7pt3);

		//ArrayList<fract> vals7pt4 = new ArrayList<fract>();
		char[] vals7pt4 = new char[NUM_SLICES +1];
		
		//newNum = new fract();
		//newNum.numerator = 4;
		//newNum.denominator = 7;
		//vals7pt4.add(newNum);
		vals7pt4[7] = 4;

		_goodPoints.put((int)(.571*_roundVal),vals7pt4);

		//ArrayList<fract> vals7pt5 = new ArrayList<fract>();
		char[] vals7pt5 = new char[NUM_SLICES +1];
		
		//newNum = new fract();
		//newNum.numerator = 5;
		//newNum.denominator = 7;
		//vals7pt5.add(newNum);
		vals7pt5[7] = 5;

		_goodPoints.put((int)(.714*_roundVal),vals7pt5);

		//ArrayList<fract> vals7pt6 = new ArrayList<fract>();
		char[] vals7pt6 = new char[NUM_SLICES +1];
		
		//newNum = new fract();
		//newNum.numerator = 6;
		//newNum.denominator = 7;
		//vals7pt6.add(newNum);
		vals7pt6[7] = 6;
		
		_goodPoints.put((int)(.857*_roundVal),vals7pt6);

		//ArrayList<fract> vals8 = new ArrayList<fract>();
		char[] vals8 = new char[NUM_SLICES +1];
		//newNum = new fract();
		//newNum.numerator = 1;
		//newNum.denominator = 8;
		//vals8.add(newNum);
		vals8[8] = 1;
		
		_goodPoints.put((int)(.125*_roundVal),vals8);

		//ArrayList<fract> vals8pt2 = new ArrayList<fract>();
		char[] vals8pt2 = new char[NUM_SLICES +1];
		//newNum = new fract();
		//newNum.numerator = 3;
		//newNum.denominator = 8;
		//vals8pt2.add(newNum);
		vals8pt2[8] = 3;
		
		_goodPoints.put((int)(.375*_roundVal),vals8pt2);

		//ArrayList<fract> vals8pt3 = new ArrayList<fract>();
		char[] vals8pt3 = new char[NUM_SLICES +1];
		//newNum = new fract();
		//newNum.numerator = 5;
		//newNum.denominator = 8;
		//vals8pt3.add(newNum);
		vals8pt3[8] = 5;
		
		_goodPoints.put((int)(.625*_roundVal),vals8pt3);

		//ArrayList<fract> vals8pt4 = new ArrayList<fract>();
		char[] vals8pt4 = new char[NUM_SLICES +1];

		//newNum = new fract();
		//newNum.numerator = 7;
		//newNum.denominator = 8;
		//vals8pt4.add(newNum);
		vals8pt4[8] = 7;
		
		_goodPoints.put((int)(.875*_roundVal),vals8pt4);

		for(char i = 0; i < 8; ++i){
			//ArrayList<fract> vals9 = new ArrayList<fract>();
			if(i+1 !=3 && i+1 !=6){
				char[] vals9 = new char[NUM_SLICES +1];		
				
				//newNum = new fract();
				//newNum.numerator = i+1;
				//newNum.denominator = 9;
				//vals9.add(newNum);
				vals9[9] = (char) (i+1);
	
				_goodPoints.put((int)((i+1)*.111*_roundVal),vals9);
			}
		}

		for(char i = 0; i < 5; ++i){
			if(i != 2){
				//ArrayList<fract> vals10 = new ArrayList<fract>();
				char[] vals10 = new char[NUM_SLICES +1];
						
				//newNum = new fract();
				//newNum.numerator = i*2+1;
				//newNum.denominator = 10;
				//vals10.add(newNum);
				vals10[10] = (char) (i*2+1);
				
	
				_goodPoints.put((int)((i*2+1)*.100*_roundVal),vals10);
			}
		}
		
	}
	//private class fract{
	//	int numerator;
	//	int denominator;
	//}
	
	//.00 --------------------------------------> 1 2 3 4 5 6 7 8 9 10
	//.50 --------------------------------------> 2 4 6 8 10
	//(.33||.66) -------------------------------> 3 6 9
	//(.25||.75) -------------------------------> 4 8
	//(.20||.40||.60||.80) ---------------------> 5 10
	//(.16||.83)--------------------------------> 6
	//(.14||.28||.42||.57||.71||.85) -----------> 7
	//(.12||.37||.62||.87) ---------------------> 8
	//(.11||.22||.44||.55||.77||.88) -> 9
	//(.10||.30||.70||.90) ---------------------> 10
	public void adjustAvgPos(double[] avgPos){
		double[] goodVals = {.000,.100,.111,.125,.142,.166,.200,.222,.250,.285,.300,.333,.375,.400,.428,.444,.500,.555,.571,.600,.625,.666,.700,.714,.750,.777,.800,.833,.857,.875,.888,.900,1.000};
		
		double prevDistance = 1000;
		double curDistance = 1000;
		for(int a = 0; a < avgPos.length; ++a){
			double changedPos = avgPos[a] - ((int) avgPos[a]);
			for(int i = 0; i < goodVals.length; ++i){
				
				if(goodVals[i] > changedPos){
					curDistance = goodVals[i] - changedPos;
					if(curDistance < prevDistance){
						//use curdistance
						avgPos[a] = ((int) avgPos[a]) + goodVals[i];
						
					}else{
						//use prevDistance
						avgPos[a] = ((int) avgPos[a]) + goodVals[i-1];
					}
					System.out.println(i);
					System.out.println(avgPos[a]);
					if(i == goodVals.length-1){
						avgPos[a] = avgPos[a]-1;
					}
					
					break;
				}
				prevDistance =  changedPos - goodVals[i];
			}
		}
		
	}
	
	private class AgentPos implements Comparable<Object>{
		public int _index;
	    public double _avgPos;
	    public int _decVals;
	    char[] _possibleFracts;
	    int _highestVal;
	    int _lowestVal;
	    ;
	    
		AgentPos(int index, double avgPos,int digits,HashMap<Integer,/*ArrayList<fract>*/char[]> goodPoints){
			_index = index;
			_avgPos = avgPos;
			int val = 1;
			double onlyDec;
			//System.out.println("avgPos  = " + avgPos);
			//System.out.println("avgPos - Math.floor(avgPos)  = " + (avgPos - Math.floor(avgPos)));
			onlyDec = avgPos - (int)(avgPos);
			//System.out.println("onlyDec  = " + onlyDec);
			for(int i = 0 ;i  < digits; ++i){
				val *= 10;
			}
			//this resets the val
			onlyDec = onlyDec + .00001;
			//System.out.println("val  = " + val);
			_decVals = (int) Math.floor(onlyDec*val);
			//System.out.println("decVals  = " + _decVals);
			_possibleFracts = goodPoints.get(_decVals);
			//_highestVal = _possibleFracts.get(_possibleFracts.size()-1).denominator;
			for(int i = _possibleFracts.length-1;i>=0; --i){
				
				if(_possibleFracts[i] != 0){
					_highestVal = i;
					break;
				}
			}
			//System.out.println(_highestVal + " is the highest val");
			for(int i = 0;i<_possibleFracts.length; ++i){
				
				if(_possibleFracts[i] != 0){
					_lowestVal = i;
					break;
				}
			}
			//System.out.println(_lowestVal + " is the lowestg val");
		}
	    
		@Override
		public int compareTo(Object o) {
			AgentPos that = (AgentPos) o;
			if(this._avgPos < that._avgPos){
				return -1;
			}else if(this._avgPos > that._avgPos){
				return 1;
			}else{
				return 0;
			}
			
		}
		
	
	}
	
	public void setEpsilon(double epsilon){
		EPSILON = epsilon;
	}
	public void swap(AgentPos agents[], int i, int j){
		AgentPos t;
		t = agents[i];
		agents[i] = agents[j];
		agents[j] = t;
	}
	
	public void findFirst(boolean onlyFirst){
		_findFirst = onlyFirst;
	}
	
	public void setTimeout(double timeout){
		_timeout = timeout;
	}
	
	public void isVerbose(boolean verbose){
		_verbose = verbose;
	}
	
	public void onePerUnique(boolean onePerUnique){
		_onePerUnique = onePerUnique;
	}
	
	public void enumerate(AgentPos agents[], int n){
		if(0 == n){
			int[] ordering = new int[agents.length];
			for(int j = 0; j < agents.length; ++j){
				ordering[j] = agents[j]._index;
				//System.out.print(agents[j]._index + " ");
			}
			_feasibleOrders.add(ordering);
			//System.out.println();
		}else{
			for(int i = 0; i < n; ++i){
				
				//big number				small number
				swap(agents, agents.length -1 - i, agents.length -1 -(n-1));
				//swap(agents, i, n-1);
				//the lower value is locked in place
				if(agents[agents.length -1 -(n-1)]._avgPos <= agents.length  -(n-1)){
					enumerate(agents, n-1);
				}else{
					//System.out.println("uhoh");
				}
				swap(agents, agents.length -1 - i, agents.length -1 -(n-1));
				//swap(agents, i, n-1);
			}
		}
	}
	
	public int epsilonCompareson(double a, double b){
		if((a + EPSILON > b) && (a - EPSILON < b)){
			return 0;
		}else if (a > b){
			return 1;
		}else{
			return -1;
		}
	}
	
	public int validatePosition(int[][] totalImpress, int[][] agentPosSum,AgentPos[] agentOrdering ,int ageNum, int impNum){
		int posSum = agentPosSum[ageNum][impNum];
		int numImps = totalImpress[ageNum][impNum];
		
		/*for(int k = 0; k <= impNum; ++k){
			if(agentInGame[ageNum][k] > 0){
				 numImps += agentInGame[ageNum][k];
				 posSum += agentPositions[ageNum][k];
			}
		}*/
		
		
		int futureImpress = NUM_SLICES -1 - impNum;
		double givenAvgPos = agentOrdering[ageNum]._avgPos;
		//double roundedDecimals =  (double)((int) (Math.round((givenAvgPos - Math.floor(givenAvgPos))*_roundVal)))/ (double)_roundVal;
		//System.out.println(agentOrdering[ageNum]._decVals + " is the decval");
		//ArrayList<fract> vals= agentOrdering[ageNum]._possibleFracts;
		
		//System.out.println("Number of vals " + vals.get(vals.size()-1).denominator);
		//double futureAvgPos = (double)(posSum+futureImpress)/(double)(numImps+futureImpress);

		//int futureConst = epsilonCompareson(agentOrdering[ageNum]._avgPos,(double)(posSum+futureImpress)/(double)(numImps+futureImpress));
		int futureConst = epsilonCompareson(givenAvgPos*(numImps+futureImpress),(posSum+futureImpress));
		
		//using vals = 74.
		//backtrack if cannot from now on cannot reach avgPos
		//backtrack if all the impressions is greater than highest val
		//backtrack if the first impression of an agent starts too late
		
		
		//TODO
		if(futureConst < 0||agentOrdering[ageNum]._highestVal<numImps||(NUM_SLICES-agentOrdering[ageNum]._lowestVal>ageNum&&numImps ==0)){
			return -3;
		}
		
	
		
		
		
		//int compConst = epsilonCompareson((double)posSum/(double)numImps,agentOrdering[ageNum]._avgPos);
		int compConst = epsilonCompareson(posSum,givenAvgPos*numImps);
		
		//boolean yes = false;
		//if((((double)posSum/(double)numImps) -agentOrdering[ageNum]._avgPos)<.01/*EPSILON*/ &&(((double)posSum/(double)numImps) -agentOrdering[ageNum]._avgPos)>-.01/*EPSILON*/ ){
			//System.out.println("#################################################################");
		//	yes = true;
		//}
		
		//if(compConst==0 && ){
		//	return -3;
		//}
		
		
		
		if(numImps == 0){
			return -2;
		}
		
		//if((compConst == 0||yes) &&!(compConst == 0&&yes)/*compConst==0&&!yes*/){
		//	System.out.println("agentOrdering[ageNum]._avgPos = " + agentOrdering[ageNum]._avgPos);
		//	System.out.println("(double)posSum/(double)numImps = " + (double)posSum/(double)numImps);
			
		//	System.out.println("posSum = " + posSum);
		//	System.out.println("agentOrdering[ageNum]._avgPos*numImps = " + agentOrdering[ageNum]._avgPos*numImps);
		//	System.out.println("numImps = " + numImps);
		//	System.out.println("dingdingdingdingdingdingdingdingdingdingdingdingdingdingdingding");
		//	System.out.println();
		//}
		
		return compConst;
	}
	
	public ArrayList<OrderingSolution> createImpressions(int agents, int numslots, double[] avgPos){
		//System.out.print("RecieveVals : [");
		//for(int i = 0; i < avgPos.length; ++i){
		//	System.out.print(avgPos[i] + ", ");
		//}
		//System.out.println("]");
		double timeoutStart = System.currentTimeMillis();
		double timeoutEnd = timeoutStart + 1000.0*_timeout;
		ArrayList<OrderingSolution> sols = new ArrayList<OrderingSolution>();
		assert(agents == avgPos.length);
		//sorting the agents
		AgentPos[] myAgents = new AgentPos[avgPos.length];
		AgentPos[] originalOrdering = new AgentPos[avgPos.length];
		boolean feasible = false;
		for(int i = 0;i < agents;++i){
			if(avgPos[i] == 1.0){
				feasible = true;
			}
			
			myAgents[i] =  new AgentPos(i,avgPos[i],DECIMAL_PLACES,_goodPoints);
			originalOrdering[i] = myAgents[i];
		}
		
		
		assert(feasible);
		
		
		Arrays.sort(myAgents);
		enumerate( myAgents, agents);
		if(_verbose){
			System.out.println("There are "+_feasibleOrders.size() + " possible orderings");
		}
		for(int i = _feasibleOrders.size()-1; i >=0 /* i >=_feasibleOrders.size()-1*/; --i){
			long time1 = System.currentTimeMillis();
			if(_verbose){
				System.out.print("Ordering " + (_feasibleOrders.size() - i) + ": ");
				for(int j = 0; j < agents; ++j){
					
					System.out.print (_feasibleOrders.get(i)[j] + " ");
				}
				System.out.print("... ");
			}
			ArrayList<int[][]> waterfalls = new ArrayList<int[][]>();
			int mySolutions = 0;
			
			//if((_feasibleOrders.size() - i)!= 103){
			//	continue;
			//}
			
			
			if(_feasibleOrders.get(i)[0] != 1.0){
				assert(false);
			}
			int[] ordering = _feasibleOrders.get(i);
			AgentPos[] agentOrdering = new AgentPos[agents];
			
			//System.out.print("avgPos ");
			for(int j = 0; j < agents; ++j){	
				agentOrdering[j] = originalOrdering[ordering[j]];
				//System.out.print(agentOrdering[j]._avgPos + ", ");
			}
			//System.out.println();
			//System.out.print("Agents ordering ");
			//for(int j = 0; j < agents; ++j){
			//	System.out.print(agentOrdering[j]._index + " ");				
			//}
			//System.out.println();
			//Try to recreate the orderings
			
			int[][] agentPositions = new int [agents][NUM_SLICES];
			int[][] agentInGame = new int [agents][NUM_SLICES];
			int[][] agentPosSum = new int[agents][NUM_SLICES];
			int[][] totalImpress = new int[agents][NUM_SLICES];
			int[][] satisfiedPos = new int[agents][NUM_SLICES];
			int[][] zeroVals = new int[agents][NUM_SLICES];
		
			int satCount = 0;
			int zeroCount = 0;
			for(int j = 0; j < agents; ++j){
				if(j < numslots){
					agentPositions[j][0] = j+1;
					agentPosSum[j][0] = j+1;
					totalImpress[j][0] = 1;
					agentInGame[j][0] = 1;
					zeroVals[j][0] = 0;
				}else{
					agentPositions[j][0] = 0;
					agentPosSum[j][0] = 0;
					totalImpress[j][0] = 0;
					agentInGame[j][0] = -1;
					++zeroCount;
					zeroVals[j][0] = zeroCount;
				}
				//TODO
				if(validatePosition(totalImpress, agentPosSum, agentOrdering , j,  0)==0){
					satCount ++;
					satisfiedPos[j][0] = satCount;
				}
			}
			
			//so from now on if the previous value at an agent is a 1.. we decide if
			//the agent continues or stops
			boolean backtracking = false;
			int changedImp = 0;
			int prevVal = 0;
			for(int j = agents; /*j < NUM_SLICES*agents*/; ++j){
				if(_timeout!=-1&&timeoutEnd < System.currentTimeMillis()){
					return sols;
				}
				
				boolean was1 = false;
				int ageNum = j%agents;
				int impNum = j/agents;
				
				
				if(j == agents-1){
					break;
				}
				
				
				if(changedImp == impNum -1){
					prevVal = 0;
					//move the previous col
					
					for(int k = 0; k < agents; ++k){
						agentInGame[k][impNum] = agentInGame[k][impNum-1]; 
					}
					changedImp = impNum;
				}
				//System.out.println("DEPTH IS " + j+ " row " + ageNum +" col "+ impNum +" prevVal = " + prevVal);
				if(changedImp == impNum +1){
					for(int k = 0; k < agents; ++k){
						agentInGame[k][changedImp] = 0; 
						agentPositions[k][changedImp] = 0; 
					}
					changedImp = impNum;
					int k;
					for(k = agents-1; k >= 0; --k){
						if(agentPositions[k][impNum]>0){
							
							prevVal = agentPositions[k][impNum];
							//System.out.println("prevVal set to " + prevVal);
							break;
						}	
					}
				}
				
	
				if(backtracking){
					if(agentInGame[ageNum][impNum] == 1){
						was1 = true;
						//System.out.println("backtracked");
						//j%agents
						//j/agents
						//System.out.println("prevRow");
						
						//for(int k = 0; k < agents; ++k){
						//	System.out.println(agentInGame[k][impNum]);
						//}
						int howMany  = 0;
						//int howMany = zeroVals[agents-1][impNum-1] - zeroVals[ageNum-1][impNum-1];
						//for(int k = agents-1 ; k >=ageNum; --k){
						//	if(agentInGame[k][impNum-1] != 0){
						//		howMany ++;
						//	}
						//}
						/*0
						1
						2
			ageNum		3
						ageNum
						5
						6 
			agents -    agents-1*/
						
						int count =	0;
						if(ageNum == 0){
							howMany = agents - zeroVals[agents-1][impNum-1];
							count = numslots;
						}else{
							howMany = 1 + agents - zeroVals[agents-1][impNum-1] - (ageNum  - zeroVals[ageNum-1][impNum-1]);
							count = numslots - (ageNum-zeroVals[ageNum-1][impNum]);
						}
						/*
						count = numslots;
						for(int k = 0 ; k <ageNum; ++k){
							if(agentInGame[k][impNum] == 1){
								count --;
							}
						}*/
						//System.out.println("DEPTH " +j);
						//System.out.println("loc = " + ageNum + " " + impNum);
						//System.out.println("count = "+ count);
						//System.out.println("howMany = " + howMany);
						int k;
						for(k = ageNum+1 ; k <agents; ++k){
							if(agentInGame[k][impNum-1] != 0){
								agentInGame[k][impNum] = 1;
								//System.out.println("k " + k);
								//System.out.println("counting down " + count);
								--count;
								 --howMany;
								
								 
								if(howMany == 0){
									//System.out.println("hit howMany");
									 break;
								 }
								if(count == 0){
									//System.out.println("hit count");
									break;
								}
								
								
								 
								 
							}
							
						}
						k++;
						for(;k < agents; ++k){
							if(agentInGame[k][impNum-1] != 0){
								agentInGame[k][impNum] = -1;
							}
						}
						
						//if(agentInGame[7][impNum] ==1){
						//	return;
						//}
						backtracking = false;
					
						prevVal --;
						agentInGame[ageNum][impNum] = 0;
						agentPositions[ageNum][impNum] = 0;
						
						//System.out.println("checking backtrack");
						 //TODO
						int compConst =  validatePosition(totalImpress, agentPosSum, agentOrdering , ageNum,  impNum-1);
						//System.out.println("");
						//need to check if its still in a bad location
						if(compConst<0){
							backtracking = true;
						}
						if(impNum == NUM_SLICES-1 && compConst != 0){
							backtracking = true;
						}
					
						//System.out.println("curRow");
						
						//int sums = 0;
						//for(int l = 0; l < agents; ++l){
							//System.out.println(agentInGame[l][impNum]);
						//	if(agentInGame[l][impNum] == 1){
						//		sums++;
						//	}
							
						//}
						
						//if(sums == 4){
						//	return null;
						//}
						
						
						//System.out.println("After");
						//for(int l = 0; l < agents; ++l){
						//	System.out.println(agentInGame[l][impNum]);
						//}
						
					
					}
				}else{
					//if(agentInGame[ageNum][impNum]==0){
					//	agentPositions[ageNum][impNum]= 0;
					//}
					
					if(agentInGame[ageNum][impNum]==1){
						was1 = true;
						if(prevVal != agents+1){
							prevVal++;
							//System.out.println("prevval incremented to " + prevVal);
							agentPositions[ageNum][impNum] = prevVal;
							if(prevVal>5){
								System.out.println("FUCK 6");
								return null;
							}
						}else{
							agentPositions[ageNum][impNum] = agentPositions[ageNum][impNum-1];
						}
					}
				}
				
				if(agentInGame[ageNum][impNum]==1){
					agentPosSum[ageNum][impNum] = agentPosSum[ageNum][impNum-1] + agentPositions[ageNum][impNum];
					totalImpress[ageNum][impNum] = totalImpress[ageNum][impNum-1] + 1;
				}else{
					agentPosSum[ageNum][impNum] = agentPosSum[ageNum][impNum-1];
					totalImpress[ageNum][impNum] = totalImpress[ageNum][impNum-1];
				}
				
				
				if(agentInGame[ageNum][impNum]==0){
					if(ageNum==0){
						zeroVals[ageNum][impNum] =1;
					}else{
						zeroVals[ageNum][impNum] = zeroVals[ageNum-1][impNum] + 1;
					}
				}else{
					if(ageNum==0){
						zeroVals[ageNum][impNum] =0;
					}else{
						zeroVals[ageNum][impNum] = zeroVals[ageNum-1][impNum];
					}
				}
				
		
				//int compConst = epsilonCompareson(curAvgPos,agentOrdering[ageNum]._avgPos);
				boolean untouched = true;
				if(!backtracking){
					//System.out.println("check non backtrack");
					//TODO
					int compConst =  validatePosition(totalImpress, agentPosSum, agentOrdering , ageNum,  impNum);;
					//System.out.println("\n\n\n\n");
					if(agentInGame[ageNum][impNum]==1 &&((compConst<0)|| (impNum == NUM_SLICES-1 && compConst != 0))){
						//need to backtrack
						
						backtracking = true;
					}
					if(compConst == 0&& !backtracking){
						//System.out.println("progressing because 1 works");
						untouched = false;
						if(ageNum == 0){
							satisfiedPos[ageNum][impNum]  = 1;
						}else{
							satisfiedPos[ageNum][impNum] = satisfiedPos[ageNum-1][impNum] +1;
						}
					}
					
					if(compConst == -2 && impNum == NUM_SLICES-1){
						backtracking = true;
					}else if(compConst == -2 && impNum != NUM_SLICES-1){
						backtracking = false;
					}
					
				}
				
				if(untouched){
					//System.out.println("using the previous value");
					if(ageNum == 0){
						satisfiedPos[ageNum][impNum]  = 0;
					}else{
						satisfiedPos[ageNum][impNum] = satisfiedPos[ageNum-1][impNum];
					}
				}
				
				if(ageNum == agents-1 && zeroVals[ageNum][impNum] == agents){
					backtracking = true;
				}
				
				
				/*System.out.println("ageNum " + ageNum);
				System.out.println("impNum " + impNum);
				//System.out.println("Solution " + solution + " satisfiedPos[ageNum][impNum] = " + satisfiedPos[ageNum][impNum]);
				
				
					System.out.println("AGENTPOS");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( agentPositions[a][b]+"\t");
							if(agentPositions[a][b] < 0||agentPositions[a][b] > 5){
								//System.out.println("printing 6 bad");
								return null;
							}
						}
						System.out.println();
					}
					System.out.println("INGAME");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( agentInGame[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
					
					System.out.println("POS SUMS");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( agentPosSum[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
					
					System.out.println("TOTAL IMPS");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( totalImpress[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
					
					System.out.println("Satisfied Sums");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( satisfiedPos[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
					
					System.out.println("zeroVals");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( zeroVals[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
					*/
					
				
				
				if(/*ageNum == agents-1&& */!backtracking && satisfiedPos[ageNum][impNum] == agents){
					
					
					//System.out.println(satisfiedPos[ageNum][impNum]);
					//int compConst =0;
					//boolean solution = true;
					//int sum = 0;
					//if( tempSum == 2){
					//for(int x = 0; x < agents; ++x){
					//	compConst =  validatePosition(totalImpress, agentPosSum, agentOrdering , x,  impNum);
						/*if(x == 0){
							for(int y = 0; y < NUM_SLICES; ++y){
								if(agentInGame[0][y] == 1){
									sum ++;
								}
							}
						}*/
					//	if(compConst != 0){
					//		solution = false;
					//		break;
					//	}
					//}
					//System.out.println("impNum " + impNum);
					//System.out.println("Solution " + solution + " satisfiedPos[ageNum][impNum] = " + satisfiedPos[ageNum][impNum]);
					
					//if(satisfiedPos[ageNum][impNum] == 6 ){
						/*System.out.println("AGENTPOS");
						for(int a = 0; a < agents; ++a){
							for(int b = 0; b< NUM_SLICES; ++b){
								
								System.out.print( agentPositions[a][b]+"\t");
								if(agentPositions[a][b] < 0||agentPositions[a][b] > 5){
									//System.out.println("printing 6 bad");
									return null;
								}
							}
							System.out.println();
						}
						System.out.println("INGAME");
						for(int a = 0; a < agents; ++a){
							for(int b = 0; b< NUM_SLICES; ++b){
								
								System.out.print( agentInGame[a][b]+"\t");
							}
							System.out.println();
						}
						System.out.println();
						
						System.out.println("POS SUMS");
						for(int a = 0; a < agents; ++a){
							for(int b = 0; b< NUM_SLICES; ++b){
								
								System.out.print( agentPosSum[a][b]+"\t");
							}
							System.out.println();
						}
						System.out.println();
						
						System.out.println("TOTAL IMPS");
						for(int a = 0; a < agents; ++a){
							for(int b = 0; b< NUM_SLICES; ++b){
								
								System.out.print( totalImpress[a][b]+"\t");
							}
							System.out.println();
						}
						System.out.println();
						
						System.out.println("Satisfied Sums");
						for(int a = 0; a < agents; ++a){
							for(int b = 0; b< NUM_SLICES; ++b){
								
								System.out.print( satisfiedPos[a][b]+"\t");
							}
							System.out.println();
						}
						System.out.println();*/
						
						
					//}
					
					
					
					//if(solution/* sum == 2*/){
					//if(satisfiedPos[ageNum][impNum] != agents){
						//System.out.println("Ive satisfied " + satisfiedPos[ageNum][impNum]);
					//}
					int[][] water = new int[numslots][NUM_SLICES];
					
					//System.out.println("###################SOLUTINONS####################");
					//for(int x = 0; x < agents; ++x){
					//	compConst =  validatePosition(agentInGame, agentPositions, agentOrdering , x,  impNum);
					//	if(compConst != 0){
					//		solution = false;
					//	}
					//}
					//System.out.println("$$$$$$$$$$$$$$$$$$$SOLUTINONS$$$$$$$$$$$$$$$$$$$$");
					
					//System.out.println("DEPTH = " + j + " ordering " + (_feasibleOrders.size() - i));
					//System.out.println("Backtracking = " + backtracking + " prevVal = " + prevVal);
					
					//System.out.println("agentPositions");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							if(agentPositions[a][b] >0 ){
								water[agentPositions[a][b]-1][b] = ordering[a]+1;
							}
							//System.out.print( agentPositions[a][b]+"\t");
							if(agentPositions[a][b] < 0||agentPositions[a][b] > 5){
								//System.out.println("printing 6 bad");
								return null;
							}
						}
						//System.out.println();
					}
					//System.out.println();
					//System.out.println("INGAME");
					/*for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
								
							System.out.print( agentInGame[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
						
					System.out.println("POS SUMS");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
								
							System.out.print( agentPosSum[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();
						
					System.out.println("TOTAL IMPS");
					for(int a = 0; a < agents; ++a){
						for(int b = 0; b< NUM_SLICES; ++b){
							
							System.out.print( totalImpress[a][b]+"\t");
						}
						System.out.println();
					}
					System.out.println();*/
						
					/*boolean copy = true;
					if(mySolutions >0){
						int[][] tempSol = waterfalls.get(waterfalls.size()-1);
						for(int a = 0; a < numslots; ++a){
							for(int b = 0; b< NUM_SLICES; ++b){
								if(water[a][b] != tempSol[a][b]){
									copy = false;
									break;
								}
							}
								
						}
					}*/
						
						
					//if(!copy||mySolutions ==0){
						//if it isnt a copy then add it to the list
					waterfalls.add(water);
					mySolutions++;
					if(_findFirst||_onePerUnique){
						break;
					}
					
					
					//}
				}

				
				/*if(ageNum == agents-1 && zeroVals[ageNum][impNum] == agents&&j != NUM_SLICES*agents-1 ){
					return null;
					
				}*/
				
				
				
				if(j == NUM_SLICES*agents-1){
					backtracking = true;
					
				}
				
				if(backtracking){
					if(was1 == true){
						//prevVal --;
						--j;
						//System.out.println("Backtracked back over a 1 value");
						
					}else{
						j = j-2;
						//satisfiedPos[j%agents][j/agents] = 0;
					
					}

					//wipe current position
					//agentInGame[ageNum][impNum]=0;
					agentPositions[ageNum][impNum]= 0;
					totalImpress[ageNum][impNum]= 0;
					agentPosSum[ageNum][impNum]= 0;
					satisfiedPos[ageNum][impNum] = 0;
					zeroVals[ageNum][impNum] = 0;
					
					//System.out.println("Backtracking");
					
				}
				//update the sums
				//update the impressions
				
			}
			//add everything to the list
			long time2 = System.currentTimeMillis();
			if(_verbose){
				System.out.println(" took " + (double) ((double)(time2-time1)/(double)1000)+ " seconds");
			}
			if(mySolutions >0){
				sols.add(new OrderingSolution(mySolutions, waterfalls,ordering));
				if(_onePerUnique){
					continue;
				}
				if(_findFirst){
					break;
				}
			}
				
			
			
		}		
		return sols;
	}
}
