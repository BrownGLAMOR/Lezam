package newmodels.usermodel;

/**
 * @author jberg
 *
 */

import java.util.Random;

import usermodel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicUserModel extends AbstractUserModel {
	
	private Random _R = new Random();					//Random number generator
	private double minF0 = 1755;
	private double maxF0 = 8716;
	private double minF1 = 1734;
	private double maxF1 = 5060;
	private double minF2 = 1412;
	private double maxF2 = 4699;
	private double minIS = 895;
	private double maxIS = 9111;
	private double minT = 1729;
	private double maxT = 822;
	private int numUsers = 90000;
	private double F0users;
	private double F2users;
	private double F1users;
	private double ISusers;
	private double Tusers;
	private double NSusers;
	private int numProducts = 9;

	public BasicUserModel() {
		F0users = randGaussian(minF0,maxF0)/numProducts;
		F1users = randGaussian(minF1,maxF1)/numProducts;
		F2users = randGaussian(minF2,maxF2)/numProducts;
		ISusers = randGaussian(minIS,maxIS)/numProducts;
		Tusers = randGaussian(minT,maxT)/numProducts;
		NSusers = (numUsers - F0users - F1users - F2users - ISusers - Tusers)/numProducts;
	}
	
	@Override
	public double getPrediction(Product product, UserState userState) {
		
		/*
		 * I just simulated a game for a really long time to get the minimum
		 * and maximum % of users in F0,F1, and F2..Then I basically just
		 * pick a normally distributed number based on the min and max.
		 * 
		 * This function returns an array of the number of users in a given
		 * query class.  Index 0 is F0, 1 is F1, and 2 is F2
		 */

		if(userState == UserState.F0) {
			return F0users/numUsers;
		}
		else if(userState == UserState.F1) {
			return F1users/numUsers;
		}
		else if(userState == UserState.F2) {
			return F2users/numUsers;
		}
		else if(userState == UserState.IS) {
			return ISusers/numUsers;
		}
		else if(userState == UserState.T) {
			return Tusers/numUsers;
		}
		else if(userState == UserState.NS) {
			return NSusers/numUsers;
		}
		else {
			throw new RuntimeException("");
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}
	
	private double randGaussian(double a, double b) {
		double rand = _R.nextGaussian();
		double mean = (a+b)/2;
		//How many deviations away from the mean is the min value
		double deviations = 3;
		double stddev = (mean-a)/deviations;
		return rand*stddev+mean;
	}
}
