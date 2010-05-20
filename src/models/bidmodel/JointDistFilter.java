package models.bidmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.List;

import edu.umich.eecs.tac.props.QueryType;

public class JointDistFilter {


	int ADJUST_BID_ITERATIONS = 15;
	private ArrayList<String> names;
	private Particle[] particles;
	private double[] probDist;
	Random r = new Random();
	double maxReasonableBid;
	String _ourAdvertiser;
	double[] dist;
	public static final int NUMPARTICLES = 5000;
	
	final static double[] initDistF0 = {0.13665254237288135, 0.0016419491525423728, 0.0022245762711864407, 0.0026483050847457626, 0.005084745762711864, 
	0.03172669491525424, 0.010963983050847458, 0.023146186440677965, 0.027807203389830507, 0.028336864406779662, 
	0.03569915254237288, 0.06324152542372881, 0.06138771186440678, 0.039353813559322035, 0.03093220338983051, 
	0.025052966101694916, 0.029872881355932204, 0.12108050847457627, 0.026906779661016948, 0.02960805084745763, 
	0.02844279661016949, 0.02388771186440678, 0.018114406779661016, 0.013082627118644068, 0.01159957627118644, 
	0.011228813559322034, 0.010328389830508475, 0.008898305084745763, 0.006461864406779661, 0.011387711864406779, 
	0.02478813559322034, 0.005773305084745763, 0.008050847457627118, 0.006673728813559322, 0.0028601694915254237, 
	0.003125, 0.007521186440677966, 0.0044491525423728815, 0.005773305084745763, 0.011970338983050848, 
	0.0034427966101694915, 0.004290254237288136, 0.004502118644067797, 0.0014300847457627119, 0.0032838983050847456, 
	0.0025953389830508473, 0.002436440677966102, 0.0028601694915254237, 0.0032309322033898303, 0.003919491525423729, 
	0.0016419491525423728, 6.885593220338983E-4, 5.296610169491525E-4, 4.766949152542373E-4, 7.415254237288136E-4, 
	5.296610169491525E-4, 0.0016419491525423728, 4.23728813559322E-4, 1.059322033898305E-4, 2.6483050847457627E-4, 
	2.11864406779661E-4, 3.177966101694915E-4, 2.11864406779661E-4, 5.296610169491525E-5, 1.059322033898305E-4, 
	1.059322033898305E-4, 1.5889830508474575E-4, 3.177966101694915E-4, 4.23728813559322E-4, 1.059322033898305E-4, 
	2.11864406779661E-4, 1.5889830508474575E-4, 2.11864406779661E-4, 1.5889830508474575E-4, 5.296610169491525E-5, 
	1.059322033898305E-4, 0.0, 5.296610169491525E-5, 0.0, 5.296610169491525E-5, 
	5.296610169491525E-5, 0.0, 1.059322033898305E-4, 0.0, 0.0, 
	0.0, 0.0, 0.0, 0.0, 0.0, 
	0.0, 0.0, 0.0, 0.0, 0.0, 
	0.0, 0.0, 0.0, 0.0, 0.0};

