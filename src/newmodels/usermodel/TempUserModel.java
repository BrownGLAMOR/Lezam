package newmodels.usermodel;

/**
 * 
 * @author hchen
 *
 */
import java.util.HashMap;

import usermodel.UserState;
import edu.umich.eecs.tac.props.AdvertiserInfo;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;

public class TempUserModel extends AbstractUserModel{
	protected String _compSpecialty;
	protected String _manSpecialty;
	protected HashMap<String,Double> _manOpponents;
	protected HashMap<String, Double> _compOpponents;
	protected HashMap<Double, Double> _burstDays;
	
	public TempUserModel(AdvertiserInfo advertiserinfo){
		 _compSpecialty = advertiserinfo.getComponentSpecialty();
		 _manSpecialty = advertiserinfo.getManufacturerSpecialty();
		 _manOpponents = new HashMap<String, Double>();
		 _compOpponents = new HashMap<String, Double>();	 
		 _burstDays = new HashMap<Double, Double>(); 
	}
	
	@Override
	public double getPrediction(UserState userState) {
		
		return 0;
	}

	@Override
	public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
		
		return false;
	}

	
	//if the impression is significantly high, then we doubt it is a burst day
	//the doubt is quantified as a number between 0 and 1.
	//if the double is above 0.9, then it is worth considering it
	public double detectBurstDay(){
		 return 0; 
	}
	
	//return null in the first two days
	//assume all agents target their ads rationally, i.e. they target their ads on F2 users, and possibly on F1 users
	public double[] detectManufactureOpponent(){
		return null;
	}
	
	//return null in the first two days
	//assume all agents target their ads rationally, i.e. they target their ads on F2 users, and possibly on F1 users
	public double[] detectComponentOpponent(){
		return null;
	}
	
	//return 0 in the first two days
	//assumption: if 
	public double detectCapacityOverheads(){
		return 0;
	}
}
