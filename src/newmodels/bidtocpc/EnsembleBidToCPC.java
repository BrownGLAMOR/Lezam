package newmodels.bidtocpc;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;

public class EnsembleBidToCPC extends AbstractBidToCPC {

	protected Set<Query> _querySpace;

	/*
	 * Model Type I
	 * 	-These models predict for all queries
	 */
	protected HashMap<String,AbstractBidToCPC> _typeIModels;
	protected HashMap<String,AbstractBidToCPC> _typeIUsableModels;
	protected HashMap<String,HashMap<Query,LinkedList<Double>>> _typeIDailyModelError;
	protected HashMap<BidBundle,HashMap<String,HashMap<Query,Double>>> _typeIPredictions;

	/*
	 * Model Type II
	 * 	-These models predict by query type
	 */
	protected HashMap<QueryType,HashMap<String,AbstractBidToCPC>> _typeIIModels;
	protected HashMap<QueryType,HashMap<String,AbstractBidToCPC>> _typeIIUsableModels;
	protected HashMap<QueryType,HashMap<String,HashMap<Query,LinkedList<Double>>>> _typeIIDailyModelError;
	protected HashMap<BidBundle,HashMap<QueryType,HashMap<String,HashMap<Query,Double>>>> _typeIIPredictions;

	/*
	 * Model Type II
	 * 	-These models only predict for one query
	 */
	protected HashMap<Query,HashMap<String,AbstractBidToCPC>> _typeIIIModels;
	protected HashMap<Query,HashMap<String,AbstractBidToCPC>> _typeIIIUsableModels;
	protected HashMap<Query,HashMap<String,LinkedList<Double>>> _typeIIIDailyModelError;
	protected HashMap<BidBundle,HashMap<Query,HashMap<String,Double>>> _typeIIIPredictions;


	public EnsembleBidToCPC(Set<Query> querySpace) {
		_querySpace = querySpace;

		/*
		 * Initialize Type I Models
		 */
		_typeIModels = new HashMap<String, AbstractBidToCPC>();
		_typeIUsableModels = new HashMap<String,AbstractBidToCPC>();
		_typeIDailyModelError = new HashMap<String, HashMap<Query,LinkedList<Double>>>();
		_typeIPredictions = new HashMap<BidBundle, HashMap<String,HashMap<Query,Double>>>();

		/*
		 * Initialize Type II Models
		 */
		_typeIIModels = new HashMap<QueryType, HashMap<String,AbstractBidToCPC>>();
		_typeIIUsableModels = new HashMap<QueryType, HashMap<String,AbstractBidToCPC>>();
		_typeIIDailyModelError = new HashMap<QueryType, HashMap<String,HashMap<Query,LinkedList<Double>>>>();
		for(QueryType queryType : QueryType.values()) {
			HashMap<String, AbstractBidToCPC> typeIIModels = new HashMap<String, AbstractBidToCPC>();
			HashMap<String, AbstractBidToCPC> typeIIUsableModels = new HashMap<String,AbstractBidToCPC>();
			HashMap<String, HashMap<Query, LinkedList<Double>>> typeIIDailyModelError = new HashMap<String, HashMap<Query,LinkedList<Double>>>();
			_typeIIModels.put(queryType, typeIIModels);
			_typeIIUsableModels.put(queryType,typeIIUsableModels);
			_typeIIDailyModelError.put(queryType,typeIIDailyModelError);
		}
		_typeIIPredictions = new HashMap<BidBundle, HashMap<QueryType,HashMap<String,HashMap<Query,Double>>>>();

		/*
		 * Initialize Type III Models
		 */
		_typeIIIModels = new HashMap<Query, HashMap<String,AbstractBidToCPC>>();
		_typeIIIUsableModels = new HashMap<Query, HashMap<String,AbstractBidToCPC>>();
		_typeIIIDailyModelError = new HashMap<Query, HashMap<String,LinkedList<Double>>>();
		for(Query query : _querySpace) {
			HashMap<String, AbstractBidToCPC> typeIIIModels = new HashMap<String, AbstractBidToCPC>();
			HashMap<String, AbstractBidToCPC> typeIIIUsableModels = new HashMap<String,AbstractBidToCPC>();
			HashMap<String, LinkedList<Double>> typeIIIDailyModelError = new HashMap<String, LinkedList<Double>>();
			_typeIIIModels.put(query, typeIIIModels);
			_typeIIIUsableModels.put(query,typeIIIUsableModels);
			_typeIIIDailyModelError.put(query,typeIIIDailyModelError);
		}
		_typeIIIPredictions = new HashMap<BidBundle, HashMap<Query,HashMap<String,Double>>>();
	}