	final static double[] initDistF1 = {0.04595318607000132, 3.3375784989679856E-4, 8.519608273681437E-4, 7.377805102981862E-4, 0.0012823327917087523, 
	0.008212199727723858, 0.002591014887356726, 0.00652584427561372, 0.004110491414518466, 0.006077906108646963, 
	0.00664880769399675, 0.00570901585349787, 0.008308813842167669, 0.009362785999736507, 0.012726713802643714, 
	0.012559834877695315, 0.013877300074656361, 0.026533749066795485, 0.023143471959949057, 0.02801809318870493, 
	0.0752184796451627, 0.03493039392209389, 0.03480743050371086, 0.03500065873259848, 0.034895261516841594, 
	0.05948794519344781, 0.03651135215844715, 0.03595801677572351, 0.03368319353563743, 0.03267313688463396, 
	0.04346756839840148, 0.033735892143515876, 0.027183698563962935, 0.02372315664661192, 0.01925255807825743, 
	0.018031706995740197, 0.016916253128979843, 0.017794563260287208, 0.018918800228360635, 0.02057002327521848, 
	0.01238417285143384, 0.012612533485573755, 0.01298142374072285, 0.01175178955689254, 0.007140661367528874, 
	0.006833252821571297, 0.0076500812436871455, 0.004391550656536823, 0.005296210091783409, 0.004444249264415265, 
	0.003767950463308594, 0.005638751042993281, 0.003697685652804005, 0.004716525405120548, 0.0031970488779588073, 
	0.003627420842299416, 0.001343814500900268, 0.0013086820956479733, 0.0010100566510034693, 6.587325984805235E-4, 
	5.796846866628607E-4, 5.972508892890079E-4, 7.114312063589654E-4, 6.587325984805235E-4, 0.0020025470993807913, 
	3.776733564621668E-4, 3.688902551490931E-4, 3.601071538360195E-4, 2.7227614070528304E-4, 3.249747485837249E-4, 
	1.7566202626147294E-4, 2.1957753282684115E-4, 2.1079443151376752E-4, 2.1079443151376752E-4, 2.5470993807913574E-4, 
	1.7566202626147294E-4, 2.3714373545298846E-4, 1.9322822888762022E-4, 1.6687892494839928E-4, 2.0201133020069387E-4, 
	1.6687892494839928E-4, 8.783101313073647E-5, 7.904791181766281E-5, 3.513240525229459E-5, 5.269860787844188E-5, 
	5.269860787844188E-5, 4.3915506565368234E-5, 7.904791181766281E-5, 4.3915506565368234E-5, 3.513240525229459E-5, 
	6.148170919151552E-5, 3.513240525229459E-5, 2.634930393922094E-5, 8.783101313073647E-6, 1.7566202626147294E-5, 
	8.783101313073647E-6, 3.513240525229459E-5, 8.783101313073647E-6, 1.7566202626147294E-5, 2.634930393922094E-5};

	final static double[] initDistF2 = {0.06311536752496551, 1.1693444654926448E-5, 2.3386889309852897E-5, 4.852779531794476E-4, 4.151172852498889E-4, 
	0.003923150681727824, 0.001327205968334152, 0.0020638929815945182, 0.001327205968334152, 0.002917514441404149, 
	0.0027245726045978624, 0.0034729530625131552, 0.003940690848710213, 0.004121939240861573, 0.0054140648752309456, 
	0.006144905166163848, 0.007624125915012044, 0.014149068032461002, 0.010921677307701303, 0.012810168619471924, 
	0.07039453682265721, 0.01813653265979092, 0.02215907762108562, 0.02232278584625459, 0.02642133819780631, 
	0.02518767978671157, 0.024930424004303186, 0.028988049299562665, 0.02551509623704951, 0.024146963212423114, 
	0.027771931055450314, 0.026304403751257047, 0.02574896513014804, 0.023942327930961904, 0.024866110058701093, 
	0.02798241305923899, 0.027678383498210904, 0.025053205173179917, 0.022398793236511613, 0.02127037582731121, 
	0.02084941181973386, 0.020311513365607242, 0.02041675436750158, 0.01791435721134732, 0.018797212282794264, 
	0.01686194719240394, 0.016078486400523867, 0.014032133585911738, 0.013891812250052621, 0.019797001800790477, 
	0.011991627493627073, 0.011634977431651816, 0.0121144086625038, 0.011202319979419537, 0.008694076100937814, 
	0.006823124956149583, 0.006191678944783554, 0.008764236768867372, 0.005665473935311864, 0.005308823873336608, 
	0.004256413854393227, 0.006472321616501789, 0.0031396898898477512, 0.003291704670361795, 0.0021749807058163194, 
	0.002788886550199958, 0.002531630767791576, 0.0022276012067634883, 0.0019878855913374963, 0.0030344488879534134, 
	0.0016020019177249234, 0.0013739797469538577, 0.0010991837975630862, 8.536214598096307E-4, 7.893075142075352E-4, 
	7.191468462779766E-4, 7.659206248976823E-4, 5.963656774012489E-4, 8.06847681189925E-4, 6.957599569681237E-4, 
	4.7358450852452114E-4, 5.495918987815431E-4, 4.151172852498889E-4, 4.618910638695947E-4, 2.5725578240838186E-4, 
	1.929418368062864E-4, 4.2681072990481537E-4, 4.677377861970579E-5, 5.2620500947169015E-5, 1.754016698238967E-5, 
	1.754016698238967E-5, 5.2620500947169015E-5, 5.8467223274632243E-5, 5.8467223274632243E-5, 6.431394560209547E-5, 
	2.9233611637316122E-5, 4.092705629224257E-5, 2.9233611637316122E-5, 5.846722327463224E-6, 4.677377861970579E-5};



	
	
