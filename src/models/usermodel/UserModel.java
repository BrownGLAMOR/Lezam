/**
 *
 */
package models.usermodel;

import java.util.Map;

import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;
import simulator.parser.GameStatusHandler;

/**
 * @author jberg
 */
public abstract class UserModel extends AbstractModel {

	public enum ModelType {
		DEFAULT, HISTORICAL_DAILY_AVERAGE, JBERG_PARTICLE_FILTER, JBERG_DYNAMIC_PARTICLE_FILTER, SINGLE_PARTICLE_FILTER
	}

	public static UserModel build(UserModelInput input) {
		return build(input, ModelType.DEFAULT);
	}

	public static UserModel build(UserModelInput input, ModelType type) {
		switch(type) {
			case HISTORICAL_DAILY_AVERAGE:
				return new HistoricalDailyAverageUserModel(input);
			case JBERG_PARTICLE_FILTER:
				return new jbergParticleFilter(input);
			case JBERG_DYNAMIC_PARTICLE_FILTER:
				return new jbergDynamicParticleFilter(input);
			case SINGLE_PARTICLE_FILTER:
				return new SingleParticleFilter(input);
			case DEFAULT:
			default:
				return new jbergDynamicParticleFilter(input);
		}
	}

	public abstract boolean updateModel(QueryReport queryReport,
			SalesReport salesReport);
	
	public abstract boolean updateModel(Map<Query,Integer> numImpressions);
	
	public abstract double getPrediction(Product prod, GameStatusHandler.UserState userState);

	public abstract int getPrediction(Product product,
			GameStatusHandler.UserState userState, int day);

}
