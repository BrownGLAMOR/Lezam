package simulator.models;

/**
 * @author jberg
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;

import newmodels.AbstractModel;
import newmodels.prconv.AbstractPrConversionModel;
import newmodels.usermodel.AbstractUserModel;

import usermodel.UserState;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectConversionProb extends AbstractPrConversionModel {
	

	
	private double _compSpecialtyBonus;
	private String _compSpecialty;
	private Query _query;
	private AbstractUserModel _userModel;
	
	private double LAMBDA = .995;

	public PerfectConversionProb(double compSpecialtyBonus,
			String compSpecialty,
			Query query,
			AbstractUserModel userModel) {
		
		super(query);
		_compSpecialtyBonus = compSpecialtyBonus;
		_compSpecialty = compSpecialty;
		_userModel = userModel;
	}

	@Override
	public double getPrediction(double amountOverCap) {
		double baselineconv;
		double users;
		if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			baselineconv = .1;
			users = (Double) _userModel.getPrediction(UserState.F0);
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			baselineconv = .2;
			users = (Double) _userModel.getPrediction(UserState.F1);
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
			baselineconv = .3;
			users = (Double) _userModel.getPrediction(UserState.F2);
		}
		else {
			throw new RuntimeException("Bad Query");
		}
		double ISUsers = (Double) _userModel.getPrediction(UserState.IS);
		double ISUserDiscount = users/(users+(ISUsers/3));
		double capDiscount = Math.pow(LAMBDA ,amountOverCap);
		double convRate = baselineconv*capDiscount*ISUserDiscount;
		if(_query.getComponent() == _compSpecialty) {
			return eta(convRate,1+_compSpecialtyBonus);
		}
		else {
			return convRate;
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		//Nothing needs to be updated
		return true;
	}
	
	public double eta(double p, double x) {
		return (p*x)/(p*x + (1-p));
	}

}
