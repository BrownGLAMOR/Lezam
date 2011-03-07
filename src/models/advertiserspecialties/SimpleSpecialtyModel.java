package models.advertiserspecialties;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import models.AbstractModel;

import java.util.*;


/**
 * A predictor of agent CSB and MSB based on simple counting heuristics:
 * 1. Agents are more likely to target queries containing their CSB/MSB
 * 2. Agents are more likely to show up in auctions containing their CSB/MSB
 * <p/>
 * For (2), we could perhaps do something like consider a player's location in
 * the auction, but we'll just consider binary observations for now
 * (they were in the auction or they weren't)
 *
 * @author sodomka
 */
public class SimpleSpecialtyModel extends AbstractModel {

   String[] advertisers = {"adv1", "adv2", "adv3", "adv4", "adv5", "adv6", "adv7", "adv8"};
//	int numAdvertisers = advertisers.length;
//	int numQueries = 16;
//	int numComponents = 3;
//	int numManufacturers = 3;
//	int numProducts = 9;

   private Set<Product> _products;
   private Set<Query> _querySpace;


   //How often did each agent appear in query q?
   HashMap<String, HashMap<Query, Double>> appearanceScore;
   HashMap<String, HashMap<Query, HashMap<Product, Integer>>> numTargets;

   public SimpleSpecialtyModel() {
      _products = new HashSet<Product>();
      _querySpace = new HashSet<Query>();

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


      appearanceScore = new HashMap<String, HashMap<Query, Double>>();
      numTargets = new HashMap<String, HashMap<Query, HashMap<Product, Integer>>>();

      for (String advertiser : advertisers) {
         appearanceScore.put(advertiser, new HashMap<Query, Double>());
         numTargets.put(advertiser, new HashMap<Query, HashMap<Product, Integer>>());

         for (Query query : _querySpace) {
            appearanceScore.get(advertiser).put(query, 0.0);
            numTargets.get(advertiser).put(query, new HashMap<Product, Integer>());

            for (Product product : _products) {
               numTargets.get(advertiser).get(query).put(product, 0);
            }
         }
      }
   }


   public void updateModel(QueryReport queryReport) {
      int NUMSLOTS = 5;
      for (String advertiser : advertisers) {
         for (Query query : _querySpace) {
            Ad ad = queryReport.getAd(query, advertiser);

            if (ad != null) {
               double avgPos = queryReport.getPosition(query, advertiser);
               double score = (Double.isNaN(avgPos)) ? 0 : NUMSLOTS - avgPos + 1;
               incrementNumAppearances(advertiser, query, score);
               if (!ad.isGeneric()) {
                  //System.out.println("incrementing a=" + advertiser + " q=" + query + " p=" + ad.getProduct());
                  incrementNumTargets(advertiser, query, ad.getProduct());
               }
            }
         }
      }
   }


   private void incrementNumTargets(String advertiser, Query query, Product product) {
      //Handle case where someone stupidly tried targeting a product that doesn't exist
      //TODO: Should we still use this information??
      if (!_products.contains(product)) {
         return;
      }

      int val = numTargets.get(advertiser).get(query).get(product);
      numTargets.get(advertiser).get(query).put(product, val + 1);
   }

   private void incrementNumAppearances(String advertiser, Query query, double inc) {
      double val = appearanceScore.get(advertiser).get(query);
      appearanceScore.get(advertiser).put(query, val + inc);
   }

   private int getNumTargets(String advertiser, Query query, Product product) {
      return numTargets.get(advertiser).get(query).get(product);
   }

   private double getNumAppearances(String advertiser, Query query) {
      return appearanceScore.get(advertiser).get(query);
   }


   public List<String> getComponentSpecialty(String advertiser) {
      //If they targeted one component more than the rest, make that the specialty
      HashMap<String, Double> numComponentAppearances = getNumComponentAppearances(advertiser);
      HashMap<String, Integer> numComponentTargets = getNumComponentTargets(advertiser);
//		return getMostLikelySpecialty(numComponentAppearances, numComponentTargets);
      return getMostLikelySpecialty(numComponentTargets, numComponentAppearances);
   }

   public List<String> getManufacturerSpecialty(String advertiser) {
      //If they targeted one component more than the rest, make that the specialty
      HashMap<String, Double> numManufacturerAppearances = getNumManufacturerAppearances(advertiser);
      HashMap<String, Integer> numManufacturerTargets = getNumManufacturerTargets(advertiser);
      return getMostLikelySpecialty(numManufacturerTargets, numManufacturerAppearances);
   }


