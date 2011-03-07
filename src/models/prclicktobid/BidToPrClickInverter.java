package models.prclicktobid;

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.bidtoprclick.AbstractBidToPrClick;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

import java.util.HashMap;
import java.util.Set;

public class BidToPrClickInverter extends AbstractPrClickToBid {

   private RConnection _rConnection;
   private Set<Query> _querySpace;
   private HashMap<Query, double[]> _coefficients;
   private double _increment;
   private double _min;
   private double _max;
   private AbstractBidToPrClick _model;

   public BidToPrClickInverter(RConnection rConnection, Set<Query> querySpace, AbstractBidToPrClick model, double increment, double min, double max) {
      _rConnection = rConnection;
      _querySpace = querySpace;
      _model = model;
      _increment = increment;
      _min = min;
      _max = max;


      _coefficients = new HashMap<Query, double[]>();
      for (Query query : _querySpace) {
         _coefficients.put(query, null);
      }
   }

   public void setIncrements(double increment, double min, double max) {
      _increment = increment;
      _min = min;
      _max = max;
   }

   public double[] getIncrementedArray() {
      return getIncrementedArray(_increment, _min, _max);
   }

   public double[] getIncrementedArray(double increment, double min, double max) {
      double diff = max - min;
      double len = Math.ceil(diff / increment) + 1;
      if (len > 100) {
         throw new RuntimeException("More than 100 points is probably overkill for the inversion");
      }
      double[] incrementedArr = new double[(int) len];
      for (int i = 0; i < len; i++) {
         incrementedArr[i] = min + i * increment;
      }
      return incrementedArr;
   }

   public double getPrediction(Query query, double prclick) {
      double[] coeff = _coefficients.get(query);
      if (coeff == null) {
         return Double.NaN;
      }

      double bid = coeff[0] + prclick * coeff[1];

      bid = 1 / (1 + Math.exp(-bid));

      if (bid < _min) {
         return _min;
      }

      if (bid > _max) {
         return _max;
      }

      return bid;
   }

   public boolean updateModel(QueryReport queryReport, SalesReport salesReport, BidBundle bidBundle) {
      for (Query query : _querySpace) {
         double[] bids = getIncrementedArray();
         double[] clickPrs = new double[bids.length];
         for (int i = 0; i < bids.length; i++) {
            clickPrs[i] = _model.getPrediction(query, bids[i], new Ad());
         }
         try {
            _rConnection.assign("bids", bids);
            _rConnection.assign("clickPrs", clickPrs);
            String Rmodel = "model = glm(bids ~ clickPrs, family = quasibinomial(link = \"logit\"))";
            _rConnection.voidEval(Rmodel);
            double[] coefficients = _rConnection.eval("coefficients(model)").asDoubles();
            _coefficients.put(query, coefficients);
            for (int i = 0; i < coefficients.length; i++) {
               if (Double.isNaN(coefficients[i])) {
                  _coefficients.put(query, null);
               }
            }
         } catch (REngineException e) {
//				e.printStackTrace();
         } catch (REXPMismatchException e) {
//				e.printStackTrace();
         }
      }
      return true;
   }

   public String toString() {
      return "BidToCPCInverter(model: " + _model + ",increment: " + _increment + ", min: " + _min + ", max: " + _max + " )";
   }

   @Override
   public AbstractModel getCopy() {
      return new BidToPrClickInverter(_rConnection, _querySpace, _model, _increment, _min, _max);
   }

}
