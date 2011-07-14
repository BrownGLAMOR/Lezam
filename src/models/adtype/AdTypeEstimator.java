package models.adtype;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import models.AbstractModel;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

/**
 * This will simply track a histogram of adTypes shown by each advertiser.
 * Most frequent ad type is returned.
 * @author sodomka
 *
 */
public class AdTypeEstimator extends AbstractAdTypeEstimator {


	Set<Query> _querySpace;
	Set<String> _advertisersSet;
	Set<Product> _products;
	Set<Ad> _adSpace;
	
	//Keep track of how many times each (agent,query,adType) occurred
	//String advertiser, Query query, Ad
	HashMap<String, HashMap<Query, HashMap<Ad, Integer>>> numAppearances;
	HashMap<String, HashMap<Query, Ad>> mostLikelyAd;
	
	

	public AdTypeEstimator(Set<Query> querySpace, Set<String> advertisersSet, Set<Product> products) {
		this._querySpace = querySpace;
		this._advertisersSet = advertisersSet;
		this._products = products;
		
		//Create set of possible ads. //TODO: Does this already exist somewhere?
		this._adSpace = new HashSet<Ad>();
		_adSpace.add(new Ad()); //generic ad
		for (Product product : _products) {
			_adSpace.add(new Ad(product));
		}
		
		//Initialize appearances map
		numAppearances = new HashMap<String, HashMap<Query, HashMap<Ad, Integer>>>();
		for (String advertiser : _advertisersSet) {
			numAppearances.put(advertiser, new HashMap<Query, HashMap<Ad, Integer>>());
			for (Query query : _querySpace) {
				numAppearances.get(advertiser).put(query, new HashMap<Ad, Integer>());
				for (Ad ad : _adSpace) {
					numAppearances.get(advertiser).get(query).put(ad, 0);
				}
			}
		}

		
		//Initialize most likely ad map
		mostLikelyAd = new HashMap<String, HashMap<Query, Ad>>();
		for (String advertiser : _advertisersSet) {
			mostLikelyAd.put(advertiser, new HashMap<Query, Ad>());
			for (Query query : _querySpace) {
				//Have the most likely ad be generic by default
				mostLikelyAd.get(advertiser).put(query, new Ad());
			}
		}		
	}


	@Override
	public Ad getAdTypeEstimate(Query q, String advertiser) {
		// TODO Add try/catch in case a bad query or advertiser is given
		Ad ad = mostLikelyAd.get(advertiser).get(q);
		return ad;
	}

	@Override
	public void updateModel(QueryReport queryReport) {
		
		//Update histogram
		for (String advertiser : _advertisersSet) {
			for (Query query : _querySpace) {
				queryReport.getAd(query, advertiser);
	            Ad ad = queryReport.getAd(query, advertiser);
	            if (ad != null) {
	               incrementNumAppearances(advertiser, query, ad);
	            }
			}
		}
		
		//Decide which ads are most likely
		for (String advertiser : _advertisersSet) {
			for (Query query : _querySpace) {
				Ad highestFrequencyAd = null;
				int highestFrequency = -1;
				
				for (Ad ad : _adSpace) {
					int frequency = numAppearances.get(advertiser).get(query).get(ad);
					if (frequency > highestFrequency) {
						highestFrequency = frequency;
						highestFrequencyAd = ad;
					}
				}
				
				mostLikelyAd.get(advertiser).put(query, highestFrequencyAd);
			}
		}
		
		
//		//DEBUG
//		//Print values
//		System.out.println("Ad counts:");
//		for (String advertiser : _advertisersSet) {
//			for (Query query : _querySpace) {
//				for (Ad ad : _adSpace) {
//					int count = numAppearances.get(advertiser).get(query).get(ad);
//					System.out.println(advertiser + "," + query + "," + ad + ": " + count);
//				}
//			}
//		}
//		
//		System.out.println("Most likely ads:");;
//		for (String advertiser : _advertisersSet) {
//			for (Query query : _querySpace) {
//				Ad ad = mostLikelyAd.get(advertiser).get(query);
//				System.out.println(advertiser + "," + query + ": " + ad);
//			}
//		}
//		//END DEBUG
		
	}


	
	private void incrementNumAppearances(String advertiser, Query query, Ad ad) {
		int previousVal = numAppearances.get(advertiser).get(query).get(ad);
		numAppearances.get(advertiser).get(query).put(ad, previousVal+1);
	}


	@Override
	public AbstractModel getCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