   /**
    * Returns the number of times the advertiser was present in an auction related
    * to each component type.
    *
    * @param advertiser
    * @return
    */
   private HashMap<String, Double> getNumComponentAppearances(String advertiser) {
      HashMap<String, Double> numComponentAppearances = new HashMap<String, Double>();
      for (Product product : _products) {
         numComponentAppearances.put(product.getComponent(), 0.0); //Faster if we just had set of components
      }

      //Count # of appearances for each component
      for (Query query : _querySpace) {
         String component = query.getComponent();
         if (component == null) {
            continue;
         }
         double oldVal = numComponentAppearances.get(component);
         double appearances = getNumAppearances(advertiser, query);
         numComponentAppearances.put(component, oldVal + appearances);
      }
      return numComponentAppearances;
   }


   private HashMap<String, Integer> getNumComponentTargets(String advertiser) {
      HashMap<String, Integer> numComponentTargets = new HashMap<String, Integer>();
      for (Product product : _products) {
         numComponentTargets.put(product.getComponent(), 0); //Faster if we just had set of components
      }

      //Count # of appearances for each component
      for (Query query : _querySpace) {
         for (Product product : _products) {
            String component = product.getComponent();

            int oldVal = numComponentTargets.get(component);
            int numTargets = getNumTargets(advertiser, query, product);
            numComponentTargets.put(component, oldVal + numTargets);
         }
      }
      return numComponentTargets;
   }


   private HashMap<String, Integer> getNumManufacturerTargets(String advertiser) {
      HashMap<String, Integer> numManufacturerTargets = new HashMap<String, Integer>();
      for (Product product : _products) {
         numManufacturerTargets.put(product.getManufacturer(), 0); //Faster if we just had set of components
      }

      //Count # of appearances for each component
      for (Query query : _querySpace) {
         for (Product product : _products) {
            String manufacturer = product.getManufacturer();

            int oldVal = numManufacturerTargets.get(manufacturer);
            int numTargets = getNumTargets(advertiser, query, product);
            numManufacturerTargets.put(manufacturer, oldVal + numTargets);
         }
      }
      return numManufacturerTargets;
   }


   /**
    * Returns the number of times the advertiser was present in an auction related
    * to each component type.
    *
    * @param advertiser
    * @return
    */
   private HashMap<String, Double> getNumManufacturerAppearances(String advertiser) {
      HashMap<String, Double> numManufacturerAppearances = new HashMap<String, Double>();
      for (Product product : _products) {
         numManufacturerAppearances.put(product.getManufacturer(), 0.0); //Faster if we just had set of components
      }

      //Count # of appearances for each manufacturer
      for (Query query : _querySpace) {
         String manufacturer = query.getManufacturer();
         if (manufacturer == null) {
            continue;
         }
         double oldVal = numManufacturerAppearances.get(manufacturer);
         double appearances = getNumAppearances(advertiser, query);
         numManufacturerAppearances.put(manufacturer, oldVal + appearances);
      }
      return numManufacturerAppearances;
   }


   private List<String> getMostLikelySpecialty(HashMap<String, Integer> numSpecialtyTargets, HashMap<String, Double> numSpecialtyAppearances) {
      //Find most likely component specialty.
      //FIXME: this is biasing toward component specialties we traverse earlier,
      //  since ties go to whatever was seen first.
      double targetWeight = 1;
      double appearanceWeight = 1;

      double mostAppearances = -1;
      for (String component : numSpecialtyTargets.keySet()) {
         double appearances = targetWeight * numSpecialtyTargets.get(component) + appearanceWeight * numSpecialtyAppearances.get(component);
         //System.out.println("appearances=" + appearances + " specApp=" + numSpecialtyAppearances.get(component) + " specTarg=" + numSpecialtyTargets.get(component));
         if (appearances > mostAppearances) {
            mostAppearances = appearances;
         }
      }

      List<String> mostLikelySpecialties = new ArrayList<String>();
      for (String component : numSpecialtyTargets.keySet()) {
         double appearances = targetWeight * numSpecialtyTargets.get(component) + appearanceWeight * numSpecialtyAppearances.get(component);
         if (appearances == mostAppearances) {
            mostLikelySpecialties.add(component);
         }
      }


      return mostLikelySpecialties;
   }


   public void printAllPredictions() {
      System.out.print("predictions: ");
      for (String advertiser : advertisers) {
         System.out.print(advertiser + "=" + getComponentSpecialty(advertiser) + "  ");
         System.out.print(advertiser + "=" + getManufacturerSpecialty(advertiser) + "  ");
      }
      System.out.println();
   }

   @Override
   public AbstractModel getCopy() {
      // TODO Auto-generated method stub
      return null;
   }

}
