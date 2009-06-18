package simulator.models;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import usermodel.UserState;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;
import newmodels.querytonumimp.AbstractQueryToNumImp;
import newmodels.slottoprclick.AbstractSlotToPrClick;

public class PerfectSlotToNumImp extends AbstractQueryToNumImp {

	private HashMap<Product,HashMap<UserState,Integer>> _userDists;
	private int _numSlots;
	/*
	 * This is so we can iterate over the Products, probably should do this some other way....
	 */
	private RetailCatalog _retailCatalog;
	private String _queryComp;
	private String _queryMan;
	private QueryType _queryType;

	public PerfectSlotToNumImp(HashMap<Product,HashMap<UserState, Integer>> userDists, int numSlots, RetailCatalog retailCatalog, Query query) {
		super(query);
		_numSlots = numSlots;
		_retailCatalog = retailCatalog;
		_userDists = userDists;
		_queryComp = _query.getComponent();
		_queryMan = _query.getManufacturer();
		_queryType = _query.getType();
	}

	@Override
	public int getPrediction(double slot) {
		int numImps = 0;
		if(slot <= _numSlots) {
			if(_queryType == QueryType.FOCUS_LEVEL_ZERO) {
				for(Product prod : _retailCatalog) {
					HashMap<UserState,Integer> users = _userDists.get(prod);
					numImps += users.get(UserState.F0);
					numImps += users.get(UserState.IS) / 3.0;
				}
			}
			else if(_queryType == QueryType.FOCUS_LEVEL_ONE) {
				for(Product prod : _retailCatalog) {
					String comp = prod.getComponent();
					String manfact = prod.getManufacturer();
					HashMap<UserState,Integer> users = _userDists.get(prod);
					if((_queryComp != null && comp == _queryComp) || (_queryMan != null && manfact == _queryMan)) {
						numImps += users.get(UserState.F1) / 2.0;
					}
					numImps += users.get(UserState.IS) / 3.0;
				}
				
			}
			else if(_queryType == QueryType.FOCUS_LEVEL_TWO) {
				for(Product prod : _retailCatalog) {
					String comp = prod.getComponent();
					String manfact = prod.getManufacturer();
					HashMap<UserState,Integer> users = _userDists.get(prod);
					if(_queryComp == comp && _queryMan == manfact) {
						numImps += users.get(UserState.F2);
					}
					numImps += users.get(UserState.IS) / 3.0;
				}
			}
			else {
				throw new RuntimeException("Malformed Query");
			}

			return numImps;
		}
		else {
			return 0;
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

}
