package models.ISratio;

import edu.umich.eecs.tac.props.Query;
import models.AbstractModel;

import java.util.HashMap;
import java.util.Set;
// predicts the ration of Informational searchers to other searchers
public class ISRatioModel extends AbstractModel {

   Set<Query> _querySpace;
   int _numSlots;
   HashMap<Query,double[]> _ISRatioPreds;

   //Initializes ISRatio Map
   public ISRatioModel(Set<Query> querySpace,int numSlots) {
      _querySpace = querySpace;
      _numSlots = numSlots;
      _ISRatioPreds = new HashMap<Query, double[]>();

      for(Query q : _querySpace) {
         double[] ISRatio = new double[_numSlots];
         for(int i = 0; i < _numSlots; i++) {
            ISRatio[i] = 0.0;
         }
         _ISRatioPreds.put(q,ISRatio);
      }
   }

   public void updateISRatio(Query q, double[] ISRatio) {
      _ISRatioPreds.put(q,ISRatio);
   }
   // FIXME: There is another getISRatio method in Constants and Functions. 
   //Should that be moved here?
   public double[] getISRatio(Query q) {
      return _ISRatioPreds.get(q);
   }

   @Override
   public AbstractModel getCopy() {
      return new ISRatioModel(_querySpace,_numSlots);
   }
}
