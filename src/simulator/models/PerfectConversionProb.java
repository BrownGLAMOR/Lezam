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

	public PerfectConversionProb(Query query) {
		super(query);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getPrediction(double overcap) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
