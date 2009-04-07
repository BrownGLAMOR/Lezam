package modelers;

import edu.umich.eecs.tac.props.*;

public class UserModel extends AbstractModel {
        // --------------
        // Output stuff
        // Note: NS users are both NS and T
        private int _userCount[];
        boolean _valid;
        // --------------
        
        // --------------
        // Stuff we use to make output stuff
        private UserStateTransitionModel _transitions;
        
        private int _clicks[];   // Per focus level
        private int _conversions[];
        private int _totalImpressions[];
        private int _ourImpressions[];
        private double _baselineConversion[];
        
        private final int TOTAL_USERS = 90000;
        
	UserModel(UserStateTransitionModel TransModel, int clicks[], int conversions[], 
                  int ttlImpressions[], int ourImpressions[], double baselineConversion[]) {
                     
           _userCount = new int[UserStateTransitionModel.UserState.values().length];
           _valid = false;
           
           _transitions = TransModel;

           _clicks = clicks;
           _conversions = conversions;
           _totalImpressions = ttlImpressions;
           _ourImpressions = ourImpressions;
           _baselineConversion = baselineConversion;
           
        }
        
        public boolean calculate() {
           int totalIS = 0;
           int totalS = 0;
           
           for(int i = 0; i < QueryType.values().length; i++) {
              int ISUsers = (int)Math.floor(_clicks[i] - (_conversions[i] / _baselineConversion[i]));
              totalIS += ISUsers;
              _userCount[i] = _totalImpressions[i] - ISUsers;
              totalS += _userCount[i];
           }
           
           _userCount[UserStateTransitionModel.UserState.IS.ordinal()] = totalIS;
           _userCount[UserStateTransitionModel.UserState.TandNS.ordinal()] = TOTAL_USERS - totalS - totalIS;
           
           _valid = true;
           return _valid;
        }
        
        public Integer GetUserCount(UserStateTransitionModel.UserState s) {
           if(_valid)
              return _userCount[s.ordinal()];
           else
              return null;
        }
	
	
}