	public void addTypeIModel(String name, AbstractBidToCPC model) {
		_typeIModels.put(name,model);
		HashMap<Query, LinkedList<Double>> dailyModelError = new HashMap<Query,LinkedList<Double>>();
		for(Query query : _querySpace) {
			LinkedList<Double> dailyModelErrorList = new LinkedList<Double>();
			dailyModelError.put(query,dailyModelErrorList);
		}
		_typeIDailyModelError.put(name,dailyModelError);
	}


	public void addTypeIIModel(QueryType queryType,String name, AbstractBidToCPC model) {
		HashMap<String, AbstractBidToCPC> typeIIModels = _typeIIModels.get(queryType);
		typeIIModels.put(name, model);
		_typeIIModels.put(queryType,typeIIModels);

		HashMap<Query, LinkedList<Double>> dailyModelError = new HashMap<Query,LinkedList<Double>>();
		for(Query query : _querySpace) {
			LinkedList<Double> dailyModelErrorList = new LinkedList<Double>();
			dailyModelError.put(query,dailyModelErrorList);
		}
		HashMap<String, HashMap<Query, LinkedList<Double>>> typeIIDailyModelError = _typeIIDailyModelError.get(queryType);
		typeIIDailyModelError.put(name,dailyModelError);
		_typeIIDailyModelError.put(queryType, typeIIDailyModelError);
	}

	public void addTypeIIIModel(Query query, String name, AbstractBidToCPC model) {
		HashMap<String, AbstractBidToCPC> typeIIIModels = _typeIIIModels.get(query);
		typeIIIModels.put(name, model);
		_typeIIIModels.put(query,typeIIIModels);

		HashMap<String, LinkedList<Double>> typeIIIDailyModelError = _typeIIIDailyModelError.get(query);
		LinkedList<Double> dailyModelErrorList = new LinkedList<Double>();
		typeIIIDailyModelError.put(name, dailyModelErrorList);
		_typeIIIDailyModelError.put(query, typeIIIDailyModelError);
	}


	@Override
	public boolean updateModel(QueryReport queryreport, BidBundle bidbundle) {

		boolean ensembleUsable = false;

		/*
		 * Update Type I Models
		 */
		_typeIUsableModels = new HashMap<String, AbstractBidToCPC>();
		for(String name : _typeIModels.keySet()) {
			AbstractBidToCPC model = _typeIModels.get(name);
			boolean usable = model.updateModel(queryreport, bidbundle);
			if(usable) {
				_typeIUsableModels.put(name, model);
				ensembleUsable = true;
			}
		}

		/*
		 * Update Type II Models
		 */
		_typeIIUsableModels = new HashMap<QueryType, HashMap<String,AbstractBidToCPC>>();
		for(QueryType queryType : QueryType.values()) {
			HashMap<String, AbstractBidToCPC> typeIIModels = _typeIIModels.get(queryType);
			HashMap<String, AbstractBidToCPC> typeIIUsableModels = new HashMap<String,AbstractBidToCPC>();
			for(String name : typeIIModels.keySet()) {
				AbstractBidToCPC model = typeIIModels.get(name);
				boolean usable = model.updateModel(queryreport, bidbundle);
				if(usable) {
					typeIIUsableModels.put(name, model);
					ensembleUsable = true;
				}
			}
			_typeIIUsableModels.put(queryType, typeIIUsableModels);
		}

		/*
		 * Update Type III Models
		 */
		_typeIIIUsableModels = new HashMap<Query, HashMap<String,AbstractBidToCPC>>();
		for(Query query : _querySpace) {
			HashMap<String, AbstractBidToCPC> typeIIIModels = _typeIIIModels.get(query);
			HashMap<String, AbstractBidToCPC> typeIIIUsableModels = new HashMap<String,AbstractBidToCPC>();
			for(String name : typeIIIModels.keySet()) {
				AbstractBidToCPC model = typeIIIModels.get(name);
				boolean usable = model.updateModel(queryreport, bidbundle);
				if(usable) {
					typeIIIUsableModels.put(name, model);
					ensembleUsable = true;
				}
			}
			_typeIIIUsableModels.put(query, typeIIIUsableModels);
		}

		return ensembleUsable;
	}

