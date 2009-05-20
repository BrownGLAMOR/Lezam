package modelers;

import agents.rules.Constants;
import modelers.UserStateTransitionModel.UserState;
import edu.umich.eecs.tac.props.*;

public class ConversionModel {

    private UserModel _userModel;
    private int _maxCapacity;
    private int _lastFiveDaySales;
    private double _capacityDiscounter;

    public static final double DIST_CAPACITY_DISCOUNTER = .995;
    
    public ConversionModel(UserModel userModel, int maxCapacity, int lastFiveDaySales) {
    
        _userModel = userModel;
        _maxCapacity = maxCapacity;
        _lastFiveDaySales = lastFiveDaySales;

        int overCapacity = (int)Math.max(lastFiveDaySales - maxCapacity,0);
        _capacityDiscounter = Math.pow(DIST_CAPACITY_DISCOUNTER,overCapacity);


    }

    public double getConversionProbability(UserState state) {
        double baseline = 0;
        if(state == UserState.F0) 
            baseline = Constants.CONVERSION_F0;
        else if(state == UserState.F1) 
            baseline = Constants.CONVERSION_F1;
        else if(state == UserState.F2) 
            baseline = Constants.CONVERSION_F2;

        return baseline*_capacityDiscounter*_userModel.getSearchingRatio();
    }

}