	private class Particle {
				
		HashMap<String, Double> bids;		
		
		//Return the ranking represented by this particle for use in recomputeDistribution
		@SuppressWarnings("unchecked")
		public HashMap<String, Integer> getRanksHash() {
			HashMap<String, Integer> ret = new HashMap<String, Integer>();
			ArrayList<Pair<Double, String>> sorted = new ArrayList<Pair<Double, String>>();
			for(String s : bids.keySet()) {
				sorted.add( new Pair<Double, String>(bids.get(s), s));
			}

			Collections.sort(sorted);
			int i = bids.keySet().size();
			for(Pair<Double, String> p : sorted) {			
				ret.put(p.getSecond(), new Integer(i));				
				i--;
			}
			return ret;
		}
		
		@SuppressWarnings("unchecked")
		public ArrayList<Pair<Double, String>> getSortedBids() {		
		/*	if(bids.values().contains(Double.NaN)){
				int i = 4; //noop
			}*/
			ArrayList<Pair<Double, String>> sorted = new ArrayList<Pair<Double, String>>();
			for(String s : bids.keySet()) {
			/*	if(!Double.isNaN(bids.get(s)) && Double.isNaN(new Pair<Double, String>(bids.get(s), s).getFirst())) {
					System.out.println("made NANBID");
				}*/
				sorted.add( new Pair<Double, String>(bids.get(s), s));
			}
			Collections.sort(sorted);	
			
			return sorted;
		}

		
		public Particle(QueryType q) {
			bids = new HashMap<String, Double>();						
				
			for(int i = 0; i < names.size(); i++) {
				
				double d = r.nextDouble();
				double sum = 0;
				double lastIndex = 0;
				for(int j = 0; sum < d; j++) {
					sum += dist[j];
					lastIndex = j;
				}
				bids.put(names.get(i), Math.pow(2, (lastIndex/25.0-2.0))-0.25);
				//System.out.println(bids.get(names.get(i)));
			}
		}
		
	}
	
	public JointDistFilter(Set<String> snames, QueryType q, double maxbid, String ourAdvertiser) {
		r.setSeed(5);
		switch(q) {
		case FOCUS_LEVEL_ZERO:
			dist = initDistF0;
			break;
		case FOCUS_LEVEL_ONE:
			dist = initDistF1;
			break;
		case FOCUS_LEVEL_TWO:
			dist = initDistF2;
			break;
		}
			
		_ourAdvertiser = ourAdvertiser;
		maxReasonableBid = maxbid;
		names = new ArrayList<String>(snames);
		particles = new Particle[NUMPARTICLES];
		probDist = new double[NUMPARTICLES];
		
		for(int i = 0; i < probDist.length; i++) {			
			probDist[i] = 1.0/NUMPARTICLES;
		}
		
		for(int i = 0; i < particles.length; i++) {			
			particles[i] = new Particle(q);
		}
	}
	
	
	private double recomputeParticleLikelihood(Particle p, HashMap<String, Integer> ranks) {
		double ret = 1;
		HashMap<String, Integer> particleRanks = p.getRanksHash();
		for(String s : names) {
			double d = Math.abs(particleRanks.get(s) - ranks.get(s));
			d = d / 7;
			ret = ret * Math.exp(- (d * d) / 4.9);
			//System.out.println("particle rank: " + particleRanks.get(s) + " real rank: " + ranks.get(s));
		}
		//System.out.println("ret: " + ret);
		return ret;
	}
	
