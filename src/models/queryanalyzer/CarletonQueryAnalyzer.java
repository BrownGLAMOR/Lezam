package models.queryanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import models.queryanalyzer.ds.QAInstance;
import models.queryanalyzer.iep.IEResult;
import models.queryanalyzer.search.LDSearchIESmart;

public class CarletonQueryAnalyzer extends AbstractQueryAnalyzer {

	private HashMap<Query,ArrayList<IEResult>> _allResults;
	private Set<Query> _querySpace;
	private HashMap<String,Integer> _advToIdx;
	private ArrayList<String> _advertisers;
	private String _ourAdvertiser;
	public final static int NUM_SLOTS = 5;
	public final static int maxImps = 2000;

	public CarletonQueryAnalyzer(Set<Query> querySpace, ArrayList<String> advertisers, String ourAdvertiser) {
		_querySpace = querySpace;

		_allResults = new HashMap<Query,ArrayList<IEResult>>();
		for(Query q : _querySpace) {
			ArrayList<IEResult> resultsList = new ArrayList<IEResult>();
			_allResults.put(q, resultsList);
		}

		_advertisers = advertisers;
		
		_ourAdvertiser = ourAdvertiser;

		_advToIdx = new HashMap<String,Integer>();
		for(int i = 0; i < advertisers.size(); i++) {
			_advToIdx.put(advertisers.get(i), i);
		}
	}

	@Override
	public int getImpressionsPrediction(Query q, String adv) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allResults.get(q).get(size-1).getSol()[_advToIdx.get(adv)];
		}
		return 0;
	}

	@Override
	public int[] getImpressionsPrediction(Query q) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allResults.get(q).get(size-1).getSol();
		}
		return null;
	}

	@Override
	public int getOrderPrediction(Query q, String adv) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allResults.get(q).get(size-1).getOrder()[_advToIdx.get(adv)];
		}
		return 0;
	}

	@Override
	public int[] getOrderPrediction(Query q) {
		int size = _allResults.get(q).size();
		if(size > 0) {
			return _allResults.get(q).get(size-1).getOrder();
		}
		return null;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
		for(Query q : _querySpace) {
			double[] avgPos = new double[_advertisers.size()];
			int[] agentIds = new int[_advertisers.size()];
			for(int i = 0; i < _advertisers.size(); i++) {
				if(_advertisers.get(i).equals(_ourAdvertiser)) {
					avgPos[i] = queryReport.getPosition(q);
				}
				else {
					avgPos[i] = queryReport.getPosition(q, _advertisers.get(i));
				}
				agentIds[i] = i;
			}

			//this should maybe be squashed bid
			QAInstance inst = new QAInstance(NUM_SLOTS, _advertisers.size(), avgPos, agentIds, _advToIdx.get(_ourAdvertiser), queryReport.getImpressions(q), bidBundle.getBid(q), maxImps);

			int[] avgPosOrder = inst.getAvgPosOrder();

			LDSearchIESmart smartIESearcher = new LDSearchIESmart(10, inst);
			smartIESearcher.search(avgPosOrder, inst.getAvgPos());
			IEResult bestSol = smartIESearcher.getBestSolution();
			_allResults.get(q).add(bestSol);
		}
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new CarletonQueryAnalyzer(_querySpace,_advertisers,_ourAdvertiser);
	}

}
