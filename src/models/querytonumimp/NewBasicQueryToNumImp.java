package models.querytonumimp;

/**
 * @author jberg
 *
 */

import edu.umich.eecs.tac.props.*;
import models.AbstractModel;
import models.usermodel.ParticleFilterAbstractUserModel;
import simulator.parser.GameStatusHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NewBasicQueryToNumImp extends AbstractQueryToNumImp {

   private ParticleFilterAbstractUserModel _userModel;
   private Set<Product> _products;
   private HashMap<Query, Integer> _numImps;
   private Set<Query> _querySpace;

   public NewBasicQueryToNumImp(ParticleFilterAbstractUserModel userModel) {
      _userModel = userModel;
      _products = new HashSet<Product>();
      _querySpace = new HashSet<Query>();
      _numImps = new HashMap<Query, Integer>();

      //Initialize products
      _products.add(new Product("pg", "tv"));
      _products.add(new Product("pg", "dvd"));
      _products.add(new Product("pg", "audio"));
      _products.add(new Product("lioneer", "tv"));
      _products.add(new Product("lioneer", "dvd"));
      _products.add(new Product("lioneer", "audio"));
      _products.add(new Product("flat", "tv"));
      _products.add(new Product("flat", "dvd"));
      _products.add(new Product("flat", "audio"));

      //Initialize Query Space
      _querySpace.add(new Query(null, null));
      for (Product product : _products) {
         // The F1 query classes
         // F1 Manufacturer only
         _querySpace.add(new Query(product.getManufacturer(), null));
         // F1 Component only
         _querySpace.add(new Query(null, product.getComponent()));

         // The F2 query class
         _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
      }
   }

   @Override
   public int getPrediction(Query q, int day) {
      return _numImps.get(q);
   }

   @Override
   public boolean updateModel(QueryReport queryReport, SalesReport salesReport) {
      //Set num impressions per query
      for (Query query : _querySpace) {
         int numImps = 0;
         for (Product product : _products) {
            if (query.getType() == QueryType.FOCUS_LEVEL_ZERO) {
               numImps += _userModel.getPrediction(product, GameStatusHandler.UserState.F0);
               numImps += _userModel.getPrediction(product, GameStatusHandler.UserState.IS) / 3.0;
            } else if (query.getType() == QueryType.FOCUS_LEVEL_ONE) {
               if (product.getComponent().equals(query.getComponent()) || product.getManufacturer().equals(query.getManufacturer())) {
                  numImps += _userModel.getPrediction(product, GameStatusHandler.UserState.F1) / 2.0;
                  numImps += _userModel.getPrediction(product, GameStatusHandler.UserState.IS) / 6.0;
               }
            } else if (query.getType() == QueryType.FOCUS_LEVEL_TWO) {
               if (product.getComponent().equals(query.getComponent()) && product.getManufacturer().equals(query.getManufacturer())) {
                  numImps += _userModel.getPrediction(product, GameStatusHandler.UserState.F2);
                  numImps += _userModel.getPrediction(product, GameStatusHandler.UserState.IS) / 3.0;
               }
            }
         }
         _numImps.put(query, numImps);
      }
      return true;
   }

   @Override
   public AbstractModel getCopy() {
      return new NewBasicQueryToNumImp(_userModel);
   }

}