	private void recomputeDistribution(HashMap<String, Integer> ranks) {	
		int i = 0;
		double sum = 0;
		for(Particle p : particles) {
			//System.out.println(p.bids.get("Schlemazl"));
			double t = recomputeParticleLikelihood(p, ranks);
			probDist[i] = t;
			sum += t;
			i++;
		}

		for(int j = 0; j < probDist.length; j++) {
			probDist[j] = probDist[j] / sum;						
		}
				
	}
	
	//generate a new generation of particles by choosing random old ones weighted by the probability distribution
	private void resample() {
		Particle[] p = new Particle[NUMPARTICLES];
		int index;
		for(int i = 0; i < NUMPARTICLES; i++) {
			double sum = 0;
			double d = r.nextDouble();
			
			for(index = 0; sum <= d; index++) {								
				sum += probDist[index];
			}	
			p[i] = particles[index - 1]; 
		}
		particles = p;
	}
	

	private double randomInRange(double low, double high) {
		
		if(high < low) {
			System.out.println("bad range: " + low + ", " + high);
			return high;
		}
		
		ArrayList<Double> bidDist = new ArrayList<Double>();
		double startVal = Math.pow(2, (1.0/25.0-2.0))-0.25;
		double aStep = Math.pow(2, (1.0/25.0));
		double maxBid = 3.75;
		int count = 0;
		for(double curKey = startVal; curKey <= maxBid+0.001; curKey = (curKey+0.25)*aStep-0.25){
			bidDist.add(curKey);
			count++;
		}
		
		int low_index = Collections.binarySearch(bidDist, low);		
		if (low_index < 0) {
			low_index = -low_index-1;
		}
	
		int high_index = Collections.binarySearch(bidDist, high);		
		if (high_index < 0) {
			high_index = -high_index-1;
		}
		
		double[] normalized = new double[1 + high_index - low_index];
		
		for(int i = low_index; i <= high_index; i++) {			
			normalized[i -low_index] = dist[i];	
		}
		
		double sum = 0;
		for(int i = 0; i < (high_index - low_index); i++) {
			sum += normalized[i];
		}
		for(int i = 0; i < (high_index - low_index); i++) {
			normalized[i] /= sum;
		}
		
		double d = r.nextDouble();		
		sum = 0;
		int lastIndex = 0;
		for(int j = 0; sum < d && j < normalized.length; j++) {
			sum += normalized[j];
			lastIndex = j;
		}
		return bidDist.get(lastIndex + low_index);
		
	}
	
