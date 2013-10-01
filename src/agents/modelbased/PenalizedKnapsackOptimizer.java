package agents.modelbased;

import clojure.lang.PersistentHashMap;
import edu.umich.eecs.tac.props.Query;

public abstract class PenalizedKnapsackOptimizer extends SchlemazlAgent{

	double _lambda = .996;
	boolean DEBUG = true;
	
	public PenalizedKnapsackOptimizer(PersistentHashMap perfectSim, String agentToReplace){
		super(perfectSim, agentToReplace);
	}


	protected double calculateConversionProb(Query q, double kappa, double solutionWeight, double ISRatio) {
		
		QueryAuction qa = new QueryAuction(q,0);
		return calculateConversionProb(qa, kappa, solutionWeight, ISRatio);
	}

	protected double calculateConversionProb(QueryAuction qa, double kappa, double solutionWeight, double ISRatio) {
		String component = qa.getQuery().getComponent();
		double remainingCapacity = _capacity-kappa;
		//debug("SolW: "+solutionWeight);
		double penalty = calculatePenalty(remainingCapacity, solutionWeight);
		double baseProb = _baseConvProbs.get(qa.getQuery()); //get from init bidder
		//double baseProb = .36;
		//debug("Penalty line 29: "+penalty);
		double nonSpecialtyConvPr =  baseProb*penalty;
		double specialtyConvPr = eta(baseProb*penalty,1+_CSB);

		double conversionProb = 0;

		//handle the 3 cases: our specialty, maybe our specialty, not our specialty
		//_compSpecialty = "C"; //REMOVE THIS
		if(_compSpecialty.equals(component)) {
			conversionProb = specialtyConvPr;

		}
		else if(component == null) {
			//If query has no component, searchers who click will have some chance (1/3) of having our component specialty.
			conversionProb =  specialtyConvPr* (1/3.0) +nonSpecialtyConvPr*(2/3.0);//HC num
		}
		else {
			conversionProb = nonSpecialtyConvPr;
			//debug("NonSpecialty: "+nonSpecialtyConvPr);
			
		}

		//We just computed the conversion probability for someone, assuming they're not an IS user.
		//If an IS user, conversion probability is 0.
		// conversionProb = (PrIS) * 0 + (1 - PrIS) * conversionProbGivenNonIS
		conversionProb *= (1.0 - ISRatio);
		//debug("conv probability: "+conversionProb);
		return conversionProb;

	}
	/**
	 *
	 * calculatePenalty picks which way to calculate the penalty
	 * @param remainingCapacity: for the current window before we are penalized 
	 * @param solutionWeight: what we will take today
	 * 
	 * pass in solutionWeight 0 to calculate the penalty we face at the beginning of the day
	 * 
	 */
	private double calculatePenalty(double remainingCapacity, double solutionWeight) {
		double penalty = 1;
		//debug("remCap: "+remainingCapacity+" solW: "+solutionWeight);
		if(solutionWeight ==0){
			penalty = getPenaltyAtStartOfDay(remainingCapacity);

		}else{
			penalty = getPenaltyForKnownCapacity(remainingCapacity, solutionWeight);
		}

		return penalty;
	}
	/**
	 * get the penalty faced if we know the number of conversions we will have at the end of the day
	 * 
	 * @param remainingCapacity
	 * @param solutionWeight
	 * @return
	 */
	private double getPenaltyForKnownCapacity(double remainingCapacity,
			double solutionWeight) {
		double penalty = 1;

		solutionWeight = Math.max(0,Math.ceil(solutionWeight));
		double penWeight = 0.0;

		if(remainingCapacity < 0) {
			//Average penalty per click:
			//For each conversion, compute its penalty. Use this to get conversion probability at that penalty,
			//and then use this to get expected number of clicks at that penalty.
			//There is a different penalty for each conversion. Average penalty is:
			// (\sum_{conversion} penaltyForConversion * expectedClicksAtPenalty) / totalClicks
			// where, for a given conversion (and thus penalty),  expectedClicksAtPenalty = 1 / PrConv(penalty)
			// and totalClicks = \sum_{conversion} expectedClicksAtPenalty
			// i.e. (ignoring component specialties),
			//    avgPenalty = \sum{c} I_c * ( 1/(pi*I_c) )

			//FIXME: We're currently making the simplifying assumption that PrConv(penalty) = penalty.
			//So the summation becomes \sum_{conversion} penaltyForConversion (1/penaltyForConversion) = #conversions
			//And totalClicks = \sum_{conversion} 1/penaltyForConversion
			//This is probably not affecting things much, but we should use the correct formula at some point.

			penWeight = 0.0;
			int convs = 0;
			for(double j = Math.abs(remainingCapacity)+1; j <= Math.abs(remainingCapacity)+solutionWeight; j++) {
				//debug("j: "+ j);
				penWeight += Math.pow(_lambda, j);//HC num
				convs++;
			}
			//debug("1 pen W: "+penWeight+ " convs: "+convs);
			penalty =  penWeight/((double) convs);
			//debug("PenW1: "+penWeight);
		} else {

			if(solutionWeight > remainingCapacity) {
				//FIXME: Same as above.
				penWeight = remainingCapacity;
				double convs = solutionWeight;
				for(int j = 1; j <= solutionWeight-remainingCapacity; j++) {
					//debug("lambda "+_lambda+" j "+j);
					penWeight += Math.pow(_lambda, j);//HC num
				}
				//debug("2 pen W: "+penWeight+ " convs: "+convs);
				penalty = penWeight/convs;
				//debug("PenW2: "+penWeight);
			}
			else {
				penalty = 1.0;//HC num
			}
		}
		if(Double.isNaN(penalty)) {
			
			System.out.println("ERROR penalty NaN 2 pw:"+penWeight); //ap
			debug("remCap: "+remainingCapacity+" solW: "+solutionWeight);
			penalty = 1.0;//HC num
		}
		return penalty;
	}



	/**
	 * get the penalty we currently face assuming that we get no more conversions
	 * @param remainingCap
	 * @return
	 */
	protected double getPenaltyAtStartOfDay(double remainingCap) {
		double penalty = 1;
		//if we have gone over, calculate penalty
		if(remainingCap < 0) {
			penalty = Math.pow(_lambda, Math.abs(remainingCap));
			//debug("Penalty: "+ penalty+" lambda "+_lambda +" Cap REM: "+ Math.abs(remainingCap));
			if(Double.isNaN(penalty)) {
				System.out.println("ERROR penalty NaN 1"); //ap
				penalty = 1.0;
			}
		}
		//debug("penalty: "+penalty);
		return penalty;
	}



	protected double eta(double p, double x) {
		return (p * x) / (p * x + (1 - p));
	}
	
	private void debug(String output){
		if(DEBUG){
			System.out.println(output);
		}
	}
}
