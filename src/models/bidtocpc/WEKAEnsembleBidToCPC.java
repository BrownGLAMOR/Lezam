package models.bidtocpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import models.AbstractModel;
import models.targeting.BasicTargetModel;

import org.jfree.base.modules.DefaultModuleInfo;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class WEKAEnsembleBidToCPC extends AbstractBidToCPC {

	private int DEBUG = 0;

	protected Set<Query> _querySpace;
	private ArrayList<BidBundle> _bidBundles;

	/*
	 * Models
	 * 
	 */
	protected HashMap<String,AbstractBidToCPC> _models;
	protected HashMap<String,AbstractBidToCPC> _usableModels;
	protected HashMap<String,HashMap<Query,LinkedList<Double>>> _dailyModelError;
	protected HashMap<String,HashMap<Query,LinkedList<Integer>>> _bordaCount;
	protected HashMap<String,Integer> _totalBordaCount;
	protected HashMap<String,LinkedList<Double>> _errorDifference;
	protected LinkedList<String> _modelList;
	protected HashMap<BidBundle,HashMap<String,HashMap<Query,Double>>> _modelPredictions;

	/*
	 * This holds the ensemble used for predicting
	 */
	protected HashMap<Query,LinkedList<AbstractBidToCPC>> _ensemble;
	protected HashMap<Query,LinkedList<Double>> _ensembleError;
	protected LinkedList<Double> _RMSE;
	protected HashMap<BidBundle,HashMap<Query,Double>> _ensemblePredictions;
	protected HashMap<Query,HashMap<String,Integer>> _ensembleMembers;
	protected HashMap<Query,HashMap<String,Double>> _ensembleWeights;

	/*
	 * Used to keep track of how often each model is used...
	 */
	protected HashMap<Query,HashMap<String, Integer>> _modelUseCount;
	protected int _totUseCount;

	/*
	 * Constants for making ensemble
	 */
	protected int NUMPASTDAYS = 5;
	protected int ENSEMBLESIZE = 25;
	private boolean _borda;
	private boolean _ignoreNaN;

	private RConnection _rConnection;

	public WEKAEnsembleBidToCPC(Set<Query> querySpace, int numPastDays, int ensembleSize, boolean borda, boolean ignoreNaN) {
		_bidBundles = new ArrayList<BidBundle>();

		_querySpace = querySpace;
		NUMPASTDAYS = numPastDays;
		ENSEMBLESIZE = ensembleSize;

		_borda = borda;
		_ignoreNaN = ignoreNaN;

		_RMSE = new LinkedList<Double>();

		_modelList = new LinkedList<String>();

		/*
		 * Initialize Models
		 */
		_models = new HashMap<String, AbstractBidToCPC>();
		_usableModels = new HashMap<String,AbstractBidToCPC>();
		_dailyModelError = new HashMap<String, HashMap<Query,LinkedList<Double>>>();
		_bordaCount = new HashMap<String, HashMap<Query,LinkedList<Integer>>>();
		_totalBordaCount = new HashMap<String, Integer>();
		_errorDifference = new HashMap<String, LinkedList<Double>>();
		_modelPredictions = new HashMap<BidBundle, HashMap<String,HashMap<Query,Double>>>();

		_modelUseCount = new HashMap<Query,HashMap<String, Integer>>();
		for(Query query : _querySpace) {
			HashMap<String, Integer> queryModelUse = new HashMap<String, Integer>();
			_modelUseCount.put(query, queryModelUse);
		}
		_totUseCount = 0;

		/*
		 * Initialize Ensemble
		 */
		_ensemble = new HashMap<Query, LinkedList<AbstractBidToCPC>>();
		_ensembleError = new HashMap<Query, LinkedList<Double>>();
		for(Query query : _querySpace) {
			LinkedList<AbstractBidToCPC> queryEnsemble = new LinkedList<AbstractBidToCPC>();
			LinkedList<Double> queryEnsembleError = new LinkedList<Double>();
			_ensemble.put(query, queryEnsemble);
			_ensembleError.put(query, queryEnsembleError);
		}
		_ensemblePredictions = new HashMap<BidBundle, HashMap<Query,Double>>();

		_ensembleMembers = new HashMap<Query, HashMap<String,Integer>>();
		for(Query query : _querySpace) {
			HashMap<String,Integer> ensembleMember = new HashMap<String, Integer>();
			_ensembleMembers.put(query, ensembleMember);
		}

		_ensembleWeights = new HashMap<Query, HashMap<String,Double>>();
		for(Query query : _querySpace) {
			HashMap<String,Double> ensembleWeights = new HashMap<String, Double>();
			_ensembleWeights.put(query, ensembleWeights);
		}

		try {
			_rConnection = new RConnection();
		} catch (RserveException e) {
			e.printStackTrace();
		}

		initializeEnsemble();
	}

	public void initializeEnsemble() {
		/*
		 * Add Models
		 */
		for(double weight = 1.0; weight > .89; weight -= .1) {
			for(int i = 1; i < 8; i++) {
				addModel(new WEKABidToCPC(i,weight));
			}
		}

		for(int i = 1; i < 20; i++) {
			addModel(new WEKAClassifBidToCPC(i));
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport,SalesReport salesReport, BidBundle bidBundle) {
		_bidBundles.add(bidBundle);

		if(_ensemble != null) {
			updatePredictions(bidBundle);
		}

		boolean ensembleUsable = false;

		/*
		 * Update Models
		 */
		_usableModels = new HashMap<String, AbstractBidToCPC>();
		for(String name : _models.keySet()) {
			AbstractBidToCPC model = _models.get(name);
			boolean usable = model.updateModel(queryReport, salesReport, bidBundle);
			if(usable) {
				_usableModels.put(name, model);
				ensembleUsable = true;
			}
		}

		double totmodels = _models.size();
		double workingmodels = _usableModels.size();
		//		System.out.println("Percent Usable [CPC]: " + (workingmodels/totmodels) + ", total: " + totmodels + ", working: " + workingmodels);

		updateEnsemble(queryReport, salesReport, bidBundle);

		return ensembleUsable;
	}

	public void updatePredictions(BidBundle bundle) {

		/*
		 * Update Predictions
		 */
		HashMap<String,HashMap<Query,Double>> modelPredictions = new HashMap<String, HashMap<Query,Double>>();
		for(String name : _usableModels.keySet()) {
			AbstractBidToCPC model = _usableModels.get(name);
			HashMap<Query,Double> predictions = new HashMap<Query, Double>();
			for(Query query : _querySpace) {
				predictions.put(query, model.getPrediction(query, bundle.getBid(query)));
			}
			modelPredictions.put(name, predictions);
		}
		_modelPredictions.put(bundle, modelPredictions);


		/*
		 * Update Ensemble Predictions
		 */
		HashMap<Query,Double> ensemblePredictions = new HashMap<Query, Double>();
		for(Query query : _querySpace) {
			double prediction = getPrediction(query, bundle.getBid(query));
			ensemblePredictions.put(query, prediction);
		}
		_ensemblePredictions.put(bundle, ensemblePredictions);
	}


	public void updateEnsemble(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		boolean ensembleReady = updateError(queryReport, salesReport, bundle);
		if(ensembleReady) {
			if(_borda) {
				_ensemble = bordaCountCombiner();
			}
			else {
				_ensemble = meanPredictionCombiner();
			}
			for(Query query : _querySpace) {
				double bid = bundle.getBid(query);
				Ad ad = bundle.getAd(query);
				if(DEBUG > 0) {
					System.out.println(query + ", pred: " + getPrediction(query, bid));
				}
				LinkedList<AbstractBidToCPC> queryEnsemble = _ensemble.get(query);
				for(AbstractBidToCPC model : queryEnsemble) {
					String modelName = model.toString();
					double weight = _ensembleWeights.get(query).get(modelName);
					LinkedList<Double> dailyModelError = _dailyModelError.get(modelName).get(query);
					String pastError = ", Past Error: (";

					int bound = NUMPASTDAYS;
					if(dailyModelError.size() < NUMPASTDAYS) {
						bound = dailyModelError.size();
					}

					for(int i = 0; i < bound; i++) {
						double error = dailyModelError.get(dailyModelError.size() - bound + i);
						pastError += error  + ", ";
					}
					pastError = pastError.substring(0, pastError.length()-2);
					pastError += ")";
					//					System.out.println("\t" + modelName);
					if(DEBUG > 0) {
						System.out.println("\t\t Weight: " + weight + ", pred: " + model.getPrediction(query, bid) + pastError);
					}
				}
			}
		}
		else {
			_ensemble = null;
		}
	}

	private boolean updateError(QueryReport queryReport, SalesReport salesReport, BidBundle bundle) {
		/*
		 * Update Error
		 */
		HashMap<String, HashMap<Query, Double>> modelPredictions = _modelPredictions.get(bundle);
		if(modelPredictions == null) {
			//			System.out.println("No Predictions Yet");
			return false;
		}
		else {
			HashMap<Query, Double> ensemblePredictions = _ensemblePredictions.get(bundle);
			for(String name : _models.keySet()) {
				LinkedList<Double> errorDiffList = _errorDifference.get(name);
				HashMap<Query, LinkedList<Double>> dailyModelError = _dailyModelError.get(name);
				HashMap<Query, Double> predictions = modelPredictions.get(name);
				for(Query query : _querySpace) {
					LinkedList<Double> queryDailyError = dailyModelError.get(query);
					double ensemblepred = ensemblePredictions.get(query);
					if(predictions != null) {
						double error = predictions.get(query);
						double CPC = queryReport.getCPC(query);
						if(Double.isNaN(CPC)) {
							CPC = 0.0;
						}

						double errorDiff = 0.0;
						double ensembleerror = ensemblepred-CPC;
						if(Double.isNaN(error)) {
							error = bundle.getBid(query);
						}
						double modelDiff = error-CPC;
						errorDiff = Math.abs(modelDiff-ensembleerror);
						errorDiffList.add(errorDiff);

						if(!(_ignoreNaN && CPC == 0.0)) {
							error -= CPC;
							error = error*error;
							queryDailyError.add(error);
						}
					}
					else {
						if(!(_ignoreNaN && Double.isNaN(queryReport.getCPC(query)))) {
							queryDailyError.add(Double.NaN);
						}
					}
					dailyModelError.put(query, queryDailyError);
				}
				_errorDifference.put(name, errorDiffList);
				_dailyModelError.put(name, dailyModelError);
			}

			/*
			 * Update Ensemble Error
			 */
			double MSE = 0.0;
			for(Query query : _querySpace) {
				LinkedList<Double> queryEnsembleError = _ensembleError.get(query);
				double error = ensemblePredictions.get(query);
				//				System.out.println(query + " prediction: " + error);
				double CPC = queryReport.getCPC(query);
				if(Double.isNaN(CPC)) {
					CPC = 0.0;
				}
				error -= CPC;
				error = error*error;
				MSE += error;
				queryEnsembleError.add(error);
				//				System.out.println(query + " error: " + error);
				_ensembleError.put(query, queryEnsembleError);
			}
			MSE /= _querySpace.size();
			double RMSE = Math.sqrt(MSE);
			_RMSE.add(RMSE);
			double totRMSE = 0.0;
			for(int i = 0; i < _RMSE.size(); i++) {
				totRMSE += _RMSE.get(i);
			}
			double avgRMSE = totRMSE/_RMSE.size();
			//			System.out.println("Bid-CPC (Today,Avg) RMSE: " + RMSE + ", " + avgRMSE);
			return true;
		}
	}

	@Override
	public double getPrediction(Query query, double bid) {
		double prediction = 0.0;
		if(bid == 0 || Double.isNaN(bid)) {
			return prediction;
		}
		if(_ensemble == null) {
			return bid;
		}
		LinkedList<AbstractBidToCPC> queryEnsemble = _ensemble.get(query);
		if(queryEnsemble.size() == 0) {
			return bid;
		}
		double totWeight = 0.0;
		HashMap<String, Double> ensembleWeights = _ensembleWeights.get(query);
		for(AbstractBidToCPC model : queryEnsemble) {
			double pred = model.getPrediction(query, bid);
			double weight = ensembleWeights.get(model.toString());
			//						System.out.println(query + ", bid: " + bid + ", pred: " + pred);
			if(!(Double.isNaN(pred) || Double.isNaN(weight))) {
				prediction += pred*weight;
				totWeight += weight;
			}
		}
		prediction /= totWeight;
		if(prediction <= 0) {
			return 0.0;
		}
		if(prediction > bid || Double.isNaN(prediction)) {
			return bid;
		}
		//		System.out.println("Overall Prediction: " + prediction);
		return prediction;
	}

	@Override
	public String toString() {
		return "WEKAEnsembleBidToCPC(" + "numPastDays: " + NUMPASTDAYS + ", ensemble size: " + ENSEMBLESIZE + ", borda count: " + _borda + ", ignoreNaN: " + _ignoreNaN + ")";
	}

	public boolean intToBin(int x) {
		if(x == 0) {
			return false;
		}
		else if(x == 1) {
			return true;
		}
		else {
			throw new RuntimeException("intToBin can only be called with 0 or 1");
		}
	}

	public void addModel(AbstractBidToCPC model) {
		addModel(model.toString(),model);
	}

	public void addModel(String name, AbstractBidToCPC model) {
		_models.put(name,model);
		HashMap<Query, LinkedList<Double>> dailyModelError = new HashMap<Query,LinkedList<Double>>();
		HashMap<Query, LinkedList<Integer>> bordaCount = new HashMap<Query, LinkedList<Integer>>();
		LinkedList<Double> errorDifference = new LinkedList<Double>();
		for(Query query : _querySpace) {
			LinkedList<Double> dailyModelErrorList = new LinkedList<Double>();
			dailyModelError.put(query,dailyModelErrorList);

			LinkedList<Integer> bordaCountList = new LinkedList<Integer>();
			bordaCount.put(query, bordaCountList);
		}
		_dailyModelError.put(name,dailyModelError);
		_bordaCount.put(name,bordaCount);
		_totalBordaCount.put(name,0);
		_errorDifference.put(name, errorDifference);
		_modelList.add(name);
	}

	public HashMap<Query, LinkedList<AbstractBidToCPC>> meanPredictionCombiner() {
		/*
		 * Initialize Error Mappings
		 */
		HashMap<Query,ArrayList<ModelErrorPair>> modelErrorMap = new HashMap<Query, ArrayList<ModelErrorPair>>();
		for(Query query : _querySpace) {
			ArrayList<ModelErrorPair> modelErrorPairList = new ArrayList<ModelErrorPair>();
			modelErrorMap.put(query, modelErrorPairList);
		}

		/*
		 * Add Models
		 */
		for(String name : _usableModels.keySet()) {
			AbstractBidToCPC model = _usableModels.get(name);
			HashMap<Query, LinkedList<Double>> typeIDailyModelError = _dailyModelError.get(name);
			for(Query query : _querySpace) {
				ArrayList<ModelErrorPair> modelErrorPairList = modelErrorMap.get(query);
				double error = 0;
				LinkedList<Double> dailyModelError = typeIDailyModelError.get(query);
				double bound = NUMPASTDAYS;
				if(dailyModelError.size() < NUMPASTDAYS) {
					bound = dailyModelError.size();
				}
				for(int i = 0; i < bound; i++) {
					error += dailyModelError.get(dailyModelError.size() - 1 - i);
				}
				error/= bound;
				if(!Double.isNaN(error)) {
					ModelErrorPair modelErrorPair = new ModelErrorPair(name,model,error);
					modelErrorPairList.add(modelErrorPair);
					Collections.sort(modelErrorPairList);
					while(modelErrorPairList.size() > ENSEMBLESIZE) {
						modelErrorPairList.remove(modelErrorPairList.size()-1);
					}
				}
				modelErrorMap.put(query, modelErrorPairList);
			}
		}

		/*
		 * Initialize Ensemble
		 */
		HashMap<Query, LinkedList<AbstractBidToCPC>> ensemble = new HashMap<Query, LinkedList<AbstractBidToCPC>>();
		_ensembleWeights = new HashMap<Query, HashMap<String,Double>>();
		for(Query query: _querySpace) {
			LinkedList<AbstractBidToCPC> queryEnsemble = new LinkedList<AbstractBidToCPC>();
			ArrayList<ModelErrorPair> modelErrorPairList = modelErrorMap.get(query);
			HashMap<String,Double> ensembleWeights = new HashMap<String, Double>();
			for(ModelErrorPair modelErrorPair : modelErrorPairList) {
				//				System.out.println("Query: " + query +"  Name: " + modelErrorPair.getName() + "  Error: " + modelErrorPair.getError());
				queryEnsemble.add((AbstractBidToCPC) modelErrorPair.getModel());
				ensembleWeights.put(modelErrorPair.getName(), 1.0);
				HashMap<String, Integer> ensembleMembers = _ensembleMembers.get(query);
				Integer ensembleUseCount = ensembleMembers.get(modelErrorPair.getName());
				if(ensembleUseCount == null) {
					ensembleMembers.put(modelErrorPair.getName(), 1);
				}
				else {
					ensembleMembers.put(modelErrorPair.getName(), ensembleUseCount+1);
				}
			}
			ensemble.put(query, queryEnsemble);
			_ensembleWeights.put(query, ensembleWeights);

			//			System.out.println(query);
			//			for(ModelErrorPair modelErrorPair : modelErrorPairList) {
			//				System.out.println("\t" + modelErrorPair);
			//			}
		}
		return ensemble;
	}

	public HashMap<Query, LinkedList<AbstractBidToCPC>> bordaCountCombiner() {
		/*
		 * Initialize Error Mappings
		 */
		HashMap<Query,ArrayList<ModelErrorPair>> modelErrorMap = new HashMap<Query, ArrayList<ModelErrorPair>>();
		for(Query query : _querySpace) {
			ArrayList<ModelErrorPair> modelErrorPairList = new ArrayList<ModelErrorPair>();
			modelErrorMap.put(query, modelErrorPairList);
		}

		/*
		 * Sort Model Errors
		 */
		for(String name : _usableModels.keySet()) {
			AbstractBidToCPC model = _usableModels.get(name);
			HashMap<Query, LinkedList<Double>> typeIDailyModelError = _dailyModelError.get(name);
			for(Query query : _querySpace) {
				ArrayList<ModelErrorPair> modelErrorPairList = modelErrorMap.get(query);
				LinkedList<Double> dailyModelError = typeIDailyModelError.get(query);
				if(dailyModelError.size() == 0) {
					continue;
				}
				double error = dailyModelError.get(dailyModelError.size() - 1);
				if(!Double.isNaN(error)) {
					ModelErrorPair modelErrorPair = new ModelErrorPair(name,model,error);
					modelErrorPairList.add(modelErrorPair);
					Collections.sort(modelErrorPairList);
					while(modelErrorPairList.size() > ENSEMBLESIZE) {
						modelErrorPairList.remove(modelErrorPairList.size()-1);
					}
				}
				modelErrorMap.put(query, modelErrorPairList);
			}
		}


		/*
		 * Update the Borda Count
		 */

		int len = 0;
		for(Query query : _querySpace) {
			ArrayList<ModelErrorPair> modelErrorPairList = modelErrorMap.get(query);
			for(int i = 0; i < modelErrorPairList.size(); i++) {
				ModelErrorPair mep = modelErrorPairList.get(i);
				HashMap<Query, LinkedList<Integer>> bordaCountMap = _bordaCount.get(mep.getName());
				LinkedList<Integer> bordaCountList = bordaCountMap.get(query);
				bordaCountList.add(ENSEMBLESIZE-i);

				_totalBordaCount.put(mep.getName(),_totalBordaCount.get(mep.getName()) + ENSEMBLESIZE-i);

				if(len < bordaCountList.size()) {
					len = bordaCountList.size();
				}
				bordaCountMap.put(query, bordaCountList);
				_bordaCount.put(mep.getName(), bordaCountMap);
			}
		}

		/*
		 * For those models that weren't in the top ENSEMBLESIZE have a borda count of 0
		 */
		for(String modelName : _bordaCount.keySet()) {
			HashMap<Query, LinkedList<Integer>> bordaCount = _bordaCount.get(modelName);
			for(Query query : _querySpace) {
				LinkedList<Integer> bordaList = bordaCount.get(query);
				if(bordaList.size() == len-1) {
					bordaList.add(0);
					bordaCount.put(query, bordaList);
				}
				else if(bordaList.size() != len) {
					while(bordaList.size() < len) {
						bordaList.add(0);
					}
					bordaCount.put(query, bordaList);
				}
				while(bordaList.size() > NUMPASTDAYS) {
					bordaList.remove(0);
					bordaCount.put(query, bordaList);
				}
			}
			_bordaCount.put(modelName, bordaCount);
		}

		/*
		 * Initialize Ensemble and Weights
		 */
		HashMap<Query, LinkedList<AbstractBidToCPC>> ensemble = new HashMap<Query, LinkedList<AbstractBidToCPC>>();
		_ensembleWeights = new HashMap<Query, HashMap<String,Double>>();
		for(Query query: _querySpace) {
			LinkedList<AbstractBidToCPC> queryEnsemble = new LinkedList<AbstractBidToCPC>();
			HashMap<String,Double> ensembleWeights = new HashMap<String, Double>();

			ArrayList<ModelBordaPair> modelBordaList = new ArrayList<ModelBordaPair>();
			for(String modelName : _bordaCount.keySet()) {
				HashMap<Query, LinkedList<Integer>> bordaCount = _bordaCount.get(modelName);
				LinkedList<Integer> bordaCountList = bordaCount.get(query);
				int bound = NUMPASTDAYS;
				if(bound > bordaCountList.size()) {
					bound = bordaCountList.size();
				}

				double weight = 0;
				for(int i = 0; i < bound; i++) {
					weight += bordaCountList.get(bordaCountList.size()-1-i);
				}
				modelBordaList.add(new ModelBordaPair(modelName, _models.get(modelName),weight));
			}
			Collections.sort(modelBordaList);

			//			System.out.println(query);
			//			for(ModelBordaPair modelBordaPair : modelBordaList) {
			//				System.out.println("\t" + modelBordaPair);
			//			}

			int i = 0;
			for(ModelBordaPair modelBordaPair : modelBordaList) {
				if(i < ENSEMBLESIZE) {
					queryEnsemble.add((AbstractBidToCPC) modelBordaPair.getModel());
					ensembleWeights.put(modelBordaPair.getName(), modelBordaPair.getCount());
				}
				i++;
			}

			ensemble.put(query, queryEnsemble);
			_ensembleWeights.put(query, ensembleWeights);
		}
		return ensemble;
	}

	public HashMap<Query, HashMap<String, Integer>> getEnsembleMembers() {
		return _ensembleMembers;
	}

	public class ModelErrorPair implements Comparable<ModelErrorPair> {

		private String _name;
		private AbstractModel _model;
		private double _error;

		public ModelErrorPair(String name, AbstractModel model, double error) {
			_name = name;
			_model = model;
			_error = error;
		}

		public String getName() {
			return _name;
		}

		public void setName(String name) {
			_name = name;
		}

		public AbstractModel getModel() {
			return _model;
		}

		public void setModel(AbstractModel model) {
			_model = model;
		}

		public double getError() {
			return _error;
		}

		public void setError(double error) {
			_error = error;
		}

		public int compareTo(ModelErrorPair modelErrorPair) {
			double thisError = this._error;
			double otherError = modelErrorPair.getError();
			if(thisError < otherError) {
				return -1;
			}
			if(otherError < thisError) {
				return 1;
			}
			else {
				return 0;
			}
		}

		public String toString() {
			return _name + ": " + _error;
		}
	}


	public class ModelBordaPair implements Comparable<ModelBordaPair> {

		private String _name;
		private AbstractModel _model;
		private double _bordaCount;

		public ModelBordaPair(String name, AbstractModel model, double bordaCount) {
			_name = name;
			_model = model;
			_bordaCount = bordaCount;
		}

		public String getName() {
			return _name;
		}

		public void setName(String name) {
			_name = name;
		}

		public AbstractModel getModel() {
			return _model;
		}

		public void setModel(AbstractModel  model) {
			_model = model;
		}

		public double getCount() {
			return _bordaCount;
		}

		public void setError(double error) {
			_bordaCount = error;
		}

		public int compareTo(ModelBordaPair modelBordaPair) {
			double thisBorda = this._bordaCount;
			double otherBorda = modelBordaPair.getCount();
			if(thisBorda < otherBorda) {
				return 1;
			}
			if(otherBorda < thisBorda) {
				return -1;
			}
			else {
				return 0;
			}
		}

		public String toString() {
			return _name + ": " + _bordaCount;
		}
	}

	public void printErrDiffAndBorda() {
		double totalBorda = 0;

		for(String name : _totalBordaCount.keySet()) {
			totalBorda += _totalBordaCount.get(name);
		}

		String bordaoutput = "Borda: ";
		String diffoutput = "Diff Error: ";
		for(String name : _modelList) {
			double bordaCount = _totalBordaCount.get(name)/totalBorda;
			LinkedList<Double> errorDiff = _errorDifference.get(name);
			double totError = 0.0;
			for(Double err : errorDiff) {
				totError += err;
			}
			totError /= errorDiff.size();
			diffoutput += totError +  ", ";
			bordaoutput += bordaCount + ", ";
		}
		System.out.println(bordaoutput);
		System.out.println(diffoutput);
	}

	public AbstractModel getCopy() {
		return new WEKAEnsembleBidToCPC(_querySpace, NUMPASTDAYS, ENSEMBLESIZE, _borda, _ignoreNaN);
	}

}