	private void adjustParticle(Particle p, double ourBid, double cpc, HashMap<String, Integer> ranks) {
		
		double low_bid = .04; //reserve price
		int ourRank = ranks.get(_ourAdvertiser);
		for(int i = 0; i < ADJUST_BID_ITERATIONS; i++) {
			for(String s : ranks.keySet()) {
				int curplayerActualRank = ranks.get(s);
				if(curplayerActualRank == ourRank + 1) {
					if(!Double.isNaN(cpc)) {
						p.bids.put(s, cpc);
					} else {
						p.bids.put(s, (r.nextDouble() * (ourBid - low_bid)) + low_bid);
					}
					
				} /*else if (curplayerActualRank == -1) { //Uncomment in case of emergency
					p.bids.put(s, (r.nextDouble() * (maxReasonableBid - low_bid)) + low_bid) ;
					//System.out.println("WTF SRSLY 2");
				} */else if ( (curplayerActualRank > ourRank + 1 && p.bids.get(s) >= cpc) ||
						(curplayerActualRank < ourRank && p.bids.get(s) <= ourBid) ) {
					ArrayList<Pair<Double, String>> particleBids = p.getSortedBids();
					//System.out.println( curplayerActualRank );
					
					if(curplayerActualRank == 0) {
						//p.bids.put(s, (r.nextDouble() * .5 * (maxReasonableBid - particleBids.get(curplayerActualRank + 1).getFirst())) + particleBids.get(curplayerActualRank + 1).getFirst());							
						if(particleBids.get(curplayerActualRank + 1).getFirst() > maxReasonableBid) { //this guy bid higher than someone who bid higher than max(probable)bid
							p.bids.put(s,  particleBids.get(curplayerActualRank + 1).getFirst() + low_bid);  //so give this guy an even slightly higher bid 
						} else {
							p.bids.put(s, randomInRange(particleBids.get(curplayerActualRank + 1).getFirst(), maxReasonableBid));
						}
					} else if (curplayerActualRank == ranks.size() - 1) { 
						if(particleBids.get(curplayerActualRank - 1).getFirst() < low_bid) { //this guy bid lower than someone who bid below the reserve price
							p.bids.put(s, low_bid); //so set his bid to the reserve price.
						} else {
							p.bids.put(s, randomInRange(low_bid, particleBids.get(curplayerActualRank - 1).getFirst()));
						} 
						/*p.bids.put(s, (r.nextDouble() * 
								(particleBids.get(curplayerActualRank - 1).getFirst() - low_bid)) + low_bid);*/
					} else {
						p.bids.put(s, randomInRange(particleBids.get(curplayerActualRank + 1).getFirst(), particleBids.get(curplayerActualRank - 1).getFirst()));
						/*p.bids.put(s, (r.nextDouble() *
								(particleBids.get(curplayerActualRank - 1).getFirst() - particleBids.get(curplayerActualRank + 1).getFirst())) +
								particleBids.get(curplayerActualRank + 1).getFirst());*/ 
					}					
				} 
			}
		}
	}
	
	private void adjustBids(double ourBid, double cpc, HashMap<String, Integer> ranks) {
		
		for(Particle p : particles) {
			adjustParticle(p, ourBid, cpc, ranks);
		}
		
	}
	
	public void simulateDay(double ourBid, double cpc, HashMap<String, Integer> ranks) {
		
		resample();
		adjustBids(ourBid, cpc, ranks);
		recomputeDistribution(ranks);
		

	}
	
	public double getBid(String player) {
		return getBid(player, false);
	}
	
	private int max(double[] arr) {
		int ret = 0;
		double cur = 0;
		for(int i = 0; i < arr.length; i++) {
			if (arr[i] > cur) {
				ret = i;
				cur = arr[i];
			}
		}
		return ret;
	}
	
	//returns the current prediction for the given player's bid. If max is true, uses the most likely particle. Otherwise, does a weighted average of particles.
	public double getBid(String player, boolean max) {
		
		if(max) {
			return particles[max(probDist)].bids.get(player);
		}

		double ret = 0;
		
		
		for(int i = 0; i < NUMPARTICLES; i++) {
		
			ret += probDist[i] * particles[i].bids.get(player);
				
		}
		
		
		
			return ret;
	}
	
//	public static void main(String[] args) {
//		
//		List name = (List) Arrays.asList("One", "Two", "Three");
//		JointDistFilter jdf = new JointDistFilter(new HashSet((Collection) name), 3.75, "One");
//	
//		HashMap<String, Integer> update = new HashMap<String, Integer>();
//		update.put("One", 1);
//		update.put("Two", 3);
//		update.put("Three", 2);
//		jdf.recomputeDistribution(update);
//		for(double d : jdf.probDist)
//			System.out.print(d + "   ");
//		
//	}
	
	
	
}
