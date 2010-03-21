package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import models.usermodel.BgleibParticleFilter;
import models.usermodel.DavidLParticleFilter;
import models.usermodel.DavidandVinitParticleFilter;
import models.usermodel.GregFilter;
import models.usermodel.JakeParticleFilter;
import models.usermodel.OldUserModel;
import models.usermodel.TacTexAbstractUserModel;
import models.usermodel.jbergParticleFilter;
import models.usermodel.TacTexAbstractUserModel.UserState;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class UserModelTest {

	public ArrayList<String> getGameStrings() {
//		String baseFile = "/Users/jordanberg/Desktop/finalsgames/server1/game";
		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
		int min = 1440;
		int max = 1444;

		ArrayList<String> filenames = new ArrayList<String>();
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public double[] userStatePredictionChallenge(TacTexAbstractUserModel baseModel) throws IOException, ParseException {
		double time = 0;
		HashMap<String,Double> ourTotErrorCurrMegaMap = new HashMap<String,Double>();
		HashMap<String,Double> ourTotActualCurrMegaMap = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterCurrMegaMap = new HashMap<String,Integer>();
		HashMap<String,Double> ourTotErrorPredMegaMap = new HashMap<String,Double>();
		HashMap<String,Double> ourTotActualPredMegaMap = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterPredMegaMap = new HashMap<String,Integer>();
		
		HashMap<String,Double> ourTotErrorCurrMegaMapMAE = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterCurrMegaMapMAE = new HashMap<String,Integer>();
		HashMap<String,Double> ourTotErrorPredMegaMapMAE = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterPredMegaMapMAE = new HashMap<String,Integer>();
		
		HashMap<String,Double> ourTotErrorCurrMegaMapLinf = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterCurrMegaMapLinf = new HashMap<String,Integer>();
		HashMap<String,Double> ourTotErrorPredMegaMapLinf = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterPredMegaMapLinf = new HashMap<String,Integer>();
		
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();
			
			double start = System.currentTimeMillis();
			TacTexAbstractUserModel model = (TacTexAbstractUserModel) baseModel.getCopy();
			double stop = System.currentTimeMillis();
			double elapsed = stop - start;
			time += (elapsed / 1000.0);		
			
			double ourTotErrorCurr = 0;
			double ourTotActualCurr = 0;
			int ourTotErrorCounterCurr = 0;

			double ourTotErrorPred = 0;
			double ourTotActualPred = 0;
			int ourTotErrorCounterPred = 0;
			
			double ourTotErrorCurrMAE = 0;
			int ourTotErrorCounterCurrMAE = 0;

			double ourTotErrorPredMAE = 0;
			int ourTotErrorCounterPredMAE = 0;
			
			double ourTotErrorCurrLinf = 0;
			int ourTotErrorCounterCurrLinf = 0;

			double ourTotErrorPredLinf = 0;
			int ourTotErrorCounterPredLinf = 0;

			LinkedList<HashMap<Product, HashMap<UserState, Integer>>> allUserDists = status.getUserDistributions();

			//Make the query space
			LinkedHashSet<Query> querySpace = new LinkedHashSet<Query>();
			querySpace.add(new Query(null, null));
			for(Product product : status.getRetailCatalog()) {
				// The F1 query classes
				// F1 Manufacturer only
				querySpace.add(new Query(product.getManufacturer(), null));
				// F1 Component only
				querySpace.add(new Query(null, product.getComponent()));

				// The F2 query class
				querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
			}

			//				System.out.println(agents[agent]);
			for(int i = 0; i < 57; i++) {
				HashMap<Product, HashMap<UserState, Integer>> userDists = allUserDists.get(i);
				HashMap<Product, HashMap<UserState, Integer>> userDistsFuture = allUserDists.get(i+2);
				
				HashMap<Query, Integer> totalImpressions = new HashMap<Query,Integer>();
				for(Query q : querySpace) {
					int imps = 0;
					for(Product product : status.getRetailCatalog()) {
						HashMap<UserState, Integer> userDist = userDists.get(product);
						if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
							imps += userDist.get(UserState.F0);
							imps += (1.0/3.0)*userDist.get(UserState.IS);
						}
						else if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
							if(product.getComponent().equals(q.getComponent()) || product.getManufacturer().equals(q.getManufacturer())) {
								imps += (1.0/2.0)*userDist.get(UserState.F1);
								imps += (1.0/6.0)*userDist.get(UserState.IS);
							}
						}
						else {
							if(product.getComponent().equals(q.getComponent()) && product.getManufacturer().equals(q.getManufacturer())) {
								imps += userDist.get(UserState.F2);
								imps += (1.0/3.0)*userDist.get(UserState.IS);
							}
						}
					}
					totalImpressions.put(q, imps);
				}
				start = System.currentTimeMillis();
				model.updateModel(totalImpressions);
				stop = System.currentTimeMillis();
				elapsed = stop - start;
				time += (elapsed / 1000.0);				
				//System.out.print("Current - ");
				//System.out.print("Future - ");
				for(Product product : status.getRetailCatalog()) {
					/*if(product.getManufacturer().equals("lioneer")&&product.getComponent().equals("tv")){
						System.out.print(product + ": ");
					}*/
					HashMap<UserState, Integer> userDist = userDists.get(product);
					HashMap<UserState, Integer> userDistFuture = userDistsFuture.get(product);
					double maxDiffCurr = 0;
					double maxDiffPred = 0;
					for(UserState state : UserState.values()) {
						double users = userDist.get(state);
						double usersFuture = userDistFuture.get(state);
						
						start = System.currentTimeMillis();
						double userCurr = model.getCurrentEstimate(product,state);
						double userPred = model.getPrediction(product,state);
						stop = System.currentTimeMillis();
						elapsed = stop - start;
						time += (elapsed / 1000.0);		
						
						
						//System.out.print("(" + state + ")" + "-" + users + ", " + userCurr+ " \t");
						/*if(product.getManufacturer().equals("lioneer")&&product.getComponent().equals("tv")){
							System.out.print("(" + state + ")" + users + ", " + userCurr+ " -- " + usersFuture + ", " + userPred+ "     \t");
						}*/
						//System.out.println("Future - (" + product + ", " + state + "): " + usersFuture + ", " + userPred);
						
						userCurr -= users;
						if(Math.abs(userCurr) > maxDiffCurr) {
							maxDiffCurr = Math.abs(userCurr);
						}
						ourTotErrorCurrMAE += Math.abs(userCurr);
						ourTotErrorCounterCurrMAE++;
						
						userCurr = userCurr*userCurr;
						ourTotActualCurr += users;
						ourTotErrorCurr += userCurr;
						ourTotErrorCounterCurr++;

						userPred -= usersFuture;
						if(Math.abs(userPred) > maxDiffPred) {
							maxDiffPred = Math.abs(userPred);
						}
						ourTotErrorPredMAE += Math.abs(userPred);
						ourTotErrorCounterPredMAE++;
						
						userPred = userPred*userPred;
						ourTotActualPred += usersFuture;
						ourTotErrorPred += userPred;
						ourTotErrorCounterPred++;
					}
					ourTotErrorCurrLinf += maxDiffCurr;
					ourTotErrorCounterCurrLinf++;

					ourTotErrorPredLinf += maxDiffPred;
					ourTotErrorCounterPredLinf++;
				}
			}

			ourTotErrorCurrMegaMap.put(filename,ourTotErrorCurr);
			ourTotActualCurrMegaMap.put(filename,ourTotActualCurr);
			ourTotErrorCounterCurrMegaMap.put(filename,ourTotErrorCounterCurr);

			ourTotErrorPredMegaMap.put(filename,ourTotErrorPred);
			ourTotActualPredMegaMap.put(filename,ourTotActualPred);
			ourTotErrorCounterPredMegaMap.put(filename,ourTotErrorCounterPred);
			
			ourTotErrorCurrMegaMapMAE.put(filename,ourTotErrorCurrMAE);
			ourTotErrorCounterCurrMegaMapMAE.put(filename,ourTotErrorCounterCurrMAE);

			ourTotErrorPredMegaMapMAE.put(filename,ourTotErrorPredMAE);
			ourTotErrorCounterPredMegaMapMAE.put(filename,ourTotErrorCounterPredMAE);
			
			ourTotErrorCurrMegaMapLinf.put(filename,ourTotErrorCurrLinf);
			ourTotErrorCounterCurrMegaMapLinf.put(filename,ourTotErrorCounterCurrLinf);

			ourTotErrorPredMegaMapLinf.put(filename,ourTotErrorPredLinf);
			ourTotErrorCounterPredMegaMapLinf.put(filename,ourTotErrorCounterPredLinf);
			
		}
		ArrayList<Double> RMSEListCurr = new ArrayList<Double>();
		ArrayList<Double> RMSEListPred = new ArrayList<Double>();
		
		ArrayList<Double> RMSEListCurrMAE = new ArrayList<Double>();
		ArrayList<Double> RMSEListPredMAE = new ArrayList<Double>();
		
		ArrayList<Double> RMSEListCurrLinf = new ArrayList<Double>();
		ArrayList<Double> RMSEListPredLinf = new ArrayList<Double>();
		for(String file : filenames) {
			double totError = ourTotErrorCurrMegaMap.get(file);
			int totErrorCounter = ourTotErrorCounterCurrMegaMap.get(file);
			double MSE = (totError/totErrorCounter);
			double RMSE = Math.sqrt(MSE);
			RMSEListCurr.add(RMSE);

			double totErrorPred = ourTotErrorPredMegaMap.get(file);
			int totErrorCounterPred = ourTotErrorCounterPredMegaMap.get(file);
			double MSEPred = (totErrorPred/totErrorCounterPred);
			double RMSEPred = Math.sqrt(MSEPred);
			RMSEListPred.add(RMSEPred);
			
			
			double totErrorMAE = ourTotErrorCurrMegaMapMAE.get(file);
			int totErrorCounterMAE = ourTotErrorCounterCurrMegaMapMAE.get(file);
			double MSEMAE = (totErrorMAE/totErrorCounterMAE);
			double RMSEMAE = MSEMAE;
			RMSEListCurrMAE.add(RMSEMAE);

			double totErrorPredMAE = ourTotErrorPredMegaMapMAE.get(file);
			int totErrorCounterPredMAE = ourTotErrorCounterPredMegaMapMAE.get(file);
			double MSEPredMAE = (totErrorPredMAE/totErrorCounterPredMAE);
			double RMSEPredMAE = MSEPredMAE;
			RMSEListPredMAE.add(RMSEPredMAE);
			
			double totErrorLinf = ourTotErrorCurrMegaMapLinf.get(file);
			int totErrorCounterLinf = ourTotErrorCounterCurrMegaMapLinf.get(file);
			double MSELinf = (totErrorLinf/totErrorCounterLinf);
			double RMSELinf = MSELinf;
			RMSEListCurrLinf.add(RMSELinf);

			double totErrorPredLinf = ourTotErrorPredMegaMapLinf.get(file);
			int totErrorCounterPredLinf = ourTotErrorCounterPredMegaMapLinf.get(file);
			double MSEPredLinf = (totErrorPredLinf/totErrorCounterPredLinf);
			double RMSEPredLinf = MSEPredLinf;
			RMSEListPredLinf.add(RMSEPredLinf);
		}
		double[] rmseStd = getStdDevAndMean(RMSEListCurr);
		double[] rmseStdPred = getStdDevAndMean(RMSEListPred);
		
		double[] rmseStdMAE = getStdDevAndMean(RMSEListCurrMAE);
		double[] rmseStdPredMAE = getStdDevAndMean(RMSEListPredMAE);
		
		double[] rmseStdLinf = getStdDevAndMean(RMSEListCurrLinf);
		double[] rmseStdPredLinf = getStdDevAndMean(RMSEListPredLinf);
		
		time /= (57.0*filenames.size());
		
		System.out.println(baseModel + ", \nL2: " + rmseStd[0] + ", " + rmseStdPred[0] +", \nL1: "+ rmseStdMAE[0] + ", " + rmseStdPredMAE[0]
		                   + ", \nLinf: "+ rmseStdLinf[0] + ", " + rmseStdPredLinf[0] +"\ntime: " + time);
		double[] output = new double[4]; //time,L1,L2,Linf
		output[0] = time;
		output[1] = rmseStd[0];
		output[2] = rmseStdMAE[0];
		output[3] = rmseStdLinf[0];
		
		return rmseStdPredLinf;
	}

	private double[] getStdDevAndMean(ArrayList<Double> list) {
		double n = list.size();
		double sum = 0.0;
		for(Double data : list) {
			sum += data;
		}
		double mean = sum/n;

		double variance = 0.0;

		for(Double data : list) {
			variance += (data-mean)*(data-mean);
		}

		variance /= (n-1);

		double[] stdDev = new double[2];
		stdDev[0] = mean;
		stdDev[1] = Math.sqrt(variance);
		return stdDev;
	}

	public static void main(String[] args) throws IOException, ParseException  {
		UserModelTest evaluator = new UserModelTest();

		double start = System.currentTimeMillis();
		evaluator.userStatePredictionChallenge(new jbergParticleFilter());
		evaluator.userStatePredictionChallenge(new DavidandVinitParticleFilter());
		evaluator.userStatePredictionChallenge(new BgleibParticleFilter());
		evaluator.userStatePredictionChallenge(new DavidLParticleFilter());
		evaluator.userStatePredictionChallenge(new JakeParticleFilter());
		evaluator.userStatePredictionChallenge(new GregFilter());
		evaluator.userStatePredictionChallenge(new OldUserModel());
		
		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

}