	public void updatePredictions(BidBundle bundle) {

		/*
		 * Update Type I Predictions
		 */
		HashMap<String,HashMap<Query,Double>> typeIPredictions = new HashMap<String, HashMap<Query,Double>>();
		for(String name : _typeIUsableModels.keySet()) {
			AbstractBidToCPC model = _typeIUsableModels.get(name);
			HashMap<Query,Double> predictions = new HashMap<Query, Double>();
			for(Query query : _querySpace) {
				predictions.put(query, model.getPrediction(query, bundle.getBid(query)));
			}
			typeIPredictions.put(name, predictions);
		}
		_typeIPredictions.put(bundle, typeIPredictions);


		/*
		 * Update Type II Predictions
		 */
		HashMap<QueryType,HashMap<String,HashMap<Query,Double>>> typeIIPredictions = new HashMap<QueryType,HashMap<String,HashMap<Query,Double>>>();
		for(QueryType queryType : QueryType.values()) {
			HashMap<String, HashMap<Query, Double>> queryTypePredictions = new HashMap<String,HashMap<Query,Double>>();
			HashMap<String, AbstractBidToCPC> typeIIUsableModels = _typeIIUsableModels.get(queryType);
			for(String name : typeIIUsableModels.keySet()) {
				AbstractBidToCPC model = typeIIUsableModels.get(name);
				HashMap<Query,Double> predictions = new HashMap<Query, Double>();
				for(Query query : _querySpace) {
					if(queryType == query.getType()) {
						predictions.put(query, model.getPrediction(query, bundle.getBid(query)));
					}
				}
				queryTypePredictions.put(name, predictions);
			}
			typeIIPredictions.put(queryType, queryTypePredictions);
		}
		_typeIIPredictions.put(bundle, typeIIPredictions);


		/*
		 * Update Type III Predictions
		 */
		HashMap<Query, HashMap<String, Double>> typeIIIPredictions = new HashMap<Query,HashMap<String,Double>>();
		for(Query query : _querySpace) {
			HashMap<String, AbstractBidToCPC> typeIIIUsableModels = _typeIIIUsableModels.get(query);
			HashMap<String, Double> queryPredictions = new HashMap<String, Double>();
			for(String name : typeIIIUsableModels.keySet()) {
				AbstractBidToCPC model = typeIIIUsableModels.get(name);
				queryPredictions.put(name, model.getPrediction(query, bundle.getBid(query)));
			}
			typeIIIPredictions.put(query, queryPredictions);
		}
		_typeIIIPredictions.put(bundle, typeIIIPredictions);
	}


	public void updateError(QueryReport queryReport, BidBundle bundle) {

		/*
		 * Update Type I Error
		 */
		for(String name : _typeIModels.keySet()) {
			AbstractBidToCPC model = _typeIUsableModels.get(name);
			HashMap<Query, LinkedList<Double>> typeIDailyModelError = _typeIDailyModelError.get(name);
			for(Query query : _querySpace) {
				LinkedList<Double> queryDailyError = typeIDailyModelError.get(query);
				if(model != null) {
					double error = model.getPrediction(query, bundle.getBid(query)) - queryReport.getCPC(query);
					error = error*error;
					queryDailyError.add(error);
				}
				else {
					queryDailyError.add(Double.NaN);
				}
				typeIDailyModelError.put(query, queryDailyError);
			}
			_typeIDailyModelError.put(name, typeIDailyModelError);
		}

		/*
		 * Update Type II Error
		 */
		for(QueryType queryType : QueryType.values()) {
			HashMap<String, HashMap<Query, LinkedList<Double>>> typeIIDailyModelError = _typeIIDailyModelError.get(queryType);
			HashMap<String, AbstractBidToCPC> typeIIUsableModels = _typeIIUsableModels.get(queryType);
			for(String name : _typeIIModels.get(queryType).keySet()) {
				HashMap<Query, LinkedList<Double>> dailyError = typeIIDailyModelError.get(name);
				AbstractBidToCPC model = typeIIUsableModels.get(name);
				for(Query query : _querySpace) {
					LinkedList<Double> queryDailyError = dailyError.get(query);
					if(queryType == query.getType()) {
						if(model != null) {
							double error = model.getPrediction(query, bundle.getBid(query)) - queryReport.getCPC(query);
							error = error*error;
							queryDailyError.add(error);
						}
						else {
							queryDailyError.add(Double.NaN);
						}
					}
					dailyError.put(query, queryDailyError);
				}
				typeIIDailyModelError.put(name, dailyError);
			}
			_typeIIDailyModelError.put(queryType, typeIIDailyModelError);
		}
		
		
		/*
		 * Update Type III Error
		 */
		for(Query query : _querySpace) {
			HashMap<String, LinkedList<Double>> typeIIIDailyModelError = _typeIIIDailyModelError.get(query);
			for(String name : _typeIIIModels.get(query).keySet()) {
				AbstractBidToCPC model = _typeIIIUsableModels.get(query).get(name);
				LinkedList<Double> queryDailyError = typeIIIDailyModelError.get(name);
				if(model != null) {
					double error = model.getPrediction(query, bundle.getBid(query)) - queryReport.getCPC(query);
					error = error*error;
					queryDailyError.add(error); 
				}
				else {
					queryDailyError.add(Double.NaN);
				}
				typeIIIDailyModelError.put(name, queryDailyError);
			}
			_typeIIIDailyModelError.put(query, typeIIIDailyModelError);
		}
		
	}

	@Override
	public double getPrediction(Query query, double bid) {

		return 0;
	}

}

