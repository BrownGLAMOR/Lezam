package models.advertiserspecialties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import models.AbstractModel;


/**
 * A predictor of agent CSB and MSB based on simple counting heuristics:
 * 1. Agents are more likely to target queries containing their CSB/MSB
 * 2. Agents are more likely to show up in auctions containing their CSB/MSB
 * 
 * For (2), we could perhaps do something like consider a player's location in 
 * the auction, but we'll just consider binary observations for now
 * (they were in the auction or they weren't)
 * @author sodomka
 *
 */
public class SimpleSpecialtyModel extends AbstractModel{

	String[] advertisers = {"adv1", "adv2", "adv3", "adv4", "adv5", "adv6", "adv7", "adv8"};
//	int numAdvertisers = advertisers.length;
//	int numQueries = 16;
//	int numComponents = 3;
//	int numManufacturers = 3;
//	int numProducts = 9;
	
	private Set<Product> _products;
	private Set<Query> _querySpace;
	
	
	//How often did each agent appear in query q?
	HashMap<String, HashMap<Query, Integer>> numAppearances;
	HashMap<String, HashMap<Query, HashMap<Product, Integer>>> numTargets;

	
	public SimpleSpecialtyModel() {
		_products = new HashSet<Product>();
		_querySpace = new HashSet<Query>();
	
		//Initialize products
		_products.add(new Product("pg","tv"));
		_products.add(new Product("pg","dvd"));
		_products.add(new Product("pg","audio"));
		_products.add(new Product("lioneer","tv"));
		_products.add(new Product("lioneer","dvd"));
		_products.add(new Product("lioneer","audio"));
		_products.add(new Product("flat","tv"));
		_products.add(new Product("flat","dvd"));
		_products.add(new Product("flat","audio"));
		
		//Initialize Query Space
        _querySpace.add(new Query(null, null));
        for(Product product : _products) {
            // The F1 query classes
            // F1 Manufacturer only
            _querySpace.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            _querySpace.add(new Query(null, product.getComponent()));

            // The F2 query class
            _querySpace.add(new Query(product.getManufacturer(), product.getComponent()));
        }
        
	
		numAppearances = new HashMap<String, HashMap<Query, Integer>>();
		numTargets = new HashMap<String, HashMap<Query, HashMap<Product, Integer>>>();
		
		for (String advertiser : advertisers) {
			numAppearances.put(advertiser, new HashMap<Query, Integer>() );
			numTargets.put(advertiser, new HashMap<Query, HashMap<Product, Integer>>() );
			
			for (Query query : _querySpace) {
				numAppearances.get(advertiser).put(query, 0);
				numTargets.get(advertiser).put(query, new HashMap<Product, Integer>() );
				
				for (Product product : _products) {
					numTargets.get(advertiser).get(query).put(product, 0);
				}
			}
		}
	}
	
	
	public void updateModel(QueryReport queryReport) {
		for (String advertiser : advertisers) {
			for (Query query : _querySpace) {
				Ad ad = queryReport.getAd(query, advertiser);
				
				if (ad != null) {
					incrementNumAppearances(advertiser, query);
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
		if (!_products.contains(product)) return;
		
		int val = numTargets.get(advertiser).get(query).get(product);
		numTargets.get(advertiser).get(query).put(product, val+1);
	}

	private void incrementNumAppearances(String advertiser, Query query) {
		int val = numAppearances.get(advertiser).get(query);
		numAppearances.get(advertiser).put(query, val+1);
	}
	
	public int getNumTargets(String advertiser, Query query, Product product) {
		return numTargets.get(advertiser).get(query).get(product);
	}
	
	public int getNumAppearances(String advertiser, Query query) {
		return numAppearances.get(advertiser).get(query);
	}
	
	
	
	public String getComponentSpecialty(String advertiser) {
		//If they targeted one component more than the rest, make that the specialty
		HashMap<String, Integer> numComponentAppearances = getNumComponentAppearances(advertiser);
		return getMostLikelySpecialty(numComponentAppearances);
	}
	
	public String getManufacturerSpecialty(String advertiser) {
		//If they targeted one component more than the rest, make that the specialty
		HashMap<String, Integer> numManufacturerAppearances = getNumManufacturerAppearances(advertiser);
		return getMostLikelySpecialty(numManufacturerAppearances);
	}
	
	
	/**
	 * Returns the number of times the advertiser was present in an auction related
	 * to each component type.
	 * @param advertiser
	 * @return
	 */
	public HashMap<String, Integer> getNumComponentAppearances(String advertiser) {
		HashMap<String, Integer> numComponentAppearances = new HashMap<String, Integer>();
		for (Product product : _products) {
			numComponentAppearances.put(product.getComponent(), 0); //Faster if we just had set of components
		}

		//Count # of appearances for each component
		for (Query query : _querySpace) {
			String component = query.getComponent();
			if (component==null) continue;
			int oldVal = numComponentAppearances.get(component);
			int appearances = getNumAppearances(advertiser, query);
			numComponentAppearances.put(component, oldVal + appearances);
		}
		return numComponentAppearances;
	}
	
	
	/**
	 * Returns the number of times the advertiser was present in an auction related
	 * to each component type.
	 * @param advertiser
	 * @return
	 */
	public HashMap<String, Integer> getNumManufacturerAppearances(String advertiser) {
		HashMap<String, Integer> numManufacturerAppearances = new HashMap<String, Integer>();
		for (Product product : _products) {
			numManufacturerAppearances.put(product.getManufacturer(), 0); //Faster if we just had set of components
		}

		//Count # of appearances for each manufacturer
		for (Query query : _querySpace) {
			String manufacturer = query.getManufacturer();
			if (manufacturer==null) continue;
			int oldVal = numManufacturerAppearances.get(manufacturer);
			int appearances = getNumAppearances(advertiser, query);
			numManufacturerAppearances.put(manufacturer, oldVal + appearances);
		}
		return numManufacturerAppearances;
	}
	
	
	
	public String getMostLikelySpecialty( HashMap<String, Integer> numSpecialtyAppearances ) {
		//Find most likely component specialty.
		//FIXME: this is biasing toward component specialties we traverse earlier,
		//  since ties go to whatever was seen first.
		int mostAppearances = -1;
		String mostLikelySpecialty = null;
		for (String component : numSpecialtyAppearances.keySet()) {
			int appearances = numSpecialtyAppearances.get(component);
			if (appearances > mostAppearances) {
				mostAppearances = appearances;
				mostLikelySpecialty = component;
			}
		}
		
		return mostLikelySpecialty;
	}
	
	
	
	public void printAllPredictions() {
		System.out.print("predictions: " );
		for (String advertiser : advertisers) {
			//System.out.print(advertiser + "=" + getComponentSpecialty(advertiser) +"  ");
			System.out.print(advertiser + "=" + getManufacturerSpecialty(advertiser) +"  ");
		}
		System.out.println();
	}
	
	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
