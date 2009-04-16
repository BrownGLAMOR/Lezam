package agents;

import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.io.*;
import java.util.Hashtable;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;

/**

Pr(conversion) array
Desired sales in each query array (could be expressed as the weight of the daily capacity)
	desired # clicks in query q = desired # sales in query q / Pr(conversion_q)
MaxBid array (maximum amount we're willing to pay)
	MaxBid_q = USP_q * Pr(conversion_q)

budget_q = bid_q * desiredClicks_q

Probably want to have initBid_q set as 2/3 or 3/4 of maxBid_q

...to decide the changes in bid...
Currently, we
Want #s for desired # of clicks.
If desired # (or more) of clicks were received yesterday, 
  	lower bid for that query to CPC-epsilon 
  	(i.e. just lower than yesterday's bidder, hopefully putting us in one lower slot)
If under the desired # of clicks
	if we have hit maxBid_q, lower the weight of this query for desired # sales
  	else: Raise bid (possibly with noise)

 */

public class JESOMAgent extends AbstractAgent {

	// There should be a nicer way to get these capacities.
	final static private double SmallCapacity = 300;
	final static private double MediumCapacity = 400;
	final static private double LargeCapacity = 500;

	private double[] capWeights; //to be used later

	protected JESOMBidStrategy _bidStrategy;

	protected Hashtable<String, Double> capacityName;

	public JESOMAgent(){}

	@Override
	protected void simulationSetup() {}

	@Override
	protected void initBidder() {

		printAdvertiserInfo();

		SetCapacityName();

		BufferedReader bidsBufReader, budgetBufReader, conBufReader;

		try
		{   
			// Read initial bid and budget from the files.
			FileReader bidsFstream = new FileReader("data/bids.txt");
			FileReader budgetFstream = new FileReader("data/budget.txt");
			bidsBufReader = new BufferedReader(bidsFstream);
			budgetBufReader = new BufferedReader(budgetFstream);
			conBufReader = new BufferedReader(budgetFstream);
		}
		catch (Exception e)
		{
			//Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return;
		}

		Hashtable<Query, Pair<Double, Double> > initQueryBidBudget =
			GetInitQueryBidBudget(bidsBufReader, budgetBufReader, /**TODO conBufReader,*/
					_advertiserInfo.getManufacturerSpecialty(),
					_advertiserInfo.getComponentSpecialty(),
					_advertiserInfo.getDistributionCapacity());

		_bidStrategy = new JESOMBidStrategy(_querySpace, initQueryBidBudget);
	}

	//silly hack
	HashMap<Query, Pair<Double, Double>> savedData = new HashMap<Query, Pair<Double, Double>>();

	@Override
	protected void updateBidStrategy() {
		/* Add some code to update our strategy */
		QueryReport qr = _queryReports.remove();
		if (qr!=null){
			for (Query q : _bidStrategy.getQuerySpace()){
				double clicksGot = qr.getClicks(q);
				double clicksAim = _bidStrategy.getQuerySpendLimit(q)/_bidStrategy.getQueryBid(q);
				if (clicksGot < clicksAim){
					_bidStrategy.setQueryBid(q, _bidStrategy.getQueryBid(q)*1.15+.01);
					_bidStrategy.setQueryBudget(q, _bidStrategy.getQueryBid(q)*clicksAim*.83);
				}
				else {
					_bidStrategy.setQueryBid(q, qr.getCPC(q)+.01);
					_bidStrategy.setQueryBudget(q, _bidStrategy.getQueryBid(q)*clicksAim*1.2);
				}
			}
		}

	}

	@Override
	protected BidBundle buildBidBudle(){
		System.out.println("**********");
		System.out.println(_bidStrategy);
		System.out.println("**********");
		return _bidStrategy.buildBidBundle();
	}

