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
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.QueryType;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.SalesReport;

public class PerfectConversionProb extends AbstractPrConversionModel {
	
	private double _compSpecialtyBonus;
	private String _compSpecialty;
	private AbstractUserModel _userModel;
	
	private double LAMBDA = .995;
	private RetailCatalog _retailCatalog;
	private PerfectQueryToNumImp _numImpModel;

	public PerfectConversionProb(double compSpecialtyBonus,
			String compSpecialty,
			Query query,
			RetailCatalog retailCatalog,
			PerfectUserModel userModel,
			PerfectQueryToNumImp numImpModel) {
		
		super(query);
		_compSpecialtyBonus = compSpecialtyBonus;
		_compSpecialty = compSpecialty;
		_retailCatalog = retailCatalog;
		_userModel = userModel;
		_numImpModel = numImpModel;
	}

	@Override
	public double getPrediction(double amountOverCap) {
		double baselineconv;
		if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
			baselineconv = .1;
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
			baselineconv = .2;
		}
		else if(_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
			baselineconv = .3;
		}
		else {
			throw new RuntimeException("Bad Query");
		}
		int numISUsers = 0;
		for(Product product : _retailCatalog) {
			if(_query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
				numISUsers += _userModel.getPrediction(product, UserState.IS) / 3;
			}
			else if(_query.getType() == QueryType.FOCUS_LEVEL_ONE) {
    			if(product.getComponent().equals(_query.getComponent()) || product.getManufacturer().equals(_query.getManufacturer())) {
    				numISUsers += _userModel.getPrediction(product, UserState.IS) / 6;
    			}
			}
			else if(_query.getType() == QueryType.FOCUS_LEVEL_TWO) {
    			if(product.getComponent().equals(_query.getComponent()) && product.getManufacturer().equals(_query.getManufacturer())) {
    				numISUsers += _userModel.getPrediction(product, UserState.IS) / 3;
    			}
			}
		}
		int numImps = _numImpModel.getPrediction(_query);
		double ISUserDiscount = 1 - numISUsers/numImps;
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
