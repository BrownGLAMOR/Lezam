package simulator;

import java.util.ArrayList;

/**
 * This class will be used for ranking agents in auctions
 */

public class UserClickConvTuple {

	private double[] _click;
	private int _conv;
	private SimUser _user;

	public UserClickConvTuple(SimUser user, double[] click, int conv) {
		_user = user;
		_click = click;
		_conv = conv;
	}

	public double[] getClick() {
		return _click;
	}

	public void seClick(double[] click) {
		_click = click;
	}

	public int getConv() {
		return _conv;
	}

	public void setConv(int conv) {
		_conv = conv;
	}

	public SimUser getUsers() {
		return _user;
	}

	public void setUser(SimUser user) {
		_user = user;
	}

}