	// Again there should be a better way to get the association between the capacity and its name.
	private void SetCapacityName()
	{
		capacityName = new Hashtable<String, Double>();
		capacityName.put("small", SmallCapacity);
		capacityName.put("medium", MediumCapacity);
		capacityName.put("large", LargeCapacity);
	}

	/* Reads of bids and budgets to initialize our strategy. */
	protected Hashtable<Query, Pair<Double, Double>> GetInitQueryBidBudget(BufferedReader bidsBufReader,
			BufferedReader budgetBufReader,
			/**TODO BufferedReader convBufReader,*/
			String _manufacturer,
			String _component,
			double _capacity)
			{
		Hashtable<Query, Pair<Double, Double>> initQueryBidBudget = new
		Hashtable<Query, Pair<Double, Double>>();

		Scanner bidsScanner = new Scanner(bidsBufReader);
		Scanner budgetScanner = new Scanner(budgetBufReader);
		/**TODO Scanner conversionScanner = new Scanner(convBufReader); */

		int i = 0;
		//some change

		// Expects file of format <Manufacturer/Component/CapacityStr> <int> +
		// Assumes that the bid and budget files are of the same length. (and of the same order)
		while(bidsScanner.hasNextLine())
		{
			StringTokenizer bidsStr = new StringTokenizer(bidsScanner.nextLine(), "\t");
			StringTokenizer budgetStr = new StringTokenizer(budgetScanner.nextLine(), "\t");
			/**StringTokenizer convStr = new StringTokenizer(conversionScanner.nextLine(), "\t"); TODO */
			if ((bidsStr.countTokens() == (_querySpace.size()+1)) &&
					(budgetStr.countTokens() == (_querySpace.size()+1)) /**&& 
            		(convStr.countTokens() == (_querySpace.size()+1)) TODO*/ )
			{
				// Skip budget first column, read it from bids file instead
				budgetStr.nextToken();
				StringTokenizer speciality = new StringTokenizer(bidsStr.nextToken(), "/");

				String rowManufacturer = speciality.nextToken().toLowerCase().trim();
				String rowComponent = speciality.nextToken().toLowerCase().trim();
				String rowCapacityStr = speciality.nextToken().toLowerCase().trim();

				double rowCapacity = capacityName.get(rowCapacityStr);            	

				if (_manufacturer.toLowerCase().equals(rowManufacturer) &&
						_component.toLowerCase().equals(rowComponent) &&
						_capacity == rowCapacity)
				{    
					// Our speciality row.            		
					while (bidsStr.countTokens() > 0)
					{
						// Read bid and budget for query i.           		
						double bid = new Double(bidsStr.nextToken());
						double budget = new Double(budgetStr.nextToken());

						Pair<Double, Double> pair = new Pair<Double, Double>(bid*.6, budget*.75);
						Query q = new Query(QueryColEnum.values()[i].manufacturer, QueryColEnum.values()[i].component);
						initQueryBidBudget.put(q, pair);
						i++;
					}
					// In the future we should probably read off the whole table not just our speciality.
					return  initQueryBidBudget;
				}
			}
		}
		return initQueryBidBudget;
			}

	/*
	 * Since for now we read off the text file, we do not know which column corresponds
	 * to which query. This is one way of specifying column number->query association.
	 */
	public enum QueryColEnum {

		F0 (null, null),
		TV   (null, "tv"),
		DVD   (null, "dvd"),
		Audio   (null, "audio"),
		Lioneer ("lioneer", null),
		PG("pg", null),
		Flat("flat", null),
		TL("lioneer", "tv"),
		TP ("pg", "tv"),
		TA ("flat", "tv"),
		DL ("lioneer", "dvd"),
		DP ("pg", "dvd"),
		DF ("flat", "dvd"),
		AL ("lioneer", "audio"),
		AP ("pg", "audio"),
		AF ("flat", "audio");

		private final String manufacturer;
		private final String component;

		QueryColEnum(String m, String c) {

			this.manufacturer = m;
			this.component = c;
		}
	}
}