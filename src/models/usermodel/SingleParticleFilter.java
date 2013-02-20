package models.usermodel;

import java.util.Map;

import models.AbstractModel;
import simulator.parser.GameStatusHandler.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class SingleParticleFilter extends ParticleFilterAbstractUserModel {
	
	public SingleParticleFilter(UserModelInput input) {
		
	}

	@Override
	public boolean updateModel(Map<Query, Integer> totalImpressions) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double getPrediction(Product product, UserState userState) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentEstimate(Product product, UserState userState) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getPrediction(Product product, UserState userState, int day) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
