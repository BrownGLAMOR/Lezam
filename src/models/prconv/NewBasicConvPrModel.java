package models.prconv;

import java.util.HashMap;
import java.util.Set;


import models.AbstractModel;
import models.usermodel.AbstractUserModel;
import models.usermodel.ParticleFilterAbstractUserModel;
import models.usermodel.ParticleFilterAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class NewBasicConvPrModel extends AbstractConversionModel {

	private ParticleFilterAbstractUserModel _userModel;
	private Set<Query> _querySpace;
	private HashMap<Query,Double> _baselineConvPr;
	int day = 0;

	public NewBasicConvPrModel(ParticleFilterAbstractUserModel userModel, Set<Query> querySpace, HashMap<Query, Double> baselineConvPr) {
		_userModel = userModel;
		_querySpace = querySpace;
		_baselineConvPr = baselineConvPr;
	}
	
	@Override
	public double getPrediction(Query q) {
		double ISeffect;
		Product prod = new Product("pg","tv");
		if(q.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			double F0Users = _userModel.getPrediction(prod, UserState.F0);
			double ISUsers = _userModel.getPrediction(prod, UserState.IS);
			ISeffect = F0Users/((1/3.0) * ISUsers + F0Users);
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_ONE) {
			double F1Users = _userModel.getPrediction(prod, UserState.F1);
			double ISUsers = _userModel.getPrediction(prod, UserState.IS);
			ISeffect = F1Users/((1/3.0) * ISUsers + F1Users);
		}
		else if(q.getType() == QueryType.FOCUS_LEVEL_TWO) {
			double F2Users = _userModel.getPrediction(prod, UserState.F2);
			double ISUsers = _userModel.getPrediction(prod, UserState.IS);
			ISeffect = F2Users/((1/3.0) * ISUsers + F2Users);		}
		else {
			throw new RuntimeException("Malformed query");
		}
		return _baselineConvPr.get(q)*ISeffect;
	}

	@Override
	public double getPredictionWithBid(Query query, double bid) {
		return getPrediction(query);
	}

	@Override
	public double getPredictionWithPos(Query query, double pos) {
		return getPrediction(query);
	}

	@Override
	public void setSpecialty(String manufacturerSpecialty,
			String componentSpecialty) {
	}

	@Override
	public void setTimeHorizon(int min) {
	}

	@Override
	public boolean updateModel(QueryReport queryReport,
			SalesReport salesReport, BidBundle bundle) {
		day++;
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new NewBasicConvPrModel(_userModel,_querySpace,_baselineConvPr);
	}

}
