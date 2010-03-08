package models.usermodel;

/**
 * @author jberg
 *
 */

import java.util.Random;

import models.AbstractModel;
import models.usermodel.TacTexAbstractUserModel.UserState;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class BasicUserModel extends AbstractUserModel {
	
	private double F0users;
	private double F2users;
	private double F1users;
	private double ISusers;
	private double Tusers;
	private double NSusers;

	public BasicUserModel() {
		F0users = 570;
		F1users = 412;
		F2users = 444;
		ISusers = 301;
		Tusers = 18;
		NSusers = 8255;
	}
	
	@Override
	public int getPrediction(Product product, UserState userState, int day) {

		if(userState == UserState.F0) {
			return (int) F0users;
		}
		else if(userState == UserState.F1) {
			return (int) F1users;
		}
		else if(userState == UserState.F2) {
			return (int) F2users;
		}
		else if(userState == UserState.IS) {
			return (int) ISusers;
		}
		else if(userState == UserState.T) {
			return (int) Tusers;
		}
		else if(userState == UserState.NS) {
			return (int) NSusers;
		}
		else {
			throw new RuntimeException("");
		}
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		return true;
	}

	@Override
	public AbstractModel getCopy() {
		return new BasicUserModel();
	}
	
}
