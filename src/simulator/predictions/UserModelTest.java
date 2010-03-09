package simulator.predictions;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import models.usermodel.TacTexAbstractUserModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import simulator.parser.GameStatus;
import simulator.parser.GameStatusHandler;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryType;

public class UserModelTest {

	public ArrayList<String> getGameStrings() {
		String baseFile = "/pro/aa/finals/day-2/server-1/game"; //games 1425-1464
		int min = 1425;
		int max = 1426;

		ArrayList<String> filenames = new ArrayList<String>();
		for(int i = min; i < max; i++) { 
			filenames.add(baseFile + i + ".slg");
		}
		return filenames;
	}

	public void userStatePredictionChallenge(TacTexAbstractUserModel baseModel) throws IOException, ParseException {
		HashMap<String,Double> ourTotErrorCurrMegaMap = new HashMap<String,Double>();
		HashMap<String,Double> ourTotActualCurrMegaMap = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterCurrMegaMap = new HashMap<String,Integer>();
		HashMap<String,Double> ourTotErrorPredMegaMap = new HashMap<String,Double>();
		HashMap<String,Double> ourTotActualPredMegaMap = new HashMap<String,Double>();
		HashMap<String,Integer> ourTotErrorCounterPredMegaMap = new HashMap<String,Integer>();
		ArrayList<String> filenames = getGameStrings();
		for(int fileIdx = 0; fileIdx < filenames.size(); fileIdx++) {
			String filename = filenames.get(fileIdx);
			GameStatusHandler statusHandler = new GameStatusHandler(filename);
			GameStatus status = statusHandler.getGameStatus();

			TacTexAbstractUserModel model = (TacTexAbstractUserModel) baseModel.getCopy();

			double ourTotErrorCurr = 0;
			double ourTotActualCurr = 0;
			int ourTotErrorCounterCurr = 0;

			double ourTotErrorPred = 0;
			double ourTotActualPred = 0;
			int ourTotErrorCounterPred = 0;

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
				for(Product product : status.getRetailCatalog()) {
					HashMap<UserState, Integer> userDist = userDists.get(product);
					HashMap<UserState, Integer> userDistFuture = userDistsFuture.get(product);
					for(UserState state : UserState.values()) {
						double users = userDist.get(state);
						double usersFuture = userDistFuture.get(state);
						double userCurr = model.getCurrentEstimate(product,state);
						double userPred = model.getPrediction(product,state);
						//						System.out.println("(" + product + ", " + state + "): " + users + ", " + userPred);
						userCurr -= users;
						userCurr = userCurr*userCurr;
						ourTotActualCurr += users;
						ourTotErrorCurr += userCurr;
						ourTotErrorCounterCurr++;

						userPred -= usersFuture;
						userPred = userPred*userPred;
						ourTotActualPred += usersFuture;
						ourTotErrorPred += userPred;
						ourTotErrorCounterPred++;
					}
				}

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
				model.updateModel(totalImpressions);
			}

			ourTotErrorCurrMegaMap.put(filename,ourTotErrorCurr);
			ourTotActualCurrMegaMap.put(filename,ourTotActualCurr);
			ourTotErrorCounterCurrMegaMap.put(filename,ourTotErrorCounterCurr);

			ourTotErrorPredMegaMap.put(filename,ourTotErrorPred);
			ourTotActualPredMegaMap.put(filename,ourTotActualPred);
			ourTotErrorCounterPredMegaMap.put(filename,ourTotErrorCounterPred);
		}
		ArrayList<Double> RMSEListCurr = new ArrayList<Double>();
		ArrayList<Double> RMSEListPred = new ArrayList<Double>();
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
		}
		double[] rmseStd = getStdDevAndMean(RMSEListCurr);
		double[] rmseStdPred = getStdDevAndMean(RMSEListPred);
		System.out.println(baseModel + ", " + rmseStd[0] + ", " + rmseStdPred[0]);
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
		TacTexAbstractUserModel userModel = null; //YOUR FILTER HERE
		evaluator.userStatePredictionChallenge(userModel);

		double stop = System.currentTimeMillis();
		double elapsed = stop - start;
		System.out.println("This took " + (elapsed / 1000) + " seconds");
	}

}
